#!/bin/bash

if [ -z "$1" ]; then
    echo "Launcher script path not specified"
fi

if [ -z "$2" ]; then
    echo "Binary path not specified"
fi

if [ -z "$3" ]; then
    echo "Output path not specified"
fi

cat $1 $2 > $3
chmod +x $3
