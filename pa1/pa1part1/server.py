import socket
import sys


def main():
    if len(sys.argv) != 2:
        print("Usage: python server.py <PORT>")
        sys.exit(1)
        
    port = int(sys.argv[1])

    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server_address = ('', port)
    print(f"Server is listening on {server_address}")
    server_socket.bind(server_address)
    server_socket.listen(1)
    print("Waiting for a connection...")
    connection, client_address = server_socket.accept()
    while True:
        msg = connection.recv(1024)
        print(f"SERVER received: {msg.decode()}")
        connection.send(msg)
    
main()