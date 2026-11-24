#!/usr/bin/env bash
# Task 6: compile, run the GUI driver (virtual display) and the benchmark.
set -e
cd "$(dirname "$0")/src"
mkdir -p ../bin && javac -d ../bin $(find . -name "*.java")
java -cp ../bin ui.Bench
