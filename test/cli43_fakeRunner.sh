#!/bin/bash
# This script fakes the execution of a job by copying files from previous runs (actual or simulated).

echo "Running fake job runner"
echo "pwd=$(pwd)"

rerunIdx=$(find . -name "cli43.out" | wc -l | awk '{print $1}')
cp -r ../../cli43_out_tmpl_${rerunIdx}/* .

