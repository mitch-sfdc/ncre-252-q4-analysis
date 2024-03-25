# ncre-252-q4-analysis
Provide source code examples to overcome the issues discovered during 252 Q4 testing of the Near Core Rules Engine (NCRE).

The process for using this project is to first run the compiler to generate a ```KJAR``` rule library executable from a DRL source template file.  Then you can execute the rule library using the engine.

The two executable classes and their various command line options are defined below.

# Executables
There are two executable files in this project.  The ```com.salesforce.ncre.Compiler``` class reads a DRL template file, populates the template variables and compiles the resulting DRL files into a ```KJAR``` file. The ```com.salesforce.ncre.Engine``` class reads a previously compiled ```KJAR``` file, creates a knowledge base ```KieContainer``` / ```KieBase```, then generates a sales transaction payload comprised of one ```industries.nearcore.rule.engine.cartDetails``` header and zero or more related ```industries.nearcore.rule.engine.cartLineDetails``` objects.  The sales transaction is then inserted into working memory (in the stateful case) and a call to ```KieSession.fireAllRules()``` made to execute all the rules.

## Compiler
The ```Compiler``` class supports the following command line options.

```
usage: java [options] com.salesforce.ncre.engine
 -df,--drlFilename <arg>               The source DRL filename
 -dpc,--duplicatePriorityCount <arg>   The total number of desired
                                       duplicate priorities (default: 0)
 -grp,--generateRulePriorities         Generate the rule priorities
                                       relative to the rule set priorities
                                       (default: false)
 -h,--help                             Show help menu
 -kf,--kjarFilename <arg>              The generated KJAR filename
 -lib,--libraryName <arg>              The organization name used in the
                                       release ID and default KJAR name
 -on,--orgName <arg>                   The organization name used in the
                                       release ID and default KJAR name
 -pcs,--productCatalogSize <arg>       The total number of products in the
                                       product catalog (default: -1
                                       (sequential))
 -rc,--rulesetCount <arg>              The total number of rule sets to
                                       generate (default : 500)
 -rem,--ruleExecModel                  Generate the KJAR using the Drools
                                       Rule Exec Model [true|false]
                                       (default : true)
 -ver,--version <arg>                  The version used in the release ID
                                       and default KJAR name
```

## Engine
The ```Engine``` class supports the following command line options.

```
usage: java [options] com.salesforce.ncre.engine
 -c,--compile                           Compile rules before executing
                                        (default : false)
 -dsc,--deltaSyncCount <arg>            The number of delta sync
                                        operations to run for stateful
                                        execution (default: 1)
 -dump,--dumpActions                    Dump all actions to JSON file
                                        [true|false] (default: false)
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
 -rem,--ruleExecModel                   Run the rule engine using the
                                        Drools Rule Executable Model mode
                                        (default: false)
 -tli,--totalLineItems <arg>            Specify the total number of line
                                        items to be generated (default :
                                        200)
 -tmli,--totalMatchingLineItems <arg>   Specify the total number of
                                        matching line items to be
                                        generated (default: 40)
```
