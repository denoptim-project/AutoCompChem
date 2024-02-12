"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t "prepare input orca  " --inputGeometriesFile ../cli8.xyz --charge 0 --spin_multiplicity 3 --jobDetails "\$DIR_! \$MT_all=UHF DLPNO-CCSD def2-TZVPP def2-TZVPP/C def2/J RIJCOSX CPCM(acetonitrile)
\$DIR_coords \$DIR_coords \$DATA_xyz=\$ACCTASK: getMolecularGeometry
\$DIR_geom \$DIR_constraints \$DATA_constraints=\$START\$ACCTASK: GenerateConstraints
VERBOSITY: 2
SMARTS:\$START [#6]
[#8] [#1]
\$END
\$END"
