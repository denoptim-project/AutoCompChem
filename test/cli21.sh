"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t measureGeomDescriptors --infile ../cli21-mol.sdf  --smarts "dih [#1] [Ru] [#6] [#7]" --onlybonded  > cli21-a.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t measureGeomDescriptors --infile ../cli21-mol.sdf  --smarts "dih [#1] [Ru] [#6]" --onlybonded  > cli21-b.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t measureGeomDescriptors --infile ../cli21-mol.sdf  --smarts "dih [#1] [Ru] [#6] [#7] [#7]" --onlybonded  > cli21-c.log

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t measureGeomDescriptors --infile ../cli21-mol.sdf  --smarts "dih [#1] [Ru] [#6] [#2]" --onlybonded  > cli21-d.log


