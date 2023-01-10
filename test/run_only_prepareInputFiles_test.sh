#!/bin/bash
rm -r results/*
for id in 10 116 129 131 134 137 48 54 6 61 67 73 80 84 91 92 93 94 95 96
do
  echo "Test $id"
  ./run_tests.sh -f $id
  if [ $? -ne 0 ]; then
    echo "Stopping"
    exit -1
  fi
done
