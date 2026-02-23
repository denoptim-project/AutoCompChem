# Test the interaction between setting workdir in the compChemJob and in the workers dealing with it

#
# Case A: No setting of workdir in either worker of ccJob, but we do set the pathname root
# NB: do it in a subfolder to avoid interference from other archiviation tasks
#
rm -rf cli38_wdir_A
mkdir cli38_wdir_A
cp ../cli38.out ../cli38-jd_A.json cli38_wdir_A
cd cli38_wdir_A
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../../cli38_A.json  > ../cli38_A.log
cd ..

#
# Case B: wdir set in worker, not in ccJob
#
rm -rf cli38_wdir_B
mkdir cli38_wdir_B
cp ../cli38.out ../cli38-jd_B.json cli38_wdir_B
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli38_B.json  > cli38_B.log

#
# Case C: wdir set in ccJob not in worker, but worker defines different pathname root
#
rm -rf cli38_wdir_C
mkdir cli38_wdir_C
cp ../cli38.out ../cli38-jd_C.json cli38_wdir_C
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli38_C.json  > cli38_C.log

#
# Case D: wdir set in both worker and ccJob 
#
rm -rf cli38_wdir_D
mkdir cli38_wdir_D
cp ../cli38.out ../cli38-jd_D.json cli38_wdir_D
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli38_D.json  > cli38_D.log

