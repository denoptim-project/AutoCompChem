"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../cli15-a.json > cli15-a.log
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t analyseACCOutput --jobOutputFile cli15-a.log
