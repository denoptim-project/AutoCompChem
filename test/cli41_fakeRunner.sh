#!/bin/bash
# This script fakes the execution of a CREST job that runs while creating a 
# situation that requires a reaction.

echo "Running fake job runner"
echo "pwd=$(pwd)"

rerunIdx=$(find . -name "cli41.out" | wc -l | awk '{print $1}')
cp -r ../../cli41_out_tmpl_${rerunIdx}/* .

# Somulate job hangs in the first two attempts
if [ 2 -gt "$rerunIdx" ]; then
  # Simulate the running for 2 seconds...
  for i in 1 2
  do
    echo "Fake running job iteration $i: $(date)" >> cli41.out
    sleep 1
  done

  # ...then simulate a situation that requires a reaction...
  echo "ERROR msg triggering reaction of monitoring job" >> cli41.out

  # ...and hang for up to 5 seconds, but this process should be interrupted before that!
  for i in $(seq 1 5)
  do
    echo "Fake running job iteration $i: $(date)" >> cli41.out
    sleep 1
  done
  echo "End of simulated situation-triggering run!" >> cli41.out
else
  echo "No faking of situation-triggering run in iteration $rerunIdx" >> cli41.out
  sleep 1
  echo "End of normal behavior" >> cli41.out
fi


