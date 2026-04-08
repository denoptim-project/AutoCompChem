echo "dummy content" > cli45_out.sdf
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli45.json  > cli45_A.log
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli45.json --overwrite > cli45_B.log
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli45_nested.json  > cli45_nested_A.log
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli45_nested.json --overwrite > cli45_nested_B.log


