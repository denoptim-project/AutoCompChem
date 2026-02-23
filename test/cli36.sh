# remove file that will be created by analysis
rm ../cli36_wdir/step_0_result.sdf

"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli36.json  > cli36.log


