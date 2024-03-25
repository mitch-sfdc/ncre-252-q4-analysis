package com.salesforce.ncre;

import org.apache.commons.cli.*;
import org.drools.compiler.kie.builder.impl.MemoryKieModule;
import org.drools.model.codegen.ExecutableModelProject;
import org.drools.modelcompiler.CanonicalKieModule;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.KieModule;
import org.kie.api.builder.ReleaseId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Compiler {
    // singleton instance
    public static final Compiler INSTANCE = new Compiler();
    private static final Logger LOG = LoggerFactory.getLogger(Compiler.class);
    private final Options options = new Options();
    private final KieServices ks = KieServices.Factory.get();
    private final KieFileSystem kfs = ks.newKieFileSystem();
    private String orgName = "org.salesforce.ncre";
    private String libraryName = "250_Q4";
    private String version = "0.0.1";
    private ReleaseId releaseId = ks.newReleaseId(orgName, libraryName, version);
    private String kjarFilename = releaseId.toString() + ".kjar";
    public final Path path = Paths.get(kjarFilename);
    private String drlFileName = "RuleSet1Original-fixed.drl";
    private String drlPath = "/resources/UPSDRL/" + drlFileName;
    private Path drlTemplatePath = Path.of("/Users/mitch.christensen/dev/iphone-discount 2/src/test" + drlPath);
    private final String packagePathDrl = "src/main/resources/industries/nearcore/rule/engine/";
    private int rulesetCount = 500;
    private boolean ruleExecModel = true;
    private boolean generateRulePriorities = false;
    private int duplicatePriorityCount = 0;
    private int productCatalogSize = -1;

    // force a singleton
    private Compiler() {}

    public static void main(String[] args) throws IOException {
        // define and parse the command line options
        INSTANCE.defineOptions();
        INSTANCE.parseCommandLine(args);

        // compile the rules
        if(INSTANCE.ruleExecModel) {
            LOG.info("Compiling Rule Exec Model DRL...");
            INSTANCE.compileDrlRuleExecModel();
        } else {
            LOG.info("Compiling non-Rule Exec Model DRL...");
            INSTANCE.compileDrl();
        }
    }

    public void compileDrl() throws IOException {
        generateDrl();
        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        byte[] bytes = ((MemoryKieModule) kieModule).getBytes();

        // write the KJAR to disk
        LOG.info("Writing KJAR to disk: " + path.toAbsolutePath());
        Files.write(path, bytes);
    }

    public void compileDrlRuleExecModel() throws IOException {
        // generate the expanded DRL from the rule set DRL template
        generateDrl();

        // compile the generated rule sets
        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll(ExecutableModelProject.class);

        // convert the generated KJAR to a byte array
        CanonicalKieModule kieModule = (CanonicalKieModule) kieBuilder.getKieModule(ExecutableModelProject.class);
        byte[] bytes = kieModule.getBytes();

        // write the KJAR to disk
        LOG.info("Writing Exec Model KJAR to disk: " + path.toAbsolutePath());
        Files.write(path, bytes);
    }

    private void generateDrl() throws IOException {
        // read the (template) DRL contents
        String templateDrl = Files.readString(drlTemplatePath);

        // read the rule set template and expand/generate the DRL
        LOG.info(String.format("Generating %,d rule sets from the rule set template: %s",
                this.rulesetCount, drlTemplatePath));
        for(int i = 1 ; i <= this.rulesetCount ; i++){
            String generatedDrl =
                    MessageFormat.format(templateDrl, String.format("%03d", i),
                            "Product"+i , "Product"+(i+1));

            // optionally generate new rule priorities
            if(this.generateRulePriorities) {
                generatedDrl = generateRulePriorities(generatedDrl);
            }

            kfs.write(packagePathDrl +"UPSRuleset-"+i+".drl", generatedDrl);
        }

        kfs.generateAndWritePomXML(releaseId);
    }

    private String generateRulePriorities(String drl) {
        // calculate a random rule set priority, duplicates allowed
        List<Integer> ruleSetPriorities = IntStream.range(0, this.rulesetCount).boxed().collect(Collectors.toList());
        Collections.shuffle(ruleSetPriorities);
        List<Integer> originalRulePriorities = new ArrayList<>();

        // extract the rule priorities
        int ruleIdx = 0;
        StringBuffer newDrl = new StringBuffer();
        Pattern p = Pattern.compile("salience (-?\\d)", Pattern.MULTILINE);
        Matcher m = p.matcher(drl);
        while(m.find()) {
            // calculate and store each new salience value
            int newPriority = Integer.valueOf(m.group(1)) + new Random().nextInt(this.rulesetCount);

            // replace the previous/original salience with the new value
            m.appendReplacement(newDrl, "salience " + newPriority);
        }
        m.appendTail(newDrl);

        return newDrl.toString();
    }

    /**
     * Define the command line options
     */
    private void defineOptions() {
        //  --df --drlFilename : The name of the source DRL file
        Option option = new Option("df", "drlFilename", true,
                "The source DRL filename");
        options.addOption(option);

        //  --dpc --duplicatePriorityCount
        option = new Option("dpc", "duplicatePriorityCount", true,
                "The total number of desired duplicate priorities (default: 0)");
        options.addOption(option);

        //  --grp --generateRulePriorities : Generate the rule priorities relative to the rule set priorities
        option = new Option("grp", "generateRulePriorities", false,
                "Generate the rule priorities relative to the rule set priorities (default: false)");
        options.addOption(option);

        // --h, --help : Show the command line option menu and exit
        option = new Option("h", "help", false,
                "Show help menu");
        options.addOption(option);

        // --kf, --kjarFilename : The name of the generated KJAR file (default: org.salesforce.ncre:250_Q4:0.0.1)
        option = new Option("kf", "kjarFilename", true,
                "The generated KJAR filename");
        options.addOption(option);

        // --lib, --libraryName : The rule library name
        option = new Option("lib", "libraryName", true,
                "The organization name used in the release ID and default KJAR name");
        options.addOption(option);

        // --on, --orgName : The organization name
        option = new Option("on", "orgName", true,
                "The organization name used in the release ID and default KJAR name");
        options.addOption(option);

        // --pcs, --productCatalogSize : The total number of products in the product catalog
        option = new Option("pcs", "productCatalogSize", true,
                "The total number of products in the product catalog (default: -1 (sequential))");
        options.addOption(option);

        // --rc, --rulesetCount : The number of rule sets to generate
        option = new Option("rc", "rulesetCount", true,
                "The total number of rule sets to generate (default : 500)");
        options.addOption(option);

        // --rem, --ruleExecModel : Generate the KJAR using the Drools Rule Exec Model
        option = new Option("rem", "ruleExecModel", false,
                "Generate the KJAR using the Drools Rule Exec Model [true|false] (default : true)");
        options.addOption(option);

        // --ver, --version : The version of the generated KJAR file
        option = new Option("ver", "version", true,
                "The version used in the release ID and default KJAR name");
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

            if(cmd.hasOption("df")) {
                this.drlFileName = cmd.getOptionValue("df");
                drlPath = "/resources/UPSDRL/" + drlFileName;
                drlTemplatePath = Path.of("/Users/mitch.christensen/dev/iphone-discount 2/src/test" + drlPath);
            }

            if(cmd.hasOption("dpc")) {
                this.duplicatePriorityCount = Integer.valueOf(cmd.getOptionValue("dpc"));
            }

            if(cmd.hasOption("kf")) {
                this.kjarFilename = cmd.getOptionValue("kf");
            }

            if(cmd.hasOption("grp")) {
                this.generateRulePriorities = true;
            }

            if(cmd.hasOption("h")) {
                helper.printHelp("java [options] com.salesforce.ncre.engine", options);
                System.exit(0);
            }

            if(cmd.hasOption("lib")) {
                this.libraryName = cmd.getOptionValue("lib");
            }

            if(cmd.hasOption("on")) {
                this.orgName = cmd.getOptionValue("on");
            }

            if(cmd.hasOption("pcs")) {
                this.productCatalogSize = Integer.valueOf(cmd.getOptionValue("on"));
            }

            if(cmd.hasOption("rem")) {
                this.ruleExecModel = Boolean.valueOf(cmd.getOptionValue("rem"));
            }

            if(cmd.hasOption("rc")) {
                this.rulesetCount = Integer.valueOf(cmd.getOptionValue("rc"));
            }

            if(cmd.hasOption("ver")) {
                this.version = cmd.getOptionValue("ver");
            }

        } catch (ParseException e) {
            System.out.println(e.getMessage());
            helper.printHelp("Usage:", options);
            System.exit(0);
        }

    }

}
