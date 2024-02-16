# Checks that all help messages produce a normal termination
"$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -h | grep " *\->" | awk '{print $2}' > cli11_taskList

while IFS= read task <&3; do
  echo Task: $task
  "$javaDir/java" -jar "$ACCHome/target/autocompchem-$accVersion-jar-with-dependencies.jar" -t "$task" -h  > cli11_$task.log 2>&1
  if grep -q Error cli11_$task.log ; then
    echo NOT Passed: error while getting help for task $task
    break
  fi
  if grep -q Throw cli11_$task.log ; then
    echo NOT Passed: error while getting help for task $task
    break
  fi
done 3< cli11_taskList
