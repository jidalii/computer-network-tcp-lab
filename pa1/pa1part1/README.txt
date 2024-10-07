server.py is not the multi-thread 
First, we need to start server before starting client.
To run the server, do `python server.py <PORT>` in the terminal.
Then, run the client by `python client.py <HOST> <PORT>` in another terminal.

After starting the client, you would be prompted to type the sending message to the server.
If you type 'quit', the client would terminate while the server keeps running.

One thing worth reminding is that the server's receiver buffer is static. If the message size exceeds the buffer size. 
There would be an error as it cannot process the entire message as a whole.
