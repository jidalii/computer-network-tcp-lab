#!/bin/bash

port=58300
# packet_sizes=(1 100 200 400 800 1000) # rtt
packet_sizes=(1000 2000 4000 8000 16000 32000) # tput
delay=0
measure="tput"
probes=10

log_file="./data/log_client_${measure}_${delay}_delay_1.txt"

> $log_file

for i in "${packet_sizes[@]}"; do
    port=$((port + 1))

    python client.py csa2.bu.edu $port $measure $i $probes $delay >>$log_file 2>&1

    sleep 1
done

wait
