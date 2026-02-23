# simulate the creation of the work dir 
rm -rf cli37_wdir
mkdir cli37_wdir
cp ../cli37.out ../cli37-jobDetails.json cli37_wdir/

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli37.json  > cli37.log


