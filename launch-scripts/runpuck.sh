#!/bin/sh
# java -cp /opt/MeSC/java/repast-2.0/lib/repast.jar:hlapast.jar:hlalib.jar:testrti.jar:/usr/globus/lib/axis.jar testrti.PuckModel test $1

if [ $# -ne 1 ]; then
	echo "usage: runpuck.sh <first: true|false>"
else
	java -Djava.library.path=$JAVA_BINDING_HOME/lib testrti/PuckModel $1
fi
