#!/bin/sh
TARGET=/tmp`pwd`
ssh -p 2222 vagrant@localhost "mkdir -p $TARGET"
rsync -av --delete --progress -e 'ssh -p 2222' . vagrant@localhost:$TARGET
ssh -p 2222 vagrant@localhost "cd $TARGET;export JAVA_HOME=~/jdk1.8.0;mvn install"
rsync -av --progress -e 'ssh -p 2222' vagrant@localhost:$TARGET/target .
rsync -av --progress -e 'ssh -p 2222' vagrant@localhost:.m2/repository/com/github/spullara/java8 ~/.m2/repository/com/github/spullara
