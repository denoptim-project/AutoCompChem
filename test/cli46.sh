mkdir cli46_run
cd cli46_run
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -j ../../cli46.json  --lowmem > ../cli46.log
cd ..
