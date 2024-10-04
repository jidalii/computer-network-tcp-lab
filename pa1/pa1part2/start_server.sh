#!/bin/bash

port=58040

delay=0
measure="rtt"
packet_sizes=(1 100 200 400 800 1000)

log_file="./data/log_server_${measure}_${delay}_delay_1.txt"

# Create the log directory if it doesn't exist
mkdir -p ./data

> $log_file

for i in "${packet_sizes[@]}"; do
    port=$((port + 1))

    python server.py $port >> $log_file 2>&1 &

done

wait