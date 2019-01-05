#!/bin/sh

if [ $# -ne 1 ]; then
	echo "usage: runbasicmodel.sh <num_federates>"
else
	java -Djava.library.path=$JAVA_BINDING_HOME/lib testrti/BasicModel $1
fi
