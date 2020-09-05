#/bin/bash
echo "BASH: START-B" > t124b.log
for i in $(seq 0 2)
do
  sleep 1
  echo "BASH-B: $i" >> t124b.log
done
echo "BASH: DONE-B" >> t124b.log
exit 0
