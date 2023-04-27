#!/usr/bin/env bash

function flushRedoLogToDisk() {

  java -Dcom.gs.jini_lus.groups=xap-16.2.1 \
       -Dcom.gs.jini_lus.locators=localhost \
       -jar target/redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar FlushRedoLogToDisk --spaceName=redolog
}

function deserializeRedoLog() {

  java \
       -Dcom.gs.home=$HOME/backup \
       -jar target/redolog-client-1.0-SNAPSHOT-jar-with-dependencies.jar DeserializeRedoLog --spaceName=redolog --containerName=redolog_container1 --outputFileName=myredolog.yaml
}

PWD=$(pwd)

TARGET_CLASSES_DIR="`dirname \"$0\"`"
REDOLOG_PROJ_DIR="`( cd \"$TARGET_CLASSES_DIR/../../\" && pwd )`"

cd $REDOLOG_PROJ_DIR

deserializeRedoLog

cd $PWD