"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" --infile ../cli18.par --outFile cli18.json -t convertJobDefinition

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" --infile cli18.json --outFile cli18.par2 --outFormat par -t convertJobDefinition
