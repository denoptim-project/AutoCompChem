"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: list" --rangeformat false --verbosity 4 > cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listA" --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listB getAtomLabels" --zerobased false --labeltype indexonly --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listC getAtomLabels" --zerobased false --labeltype indexonly --rangeformat false --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listD getAtomLabels" --labeltype AtomicNumber  --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listE getAtomLabels suffix:end" --labeltype IndexBased  --verbosity 4 >> cli30.log

