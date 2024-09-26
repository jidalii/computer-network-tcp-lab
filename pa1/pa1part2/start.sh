#!/bin/bash

packet_size=100
port=58500
packet_sizes=(1 100 200 400 800 1000)
# packet_sizes=(1000 2000 4000 8000 16000 32000)

for i in "${packet_sizes[@]}"; do
    port=$((port + 1))
    python server.py $port >> ./data/log_server_tput.txt 2>&1 &

    sleep 2

    python client.py localhost $port tput $i 1200 0 >> ./data/log_client_tput.txt 2>&1

    wait
done