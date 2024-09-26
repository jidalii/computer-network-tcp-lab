import socket
import sys

if len(sys.argv) != 2:
    print("Usage: python server.py <PORT>")
    sys.exit(1)
    
port = int(sys.argv[1])

server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_address = ('', port)
server_socket.bind(server_address)
server_socket.listen(1)
connection, client_address = server_socket.accept()
while True:
    msg = connection.recv(1024)
    connection.send(msg)
    