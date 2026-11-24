#!/bin/sh
# Runs every program once and stores the raw output next to each task. (~6 minutes in total)
cd "$(dirname "$0")"
./compile.sh || exit 1
CP="out:lib/*"
run() { rm -rf testdb; java "$@" ; }
run -cp "$CP" StoreDemo        > Task01/output/storedemo_output.txt
run -Xmx1g -cp "$CP" LineCountBench > Task01/output/linecount_output.txt
run -Xmx1g -cp "$CP" SearchBench    > Task01/output/search_output.txt
run -cp "$CP" LoginDemo        > Task02/output/login_output.txt
run -cp "$CP" DamagedDataTest  > Task02/output/damaged_output.txt
run -cp "$CP" PassLoader       > Task03/output/loader_output.txt
run -Xmx2g -cp "$CP" PassBench > Task03/output/bench_output.txt
run -cp "$CP" MakeLegacyFile
run -cp "$CP" Migrate          > Task04/output/migrate_output.txt
run -cp "$CP" ConcurrencyDemo  > Task04/output/concurrency_output.txt
run -Xmx5g -cp "$CP" ScaleBench > Task04/output/scale_output.txt
rm -rf testdb derby.log Task04/data/ser Task04/data/scale_800k.txt
echo "all done - outputs are in Task*/output"
