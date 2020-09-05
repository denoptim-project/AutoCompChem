#/bin/bash
echo "BASH: START-A" > t124a.log
for i in $(seq 0 3)
do
  sleep 1
  echo "BASH-A: $i" >> t124a.log
done
echo "BASH: DONE-A" >> t124a.log
exit 0
