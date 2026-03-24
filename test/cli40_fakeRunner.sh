#!/bin/bash
# This script fakes the execution of a CREST job meant to use an XTB input file for 
# some run cofnigurations. This script simply copies files from saved folders
# to reproduce the generation of output files by CREST.

echo "Running fake job runner"
echo "pwd=$(pwd)"

rerunIdx=$(find . -name "cli40.out" | wc -l | awk '{print $1}')
cp -r ../../cli40_out_tmpl_${rerunIdx}/* .

