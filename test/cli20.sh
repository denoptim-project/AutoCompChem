"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t ricalculateconnectivity --targetElement Ru --infile ../cli20-mol.xyz --outFile cli20_RuOnly.sdf


"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t ricalculateconnectivity --infile ../cli20-mol.xyz --outFile cli20_all.sdf

