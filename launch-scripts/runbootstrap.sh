#!/bin/sh

if [ $# -ne 1 ]; then
	echo "usage: runbootstrap.sh <num_federates>"
else
	java -Djava.library.path=$JAVA_BINDING_HOME/lib testrti/BootstrapModel $1
fi
