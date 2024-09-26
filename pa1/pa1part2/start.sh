#!/bin/bash

packet_size=100
port=58000

# Start the server in the background and log to log_server.txt
python server.py $port >> ./data/log_server_${packet_size}.txt 2>&1 &

# Wait for 1 second to give the server time to start
sleep 1

# Start the client in the foreground and log to log_client.txt
python client.py localhost $port -t rtt -b $packet_size -p 10 -d 0 >> ./data/log_client_${packet_size}.txt 2>&1

# Wait for all background processes to finish
wait
