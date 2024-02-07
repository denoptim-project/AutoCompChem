# This is a test that is alsmost equal to cli4 but triggers an error in a job performed from within a Directive. Therefore, this is run of AutoCompChem is meant to fail
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" --params ../cli9.params --StringFromCLI ../cli9-mol
