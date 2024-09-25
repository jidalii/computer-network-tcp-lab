import socket
import sys

if len(sys.argv) != 3:
    print("Usage: python client.py <HOST> <PORT>")
    sys.exit(1)

host = str(sys.argv[1])
port = int(sys.argv[2])
server_addr = (host, port)

client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
client_socket.connect(server_addr)

msg = 'hello computer network!'

client_socket.send(msg.encode('utf-8'))
recv_msg = client_socket.recv(1024)
print(recv_msg.decode('utf-8'))
client_socket.close()