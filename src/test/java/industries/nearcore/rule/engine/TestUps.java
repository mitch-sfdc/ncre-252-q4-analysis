package industries.nearcore.rule.engine;

import org.drools.compiler.kie.builder.impl.MemoryKieModule;
import org.drools.model.codegen.ExecutableModelProject;
import org.junit.Test;
import org.kie.api.KieServices;
import org.kie.api.builder.*;
import org.kie.api.event.rule.AfterMatchFiredEvent;
import org.kie.api.event.rule.DefaultAgendaEventListener;
import org.kie.api.io.Resource;
import org.kie.api.marshalling.Marshaller;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Console;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TestUps {
    static final Logger LOG = LoggerFactory.getLogger(TestUps.class);

    @Test
    public void test() throws IOException {
        String packagePathDrl ="src/main/resources/industries/nearcore/rule/engine/";

        KieServices ks = KieServices.Factory.get();
        ReleaseId releaseId = ks.newReleaseId("orgId", "ruleLibraryId", "masterRulesetId");

        KieFileSystem kfs = ks.newKieFileSystem();
        String templateDrl = getFileContent("/resources/UPSDRL/RuleSet1Original-fixed.drl");

        for(int i =1 ; i <= 500 ; i ++){
            String modifiedDrl1 = MessageFormat.format(templateDrl, String.format("%03d", i), "Product"+i , "Product"+(i+1));
            //System.out.println(modifiedDrl1);
            kfs.write(packagePathDrl+"UPSRuleset-"+i+".drl", modifiedDrl1);
        }

        kfs.generateAndWritePomXML(releaseId);
        KieBuilder kieBuilder = ks.newKieBuilder(kfs).buildAll();
        KieModule kieModule = kieBuilder.getKieModule();
        byte[]  uncompressedByte = ((MemoryKieModule) kieModule).getBytes();

        KieServices kieServices = KieServices.Factory.get();
        KieRepository kieRepository = kieServices.getRepository();
        Resource jarRes = kieServices.getResources().newByteArrayResource(uncompressedByte);
        kieRepository.addKieModule(jarRes);
        KieContainer kieContainer = kieServices.newKieContainer(releaseId);
        Results verifyResults = kieContainer.verify();
        for (Message m : verifyResults.getMessages()) {
            LOG.info("{}", m);
        }

        KieSession kieSession = kieContainer.getKieBase().newKieSession();

        ActionHelper actionHelper = new ActionHelper();
        kieSession.setGlobal("actionHelper", actionHelper);

        cartDetails cartDetails = new cartDetails();
        cartDetails.setCurrencyISOCode("USD");
        cartDetails.setTransactionAmount(500.00);

        //Matching Product Loop
        List<cartLineDetails> cartLineDetailsList = new ArrayList();
        for(int i = 1 ; i <= 40 ; i++){
            cartLineDetails cartLineDetails1 = new cartLineDetails();
            cartLineDetails1.setCartLineProductId("Product"+i);
            cartLineDetails1.setCartLineProductCategoryId("Category1");
            cartLineDetails1.setCartLineItemId("UnifiedPromotion_Q4_Validation");
            cartLineDetails1.setCartLineItemQuantity(10.0);
            cartLineDetailsList.add(cartLineDetails1);
            kieSession.insert(cartLineDetails1);
        }


        //Not Matching Product Loop
        for(int i = 1 ; i <= 160 ; i++){
            cartLineDetails cartLineDetails1 = new cartLineDetails();
            cartLineDetails1.setCartLineProductId("XProduct"+i);
            cartLineDetails1.setCartLineProductCategoryId("Category2");
            cartLineDetails1.setCartLineItemId("UnifiedPromotion_Q4_Validation");
            cartLineDetails1.setCartLineItemQuantity(10.0);
            cartLineDetailsList.add(cartLineDetails1);
            kieSession.insert(cartLineDetails1);
        }

        loyaltyMember loyaltyMember = new loyaltyMember();
        loyaltyMember.setIsLoyaltyMember(true);

        cartDetails.setCartLineDetailsList(cartLineDetailsList);
        kieSession.insert(loyaltyMember);
        kieSession.insert(cartDetails);

        kieSession.addEventListener( new DefaultAgendaEventListener() {
            public void afterMatchFired(AfterMatchFiredEvent event) {
                super.afterMatchFired( event );
//                System.out.println(event);
            }
        });

        Instant start = Instant.now();
        int noOfRulesRun = kieSession.fireAllRules();
        System.out.println(String.format("Number of rules fired: %,d in %,dms ", noOfRulesRun,
                Duration.between(start, Instant.now()).toMillis()));

        start = Instant.now();
        ByteArrayOutputStream baos =  marshall(kieServices, kieSession);
        System.out.println("Sessions size: " +
                String.format("%,d bytes in ",baos.size()) +
                String.format("%,dms", Duration.between(start, Instant.now()).toMillis()));
//       System.out.println("Session data: " + baos);

        //kieContainer.getKieBase().getFactType(packageName, className);
        //Collection<KiePackage> packages = kieContainer.getKieBase().getKiePackages();
        //Collection<FactType> factTypes = packages.iterator().next().getFactTypes();
       // Class<?> factClass = factTypes.iterator().next().getFactClass();

    }


    public ByteArrayOutputStream marshall(KieServices kieServices, KieSession kieSession) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Marshaller marshaller = kieServices.getMarshallers().newMarshaller(kieSession.getKieBase());
        marshaller.marshall(baos, kieSession);
        baos.close();
        return baos;
    }

    private String getFileContent(String filePath) throws IOException {

        Path fileName
                = Path.of("." + "/src/test" + filePath);
        return Files.readString(fileName);
    }


}