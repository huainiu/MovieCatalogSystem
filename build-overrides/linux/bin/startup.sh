#!/bin/bash

scriptname=$(echo $0 | sed s/.*\\///g)

if [ "$0" = "./$scriptname" ];then
abspath=$(echo `pwd`/$0 | sed "s/\/$scriptname//g" | sed 's/\/\.//g')
else
abspath=$(echo $0 | sed "s/\/$scriptname//g")
fi

cd $abspath

jars=$(find ../lib/ -name \*.jar | xargs echo | sed s/\ /:/g)

javaw -classpath $jars net.milanaleksic.mcs.application.Startup
