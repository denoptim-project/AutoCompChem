"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: list" --rangeformat false --verbosity 4 > cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listA" --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listB getAtomLabels" --zerobased false --labeltype indexonly --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listC getAtomLabels" --zerobased false --labeltype indexonly --rangeformat false --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listD getAtomLabels" --labeltype AtomicNumber  --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*]~[#44] prefix: listE getAtomLabels suffix:end" --labeltype IndexBased  --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*] [#44] prefix: listF geometryConditions: distance 0 1 < 2.3 subtuple: 0 getAtomLabels" --zerobased false --labeltype indexonly --rangeformat true --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[*] [#44] prefix: listG geometryConditions: distance 0 1 MAX subtuple: 0 getAtomLabels" --labeltype IndexBased --zerobased false --verbosity 4 >> cli30.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t getAtomListString --infile ../cli30.sdf --SMARTS "[#6]~[#44]~[#17] prefix: listH suffix: VALuE_PLACeHOLDER Angstrom geometryConditions: angle 0 1 2 MIN subtuple: 0 1 getAtomLabels" --labeltype IndexBased --zerobased false --verbosity 4 >> cli30.log
