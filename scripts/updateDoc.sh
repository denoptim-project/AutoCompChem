#!/bin/bash
find . -type f -name "*.java" | xargs javadoc -d doc -classpath ../lib/cdk-1.4.14.jar:../lib/guava-20.0.jar -linkoffline http://docs.oracle.com/javase/7/docs/api ../doc/external_api_java8 -linkoffline http://cdk.github.io/cdk/1.4/docs/api ../doc/external_api_cdk_1.4

