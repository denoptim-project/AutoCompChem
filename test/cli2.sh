"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t PruneMolecules --InFile ../cli2.sdf --OutFile cli2-pruned.sdf --SMARTS "[He] [#7]~[#6]~[#7]" --verbosity 5
