import socket
import sys
import threading

def handle_client(connection: socket.socket, client_address: socket._RetAddress):
    try:
        print(f"Connection from {client_address}")
        
        # Receive the data in small chunks and echo it back
        while True:
            data = connection.recv(1024)
            if data:
                print(f"SERVER:{client_address},received,{data.decode()}")
                print("Echoing data back to the client")
                connection.sendall(data)
            else:
                print("No more data from", client_address)
                break
    finally:
        # Clean up the connection
        connection.close()

def server(port):
    # Create a TCP/IP socket
    server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    
    # Bind the socket to the port
    server_address = ('', port)
    server_socket.bind(server_address)
    
    # Listen for incoming connections
    server_socket.listen(5)
    print(f"Starting server on port {port}")
    
    while True:
        print("Waiting for a connection...")
        connection, client_address = server_socket.accept()
        client_thread = threading.Thread(target=handle_client, args=(connection, client_address))
        client_thread.start()

def main():
    if len(sys.argv) != 2:
        print(f"Usage: {sys.argv[0]} <port>")
        sys.exit(1)

    port = int(sys.argv[1])
    server(port)
    
main()