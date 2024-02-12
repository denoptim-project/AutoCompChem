"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t prepareInputOrca --inputGeometriesFile ../cli10-mol.sdf --rootPathnameOutput cli10-mol --charge 0 --spin_multiplicity 3 --jobDetails "\$DIR_! \$MT_all=UHF
\$DIR_coords \$DIR_coords \$DATA_xyz=\$ACCTASK: getMolecularGeometry"
