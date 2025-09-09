"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t analyseOutput --jobOutputFile ../cli26_good.out --requireNormalTermination true > cli26_good_w.log
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t analyseOutput --jobOutputFile ../cli26_bad.out --requireNormalTermination false > cli26_bad_wo.log
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t analyseOutput --jobOutputFile ../cli26_bad.out --requireNormalTermination true > cli26_bad_w.log


