#!/bin/bash
# This script fakes the execution of an XTB job by copying files from saved folders
# to reproduce the generation of output files by XTB.
# The fake output is defined in the folders ../../cli24_out_tmpl_* and is intentionally
# made dependent on the last index, to simulate a different outcome from different
# reruns of the workflow.
# Note that this script assumes the work directory of the caller job is not the usual
# 'results' folder used for most test runs, but is a subdirectory configures in the
# definition ofthe job. Hence, we the hard-coded relative pathway starts with ../../

echo "Running fake job runner"
echo "pwd=$(pwd)"
rerunIdx=$(find . -name "cli24.out" | wc -l | awk '{print $1}')
cp ../../cli24_out_tmpl_${rerunIdx}/* .

