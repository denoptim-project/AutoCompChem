#/bin/bash
echo "BASH: START-B"
for i in $(seq 0 10)
do
  sleep 3
  echo "BASH-B: $i"
done
echo "BASH: DONE-B"
exit 0
