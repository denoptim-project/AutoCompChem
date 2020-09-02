#/bin/bash
echo "BASH: START-A"
for i in $(seq 0 10)
do
  sleep 2
  echo "BASH-A: $i"
done
echo "BASH: DONE-A"
exit 0
