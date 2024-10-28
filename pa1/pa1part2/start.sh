#!/bin/bash

port=58090
# packet_sizes=(1 100 200 400 800 1000) # rtt
packet_sizes=(1000 2000 4000 8000 16000 32000) # tput
delay=200
measure="tput"
probes=15

> ./data/log_server_${measure}_${delay}_delay_1.txt
> ./data/log_client_${measure}_${delay}_delay_1.txt

for i in "${packet_sizes[@]}"; do
    port=$((port + 1))

    python server.py $port >> ./data/log_server_${measure}_${delay}_delay_1.txt 2>&1 &

    sleep 1

    python client.py localhost $port $measure $i $probes $delay >> ./data/log_client_${measure}_${delay}_delay_1.txt 2>&1

    wait
done