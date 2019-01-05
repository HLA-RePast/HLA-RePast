#!/bin/sh

if [ $# -ne 1 ]; then
	echo "usage: runbasictest.sh <num_federates>"
else
	java -Djava.library.path=$JAVA_BINDING_HOME/lib testrti/BasicTest $1
fi
