mkdir cli25_wdir
cp ../cli25.out cli25_wdir
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t EvaluateJob --joboutputfile cli25.out --workdir cli25_wdir  > cli25.log


