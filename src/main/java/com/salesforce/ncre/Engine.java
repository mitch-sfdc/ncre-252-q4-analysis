package com.salesforce.ncre;

import com.salesforce.ncre.debug.PhreakActivationIteratorNcre;
import industries.nearcore.rule.engine.ActionHelper;
import industries.nearcore.rule.engine.cartDetails;
import industries.nearcore.rule.engine.cartLineDetails;
import industries.nearcore.rule.engine.loyaltyMember;
import org.apache.commons.cli.*;
import org.drools.core.common.ReteEvaluator;
import org.drools.core.rule.consequence.InternalMatch;
import org.drools.serialization.protobuf.iterators.ActivationIterator;
import org.drools.core.util.Iterator;
import org.drools.serialization.protobuf.iterators.PhreakActivationIterator;
import org.kie.api.KieBase;
import org.kie.api.KieBaseConfiguration;
import org.kie.api.KieServices;
import org.kie.api.builder.KieRepository;
import org.kie.api.builder.Message;
import org.kie.api.builder.ReleaseId;
import org.kie.api.builder.Results;
import org.kie.api.command.Command;
import org.kie.api.conf.SequentialOption;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.event.rule.MatchCancelledEvent;
import org.kie.api.io.Resource;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.StatelessKieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.kie.internal.command.CommandFactory;
import org.kie.internal.runtime.StatefulKnowledgeSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Engine {
    private static final Logger LOG = LoggerFactory.getLogger(Engine.class);
    private final KieServices ks = KieServices.Factory.get();
    private final ReleaseId releaseId = ks.newReleaseId("org.salesforce.ncre", "250_Q4", "0.0.1");
    private final Options options = new Options();
    private final ActionHelper actionHelper = new ActionHelper();
    private boolean forceCompilation = false;
    private ExecutionMode executionMode = ExecutionMode.STATEFUL;
    private String kjarName = releaseId.toString() + ".kjar";
    private int totalLineItems = 200;
    private int totalMatchingLineItems = 40;
    private boolean isLoyaltyMember = true;
    private boolean isInteractiveMode = false;
    private boolean useExecModel = false;
    private boolean dumpActions = false;
    private int executionCount = 1;
    private int deltaSyncCount = 1;
    private final Map<Object, FactHandle> factToFactHandleMap = new HashMap<>();
    private boolean useProductCatalog = false;
    private int productCount = 10_000;
    private int productCategoryCount = 10;
    private boolean dumpPayload = false;
    private boolean marshallSession = false;
    private boolean dumpSession = false;

    private enum ExecutionMode {
        STATELESS, STATEFUL
    }

    /**
     * Run the rule engine
     *
     * @param args command line arguments (processed by commons-cli)
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        Engine engine = new Engine();

        // process the java command line
        engine.defineOptions();
        engine.parseCommandLine(args);

        // optionally compile the rules
        if (engine.forceCompilation) {
            Compiler.INSTANCE.compileDrl();
        }

        // run the engine
        engine.execute();
    }

    public void execute() throws IOException {
        if(isInteractiveMode)
            prompt("Press RETURN to begin the test...");

        // read the KJAR from disk
        LOG.info("Loading KJAR...");
        byte[] uncompressedByte = Files.readAllBytes(Paths.get(this.kjarName));

        KieRepository kieRepository = ks.getRepository();
        Resource jarRes = ks.getResources().newByteArrayResource(uncompressedByte);
        kieRepository.addKieModule(jarRes);
        KieContainer kieContainer = ks.newKieContainer(releaseId);
        Results verifyResults = kieContainer.verify();
        for (Message m : verifyResults.getMessages()) {
            LOG.info("{}", m);
        }

        // create the cart data
        cartDetails cart = generateCartDetails();

        // execute either statefully or statelessly
        if (executionMode == ExecutionMode.STATEFUL) {
            executeStatefulRules(kieContainer, cart);
        } else {
            executeStatelessRules(kieContainer, cart);
        }

        if(isInteractiveMode)
            prompt("Press any key to end the test...");
    }

    private void executeStatefulRules(KieContainer kieContainer, cartDetails cart) throws IOException {
        KieSession kieSession = null;

        // execute the rules
        int fullSyncRuleExecutionCount = 0;
        int deltaSyncRuleExecutionCount = 0;
        int serializedSessionBytes = 0;
        Instant fullRunStart = Instant.now();
        Instant innerStart = null;
        Duration insertDuration = Duration.ZERO;
        Duration fullExecuteDuration = Duration.ZERO;
        Duration deltaSyncDuration = Duration.ZERO;
        Duration marshallingDuration = Duration.ZERO;
        for (int i = 0; i < this.executionCount; i++) {
            // create the stateful session
            kieSession = kieContainer.getKieBase().newKieSession();

            kieSession.setGlobal("actionHelper", actionHelper);

            // insert the cart data into working memory
            LOG.debug("Inserting facts into working memory...");
            innerStart = Instant.now();
            for (cartLineDetails lineItem : cart.getCartLineDetailsList()) {
                kieSession.insert(lineItem);
                factToFactHandleMap.put(lineItem, kieSession.insert(lineItem));
            }
            factToFactHandleMap.put(cart.getLoyaltyMember(), kieSession.insert(cart.getLoyaltyMember()));
            factToFactHandleMap.put(cart, kieSession.insert(cart));
            insertDuration = insertDuration.plus(Duration.between(innerStart, Instant.now()));

            class MyListener extends DefaultAgendaEventListener {
                public int matchCount = 0;
                public int cancelledCount = 0;

                public void matchCancelled(MatchCancelledEvent event) {
                    if(LOG.isTraceEnabled())
                        LOG.trace(event.toString());
                    cancelledCount++;
                }

                public void afterMatchFired(AfterMatchFiredEvent event) {
                    super.afterMatchFired(event);
                    if (LOG.isTraceEnabled())
                        LOG.trace(event.toString());
                    matchCount++;
                }
            };
            MyListener myListener = new MyListener();
            kieSession.addEventListener(myListener);

            // fire the rules
            LOG.debug("Firing rules...");
            innerStart = Instant.now();
            fullSyncRuleExecutionCount += kieSession.fireAllRules();
            fullExecuteDuration = fullExecuteDuration.plus(Duration.between(innerStart, Instant.now()));
            LOG.debug("Total match count: " + myListener.matchCount);
            LOG.debug("Total match cancelled count: " + myListener.cancelledCount);

            // apply optional delta sync operations
            innerStart = Instant.now();
            for (int j = 0; j < this.deltaSyncCount; j++) {
                // modify some random rows, setting the line item discount back to 0.0
                for (int k = 0; k < cart.getCartLineDetailsList().size() * 0.05; k++) {
                    cartLineDetails lineItem =
                            cart.getCartLineDetailsList()
                                    .get(new Random().nextInt(cart.getCartLineDetailsList().size()));
                    lineItem.getPromotions().clear();
                    kieSession.update(factToFactHandleMap.get(lineItem), lineItem);
                }

                // fire the rules again for the delta sync
                deltaSyncRuleExecutionCount += kieSession.fireAllRules();
            }
            deltaSyncDuration = deltaSyncDuration.plus(Duration.between(innerStart, Instant.now()));

            // iterate the activations
            LOG.debug("Iterating internal activations...");
            PhreakActivationIteratorNcre it = PhreakActivationIteratorNcre.iterator((ReteEvaluator) kieSession);
            Map<String, List<InternalMatch>> ruleNameToInternalMatchMap = new HashMap<>();
            List<InternalMatch> distinctMatchList = new ArrayList<>();
            for (InternalMatch act = (InternalMatch) it.next(); act != null; act = (InternalMatch) it.next() ) {
                String ruleName = act.getRule().getName();

                if(distinctMatchList.size() == 0) {
                    distinctMatchList.add(act);
                } else {
                    boolean identityMatch = false;
                    // search for an identity match
                    for(InternalMatch match : distinctMatchList) {
                        if(match == act)
                            identityMatch = true;
                    }
                    if(!identityMatch)
                        distinctMatchList.add(act);
                }

                if(ruleNameToInternalMatchMap.keySet().contains(ruleName)) {
                    // add the current activation to the list for this rule
                    ruleNameToInternalMatchMap.get(ruleName).add(act);
                } else {
                    // create a new list of activations
                    List<InternalMatch> matches = new ArrayList<>();
                    matches.add(act);
                    ruleNameToInternalMatchMap.put(ruleName, matches);
                }
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Total internal match count : %,d",
                        ruleNameToInternalMatchMap.values().stream().mapToInt(List::size).sum()));
                LOG.debug(String.format("Unique internal match objects: %,d", distinctMatchList.size()));
                if (LOG.isTraceEnabled()) {
                    LOG.trace("Rule to internal match mapping: ");
                    for (String ruleName : ruleNameToInternalMatchMap.keySet()) {
                        LOG.trace(String.format("Rule : %s", ruleName));
                        for(InternalMatch match : ruleNameToInternalMatchMap.get(ruleName)) {
                            LOG.trace(String.format("    %s", match));
                        }
                    }
                }
            }

            if (this.marshallSession) {
                if(isInteractiveMode && this.executionCount == 1)
                    prompt("Press any key to begin marshalling...");

                innerStart = Instant.now();
                ByteArrayOutputStream baos = marshall(ks, kieSession);
                marshallingDuration = marshallingDuration.plus(Duration.between(innerStart, Instant.now()));
                serializedSessionBytes += baos.size();

                if(this.dumpSession) {
                    FileOutputStream fileOutputStream = new FileOutputStream("kieSession.bytes");
                    fileOutputStream.write(baos.toByteArray());
                    fileOutputStream.close();
               }
            }

            // dispose of the current session
            cart.setDiscountAmount(0.0);
            for (cartLineDetails lineItem : cart.getCartLineDetailsList()) {
                lineItem.getPromotions().clear();
            }
            factToFactHandleMap.clear();
            kieSession.dispose();
        }
        System.out.printf("Total run time : %,dms (avg. %,dms/request)\n",
                Duration.between(fullRunStart, Instant.now()).toMillis(),
                Duration.between(fullRunStart, Instant.now()).toMillis() / this.executionCount);
        System.out.printf("Total insertion time : %,dms\n", insertDuration.toMillis());
        System.out.printf("Number of rules fired for full execution: %,d (%,d per request) in %,dms (%,2.1fms/request)\n",
                fullSyncRuleExecutionCount, fullSyncRuleExecutionCount / this.executionCount,
                fullExecuteDuration.toMillis(),
                fullExecuteDuration.toMillis() / Double.valueOf(this.executionCount));
        System.out.printf("Number of rules fired on %,d delta sync operations: %,d (%,2.1f/request) in %,dms (%,2.1fms/request)\n",
                this.deltaSyncCount * this.executionCount,
                deltaSyncRuleExecutionCount, deltaSyncRuleExecutionCount / Double.valueOf(this.executionCount),
                deltaSyncDuration.toMillis(),
                deltaSyncDuration.toMillis() / Double.valueOf(this.executionCount * this.deltaSyncCount));
        System.out.println(String.format("Sessions size: %,d bytes (%,d/request) in %,dms (%,2.1f/request)",
                serializedSessionBytes, serializedSessionBytes / this.executionCount,
                marshallingDuration.toMillis(), marshallingDuration.toMillis() / Double.valueOf(this.executionCount)));
        if (this.dumpActions) {
            System.out.println("Actions: \n" + actionHelper.toJson());
        }
    }

    private void executeStatelessRules(KieContainer kieContainer, cartDetails cart) throws IOException {

        // set sequential mode on the agenda
        KieBaseConfiguration kieBaseConf = ks.newKieBaseConfiguration();
        kieBaseConf.setOption(SequentialOption.YES);
        KieBase kieBase = kieContainer.newKieBase(kieBaseConf);

        // create the stateless session
        StatelessKieSession kieSession = kieBase.newStatelessKieSession();

        // add the global action helper
        ActionHelper actionHelper = new ActionHelper();
        kieSession.setGlobal("actionHelper", actionHelper);

        // insert the cart data insert commands
        List<Command> commands = new ArrayList<>();
        commands.add(CommandFactory.newInsert(cart));
        commands.add(CommandFactory.newInsert(cart.getLoyaltyMember()));
        for (cartLineDetails lineItem : cart.getCartLineDetailsList()) {
            commands.add(CommandFactory.newInsert(lineItem));
        }

        kieSession.addEventListener( new DefaultAgendaEventListener() {
            public void afterMatchFired(AfterMatchFiredEvent event) {
                super.afterMatchFired( event );
                if(LOG.isDebugEnabled())
                    LOG.debug(event.toString());
            }
        });

        if(isInteractiveMode)
            prompt("Press any key to begin rule evaluation...");

        // fire the rules
        LOG.info(String.format("Executing stateless engine %,d times on %,d line items...",
                this.executionCount, cart.getCartLineDetailsList().size()));
        Instant start = Instant.now();
        Duration ruleExecutionDuration = Duration.ZERO;
        for (int i = 0; i < this.executionCount; i++) {
            start = Instant.now();
            kieSession.execute(CommandFactory.newBatchExecution(commands));
            ruleExecutionDuration = ruleExecutionDuration.plus(Duration.between(start, Instant.now()));

            // clear all the applied promotions prior to the next execution
            commands.clear();
            cart.getPromotions().clear();
            commands.add(CommandFactory.newInsert(cart));
            commands.add(CommandFactory.newInsert(cart.getLoyaltyMember()));
            for (cartLineDetails lineItem : cart.getCartLineDetailsList()) {
                lineItem.getPromotions().clear();
                commands.add(CommandFactory.newInsert(lineItem));
            }
        }
        System.out.printf("Number of rules fired: %,d in %,dms total time (%,2.1fms/request)\n",
                actionHelper.actionItemMap.values().stream()
                        .mapToInt(Integer::intValue)
                        .sum(),
                ruleExecutionDuration.toMillis(),
                ruleExecutionDuration.toMillis() / Double.valueOf(this.executionCount));
    }

    private cartDetails generateCartDetails() {
        Random r = new Random();
        List<cartLineDetails> lineItemList = new ArrayList<>();

        // create the shopping cart and populate the fields
        cartDetails cart = new cartDetails();
        cart.setCurrencyISOCode("USD");
        cart.setTransactionAmount(500.00);
        cart.setDiscountAmount(0.0);

        // create and add the loyalty member
        loyaltyMember loyaltyMember = new loyaltyMember();
        loyaltyMember.setIsLoyaltyMember(this.isLoyaltyMember);
        cart.setLoyaltyMember(loyaltyMember);

        // create the matching line items
        for(int i = 1 ; i <= (this.useProductCatalog ? this.totalLineItems : this.totalMatchingLineItems); i++){
            cartLineDetails lineItem = new cartLineDetails();
            lineItem.setCartLineProductId("Product" + (useProductCatalog ? r.nextInt(this.productCount) : i));
            lineItem.setCartLineProductCategoryId("Category" + (useProductCatalog ? r.nextInt(this.productCategoryCount) : "1"));
            lineItem.setCartLineItemId("UnifiedPromotion_Q4_Validation");
            lineItem.setCartLineItemQuantity(10.0);
            lineItemList.add(lineItem);
        }


        // create the non-matching line items
        if (!this.useProductCatalog) {
            for(int i = 1 ; i <= (this.totalLineItems - this.totalMatchingLineItems) ; i++){
                cartLineDetails lineItem = new cartLineDetails();
                lineItem.setCartLineProductId("XProduct"+i);
                lineItem.setCartLineProductCategoryId("Category2");
                lineItem.setCartLineItemId("UnifiedPromotion_Q4_Validation");
                lineItem.setCartLineItemQuantity(10.0);
                lineItemList.add(lineItem);
            }
        }

        cart.setCartLineDetailsList(lineItemList);

        if(this.dumpPayload)
            System.out.println("payload:\n" + cart.toJson());

        return cart;
    }

    // Display a prompt and wait for the RETURN key
    private void prompt(String x) {
        System.out.println(x);
        new Scanner(System.in).nextLine();
    }


    public ByteArrayOutputStream marshall(KieServices kieServices, KieSession kieSession) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Marshaller marshaller = kieServices.getMarshallers().newMarshaller(kieSession.getKieBase());
        marshaller.marshall(baos, kieSession);
        baos.close();
        return baos;
    }


    /**
     * Define the command line options
     */
    private void defineOptions() {
        //  -c --compile : Force rule compilation
        Option option = new Option("c", "compile", false,
                "Compile rules before executing (default : false)");
        options.addOption(option);

        // -da, --dumpActions : Dump the resulting action results to STDOUT (default: false)
        option = new Option("da", "dumpActions", false,
                "Dump the resulting action results to STDOUT (default: false)");
        options.addOption(option);

        // -dp, --dumpPayload : Dump the sales transaction payload in JSON format (default: false)
        option = new Option("dp", "dumpPayload", false,
                "Dump the sales transaction payload in JSON format (default: false)");
        options.addOption(option);

        // -ds, --dumpSession : Write the stateless session to disk as kieSession.bytes (default: false)
        option = new Option("ds", "dumpSession", false,
                "Write the stateless session to disk as kieSession.bytes (default: false)");
        options.addOption(option);

        // -dsc, --deltaSyncCount : The number of delta sync operations to run for stateful execution (default: 1)
        option = new Option("dsc", "deltaSyncCount", true,
                "The number of delta sync operations to run for stateful execution (default: 1)");
        options.addOption(option);

        // -ec, --executionCount : The number of times the engine will be executed against the payload
        option = new Option("ec", "executionCount", true,
                "The number of times the engine will be executed against the payload (default: 1)");
        options.addOption(option);

        // -em, --executionMode : Execute rules either stateful or stateless
        option = new Option("em", "executionMode", true,
                "Stateful or stateless execution [stateful|stateless] (default: stateful)");
        options.addOption(option);

        // -h, --help : Show the command line option menu and exit
        option = new Option("h", "help", false,
                "Show help menu");
        options.addOption(option);

        // -i, --interactive : Prompt (pause) at key points during execution
        option = new Option("i", "interactive", false,
                "Prompt (pause) at key points in the execution for debugging purposes (default : false)");
        options.addOption(option);

        // -ilm, --isLoyaltyMember : Set loyalty membership value
        option = new Option("ilm", "isLoyaltyMember", true,
                "Set the loyalty membership for this run [true|false] (default : true");
        options.addOption(option);

        // -k KJAR_NAME, --KJAR KJAR_NAME : Set the KJAR filename
        option = new Option("k", "kjar", true,
                "Specify the KJAR filename to be loaded (default: org.salesforce.ncre:250_Q4:0.0.1.kjar)");
        options.addOption(option);

        // -ms, --marshallSession : Marshall the KieSession after rule execution (default : false)
        option = new Option("ms", "marshallSession", false,
                "Marshall the KieSession after rule execution (default : false)");
        options.addOption(option);

        //  -pc --productCount : The number of products in the product catalog (default : 10,000)
        option = new Option("pc", "productCount", true,
                "[NOT IMPLEMENTED] The number of products in the product catalog (default : 10,000)");
        options.addOption(option);

        //  -pcc --productCategoryCount : The number of categories in the product catalog (default : 10)
        option = new Option("pcc", "productCategoryCount", true,
                "[NOT IMPLEMENTED] The number of categories in the product catalog (default : 10)");
        options.addOption(option);

        // -rem, --ruleExecMode : Run the engine using the Drools Rule Exec Model
        option = new Option("rem", "ruleExecModel", false,
                "Run the rule engine using the Drools Rule Executable Model mode (default: false)");
        options.addOption(option);

        // -tli LINE_ITEM_COUNT, --totaLineItems LINE_ITEM_COUNT : Set the total number of line items to generate
        option = new Option("tli", "totalLineItems", true,
                "Specify the total number of line items to be generated (default : 200)");
        options.addOption(option);

        // -tmli LINE_ITEM_COUNT, --totaMatchingLineItems LINE_ITEM_COUNT :
        // Set the total number of matching line items to generate
        option = new Option("tmli", "totalMatchingLineItems", true,
                "Specify the total number of matching line items to be generated (default: 40)");
        options.addOption(option);

        // -upc, --useProductCatalog : Generate product names from a product catalog simulator (default : false)
        option = new Option("upc", "useProductCatalog", false,
                "[NOT IMPLEMENTED] Generate product names from a product catalog simulator (default : false)");
        options.addOption(option);
    }

    /**
     * Parse the command line options and set the global variables
     *
     * @param args - The Java command line arguments
     */
    private void parseCommandLine(String[] args) {
        CommandLine cmd;
        CommandLineParser parser = new DefaultParser();
        HelpFormatter helper = new HelpFormatter();

        try {
            cmd = parser.parse(options, args);
            if(cmd.hasOption("c")) {
                this.forceCompilation = true;
            }

            if (cmd.hasOption("da")) {
                this.dumpActions = true;
            }

            if (cmd.hasOption("dp")) {
                this.dumpPayload = true;
            }

            if (cmd.hasOption("ds")) {
                this.dumpSession = true;
            }

            if (cmd.hasOption("dsc")) {
                this.deltaSyncCount = Integer.valueOf(cmd.getOptionValue("dsc"));
            }

            if (cmd.hasOption("ec")) {
                this.executionCount = Integer.valueOf(cmd.getOptionValue("ec"));
            }

            if (cmd.hasOption("em")) {
                this.executionMode = ExecutionMode.STATELESS;
            }

            if(cmd.hasOption("h")) {
                helper.printHelp("java [options] com.salesforce.ncre.engine", options);
                System.exit(0);
            }

            if(cmd.hasOption("i")) {
                this.isInteractiveMode = true;
            }

            if(cmd.hasOption("ilm")) {
                this.isLoyaltyMember = Boolean.valueOf(cmd.getOptionValue("ilm"));
            }

            if(cmd.hasOption("k")) {
                this.kjarName = cmd.getOptionValue("kjar");
            }

            if(cmd.hasOption("ms")) {
                this.marshallSession = true;
            }

            if(cmd.hasOption("pc")) {
                this.productCount = Integer.valueOf(cmd.getOptionValue("pc"));
                throw new IllegalArgumentException("Not yet implemented.");
            }

            if(cmd.hasOption("pcc")) {
                this.productCategoryCount = Integer.valueOf(cmd.getOptionValue("pcc"));
                throw new IllegalArgumentException("Not yet implemented.");
            }

            if(cmd.hasOption("rem")) {
                this.useExecModel = true;
            }

            if (cmd.hasOption("tli")) {
                this.totalLineItems = Integer.valueOf(cmd.getOptionValue("tli"));
            }

            if (cmd.hasOption("tmli")) {
                this.totalMatchingLineItems = Integer.valueOf(cmd.getOptionValue("tmli"));
            }

            if(cmd.hasOption("upc")) {
                this.useProductCatalog = true;
                throw new IllegalArgumentException("Not yet implemented.");
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        }

    }

}
