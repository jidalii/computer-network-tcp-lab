First, we need to start server before starting client.
To run the server, do `python server.py <PORT>` in the terminal.
Then, run the client by `python client.py <HOST> <PORT> <MEASURE> <MSG_SIZE> <PROBES_NUM> <DELAY>` in another terminal.

`http_resp.py` is the file of constants for server responses

`start_client.sh` and `start_server.sh` are shell command files provided for running the data analysis on RTT and Throughput.
