#/bin/bash
echo "BASH: START-A" > t125a.log
for i in $(seq 0 10)
do
  sleep 1
  echo "BASH-A: $i" >> t125a.log
done
echo "BASH: DONE-A" >> t125a.log
exit 0
