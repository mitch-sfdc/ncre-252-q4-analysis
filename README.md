# ncre-252-q4-analysis
A producer (test harness) with source code examples used to investigate the Drools issues discovered during 252 Q4 testing of the Near Core Rules Engine (NCRE).

The process for using this project is to first run the ```Compiler``` executable to generate a ```KJAR``` rule library executable from a DRL source template file.  Once you have generated the KJAR with the desired shape (number of rule sets, rules, etc.), then you can execute the rule library using the ```Engine``` executable.

The two executable classes and their various command line options are defined further below.

# Building Locally
Pull this repository into your local environment, then run ```mvn clean install```.  This should compile and run a unit test that both compiles and runs the engine.

Import the project into your IDE and you should be good to go.

Create the desired run (command line option) configurations within your IDE and execute the various configurations of the compiler and rule engine.

# Some Standard Run Configurations
I've added lots of command line options to execute tests in various ways.  Here are a few key example command line options and an explanation of what they do.

- **Compiler**
    - No CLI parameters (all defaults)
        * Compiles the ```RuleSet1Original-fixed.drl``` DRL template file to a Drools Exec Model KJAR named ```org.salesforce.ncre:250_Q4:0.0.1.kjar``` in the project root directory with 500 rule sets resulting in an Exec Model KJAR
- **Engine**
    - No CLI parameters (all defaults)
        * Stateful execution of the ```org.salesforce.ncre:250_Q4:0.0.1.kjar``` file using a sales transaction with 200 line items

## DRL Template Files
This project is driven from a set of DRL template files.  Each template files defines the rules associated with a single rule set.  All of the various DRL template files are located in the ```src/test/resources/UPSDRL``` folder.  The templates format is a standard DRL format with template variables which are populated at runtime to create a final set of DRL files from the template.

The following shows a single rule definition from a rule set template.

```
rule "UP_RS_193_Rule_NonEliglible_{0}-1"
dialect "java"
salience -2
date-effective "2-Nov-2023"
    when
        exists($cartLineDetails1 : cartLineDetails(cartLineProductId in ("{1}") && cartLineProductCategoryId == "Category1"))
        Number(doubleValue() > 1) from accumulate ( cartLineDetails(cartLineProductId in ("{1}" , "NotUsedProduct" ) && $qty : cartLineItemQuantity != null), sum($qty) )
        $loyaltyMember3 : loyaltyMember(isLoyaltyMember == true)
    then
        actionHelper.addAction("Executed UP_RS_193_Rule_NonEliglible_{0}-1");
end
```

Template variables take the form of ```{1}```, which will be replaced with a real value, for example ```Product1```, using ```java.text.MessageFormat.format(...)``` (see the Java docs for details).

# Executable Classes
There are two executable classes defined within this project.

The ```com.salesforce.ncre.Compiler``` class reads a DRL template file and instantiates t the template the specified number of times to getnerate the final DRL file.  The ```Compiler``` populates the template variables and compiles the resulting DRL files into a Drools Exec Model (by default) ```KJAR``` file.

The ```com.salesforce.ncre.Engine``` class reads a previously compiled ```KJAR``` file, creates a knowledge base ```KieContainer``` / ```KieBase``` in memory, then generates a sales transaction payload comprised of one ```industries.nearcore.rule.engine.cartDetails``` header and zero or more related ```industries.nearcore.rule.engine.cartLineDetails``` objects.  The shopping cart shape is controlled using command line parameters and defaults to 200 shopping cart line items.  The shopping cart data is then inserted into working memory (in the stateful case) and a call to ```KieSession.fireAllRules()``` made to execute all the rules.  In order to marshall the ```KieSession``` after the rules run, use the ```-ms``` (```--marshalSession```) command line option.

Here are the command line options for both the ```Compiler``` and ```Engine``` executables.

## Compiler
The ```Compiler``` class supports the following command line options.

```
usage: java [options] com.salesforce.ncre.engine
 -dd,--dumpDrl                         Dump the generated DRL to a file
                                       with GAV naming (default : false)
 -df,--drlFilename <arg>               The source DRL filename (default :
                                       RuleSet1Original-fixed.drl)
 -dpc,--duplicatePriorityCount <arg>   The total number of desired
                                       duplicate priorities (default: 0)
 -grp,--generateRulePriorities         Generate the rule priorities
                                       relative to the rule set priorities
                                       (default: false)
 -h,--help                             Show help menu
 -kf,--kjarFilename <arg>              The generated KJAR filename
                                       (default :
                                       [org]:[libraryName]:[version].kjar
 -lib,--libraryName <arg>              The organization name used in the
                                       release ID and default KJAR name
                                       (default : 250_Q4
 -on,--orgName <arg>                   The organization name used in the
                                       release ID and default KJAR name
                                       (default : org.salesforce.ncre
 -pc,--productCount <arg>              [NOT IMPLEMENTED] The number of
                                       products in the product catalog
                                       (default : 10,000)
 -pcc,--productCategoryCount <arg>     [NOT IMPLEMENTED] The number of
                                       categories in the product catalog
                                       (default : 10)
 -rc,--rulesetCount <arg>              The total number of rule sets to
                                       generate (default : 500)
 -rem,--ruleExecModel                  Generate the KJAR using the Drools
                                       Rule Exec Model [true|false]
                                       (default : true)
 -upc,--useProductCatalog              [NOT IMPLEMENTED] Generate product
                                       names from a product catalog
                                       simulator (default : false)
 -ver,--version <arg>                  The version used in the release ID
                                       and default KJAR name
```

## Engine
The ```Engine``` class supports the following command line options.

```
usage: java [options] com.salesforce.ncre.engine
 -c,--compile                           Compile rules before executing
                                        (default : false)
 -da,--dumpActions                      Dump the resulting action results
                                        to STDOUT (default: false)
 -dp,--dumpPayload                      Dump the sales transaction payload
                                        in JSON format (default: false)
 -ds,--dumpSession                      Write the stateless session to
                                        disk as kieSession.bytes (default:
                                        false)
 -dsc,--deltaSyncCount <arg>            The number of delta sync
                                        operations to run for stateful
                                        execution (default: 1)
 -ec,--executionCount <arg>             The number of times the engine
                                        will be executed against the
                                        payload (default: 1)
 -em,--executionMode <arg>              Stateful or stateless execution
                                        [stateful|stateless] (default:
                                        stateful)
 -h,--help                              Show help menu
 -i,--interactive                       Prompt (pause) at key points in
                                        the execution for debugging
                                        purposes (default : false)
 -ilm,--isLoyaltyMember <arg>           Set the loyalty membership for
                                        this run [true|false] (default :
                                        true
 -k,--kjar <arg>                        Specify the KJAR filename to be
                                        loaded (default:
                                        org.salesforce.ncre:250_Q4:0.0.1.k
                                        jar)
 -ms,--marshallSession                  Marshall the KieSession after rule
                                        execution (default : false)
 -pc,--productCount <arg>               [NOT IMPLEMENTED] The number of
                                        products in the product catalog
                                        (default : 10,000)
 -pcc,--productCategoryCount <arg>      [NOT IMPLEMENTED] The number of
                                        categories in the product catalog
                                        (default : 10)
 -rem,--ruleExecModel                   Run the rule engine using the
                                        Drools Rule Executable Model mode
                                        (default: false)
 -tli,--totalLineItems <arg>            Specify the total number of line
                                        items to be generated (default :
                                        200)
 -tmli,--totalMatchingLineItems <arg>   Specify the total number of
                                        matching line items to be
                                        generated (default: 40)
 -upc,--useProductCatalog               [NOT IMPLEMENTED] Generate product
                                        names from a product catalog
                                        simulator (default : false)
```

## Logging
Logging is controlled using the ```src/main/resources/log4j.properties``` file.

## Viewing Activation Record Counts
The primary issue being investigated is the explosion (cross-product?) in the number of Drools ```InternalMatch``` records in certain situations.  This symptom can be consistently reproduced by the interaction of rules #5 & #6 in the ```RuleSet1Original.drl`` rule set template.  These two rules have been isolated for testing purposes in the ```RuleSet1Original-56.drl``` DRL rule set template.

You can view a dump of all ```InternalMatch``` records using the following logger setting (in ```log4j.properties```)

```
log4j.logger.com.salesforce.ncre=DEBUG
```

## Viewing Rule Execution Results
Rules in this rule set template do not update working memory (by design).  They simply return a list of applicable promotions to the caller.  You can view a dump of the resulting rule actions (executions) using the ```-da``` (```--dumpActions```) command line option.  This will print on stdout the rule exection counts for each rule defined in the rule as the following output demonstrates.

```
Actions: 
{
    "Executed UP_RS_193_Rule_NonEliglible_001-5" : 43,
    "totalRuleExecutionCount" : 43
}
```
## Stateless versus Stateful Rule Evaluation
By default the ```Engine``` executable evaluates the rules in stateful mode (using ```KieSession```).  This is specified by the ```-em``` (```--executionMode```) command line option.

### Stateful Rule Evaluation
Stateful execution is the default, used when the ```-em``` command line option is *not* specified.  In stateful mode the engine generates a sales transaction, inserts all the shopping cart facts into working memory, then executes the rules by calling ```KieSession.fireAllRules()```.

During stateful execution a standard (stateful) ```KieSession``` is created.  The ```KieSession``` object maintains all the necessary state between requests to evaluate rules, supporting incremental (delta) changes to the sales transaction data without the need to reevaluate the entire sales transaction.

Use the ```-ms``` (```--marshalSession```) command line option to marshall the ```KieSession``` after the engine runs.

### Stateless Rule Evaluation
Stateless execution must be specified using the ```-em stateless``` command line option.  In stateless mode the engine generates a sales transaction, however, instead of inserting the facts into working memory (since there is no working memory in stateless mode), a batch of facts (```InsertCommand``` instances)is created and the engine evaluates the facts in batch mode for faster processing.

Stateless mode does not support inference.  As such, a rule that fires cannot cause another rule to fire.  The engine simply matches the data to rules, then fires the rules in strict sequential manner (as they appear on the agenda).

During stateless execution a ```StatelessKieSession``` session is created instead of a ```KieSession```.  The ```StatelessKieSession``` does not have working memory associated with it, so there is no need to call insert facts as you do with a stateful session.  In stateless mode the engine creates a ```List<Command>``` wherein each ```Command``` is an instance of the ```InsertCommand``` Drools class.  It then populates the ```List``` object with one ```InsertCommand``` for each fact.  Then instead of calling ```KieSession.fireAllRules()```, it calls ```StatelessKieSession.execute(listOfInsertCommands)`` to evaluate the rules in batch mode.

# Experimental
The following is a list of experimental features being toyed with.

## PhreakActivationIteratorNcre
A local copy of the Drools ```PhreadActivationIterator``` class.  I cleaned up and commented the code and am in the process of adding graphViz output in an effort to better understand how the iterator works, and the structure of ```KieSession``` memory.  I have refactored and commented the code, but the graphViz emmitter is still very much a work in progress.

## Product Catalog
An effort to simulate a large product catelog to reduce the number of fact to rule matches at runtime.  Early results did not show an improvement in the match count explosion issues so this feature is not currently implemented.
