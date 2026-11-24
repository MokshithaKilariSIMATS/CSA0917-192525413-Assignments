#!/bin/sh
# Compile every task into ./out
cd "$(dirname "$0")"
mkdir -p out
javac -cp "lib/*" -d out Task01/*.java Task02/*.java Task03/*.java Task04/*.java && echo "compiled OK"
