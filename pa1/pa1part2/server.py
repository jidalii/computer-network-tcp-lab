import socket
import sys
import http_resp
import time
from signal import signal, SIGPIPE, SIG_DFL   


class TCPServer:
    def __init__(self) -> None:
        self.measure_type = ""
        self.probes_num = 0
        self.msg_size = 0
        self.delay = 0
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.conn_socket: socket.socket = None
    
    def validate_client_conn_msg(self, msg: str) -> bool:
        if msg[-1] != '\n':
            return False
        msg_arr = msg.strip().split(" ")

        if len(msg_arr) != 5:
            return False

        protocol, measure_type, probes_num, msg_size, delay = msg_arr

        if protocol != 's' or measure_type not in {"rtt", "tput"}:
            return False

        try:
            probes_num = int(probes_num)
            msg_size = int(msg_size)
            delay = int(delay)

            if probes_num <= 0 or msg_size <= 0 or delay < 0:
                return False

            self.measure_type = measure_type
            self.probes_num = probes_num
            self.msg_size = msg_size
            self.delay = delay
            return True
        except ValueError:
            return False

    def validate_client_probe_msg(self, msg: str, seq_num: int) -> bool:
        if msg[-1] != '\n':
            return False
        msg = msg.strip()
        msg_arr = msg.split(" ")
        if len(msg_arr) != 3:
            return False
        if msg_arr[0] != 'm':
            return False
        
        try:
            msg_req_num = int(msg_arr[1])
            if msg_req_num != seq_num:
                return False
            return True
        except ValueError:
            return False
        
    def validate_client_termination_msg(self, msg: str) -> bool:
        return msg == "t\n"

def main():
    signal(SIGPIPE,SIG_DFL) 
    if len(sys.argv) != 2:
        print("Usage: python server.py <PORT>")
        sys.exit(1)
    port = int(sys.argv[1])

    server = TCPServer()
    server_address = ("", port)
    server.socket.bind(server_address)
    server.socket.listen(1)
    print(f"Server is listening on {server_address}")
    print("Waiting for a connection...")

    while True:
        server.conn_socket, _ = server.socket.accept()
        nack_msg = server.conn_socket.recv(2048).decode()
        is_valid = server.validate_client_conn_msg(nack_msg)
        if not is_valid:
            err = http_resp.CONN_RESP_404.encode()
            server.conn_socket.sendall(err)
            server.conn_socket.close()
            print(http_resp.CONN_RESP_404)
            return
        else:
            ack_msg = http_resp.CONN_RESP_200.encode()
            server.conn_socket.sendall(ack_msg)
            # recv probing msg from clients
            
            for i in range(1, server.probes_num+1):
                msg = server.conn_socket.recv(server.msg_size+200)
                if server.delay != 0:
                    time.sleep(server.delay/1000)
                msg = msg.decode()
                if not server.validate_client_probe_msg(msg, i):
                    nack_msg = http_resp.PROB_RESP_404.encode()
                    server.conn_socket.sendall(nack_msg)
                    server.conn_socket.close()
                    print(http_resp.PROB_RESP_404)
                    return
                print(f"SERVER,RECV,{msg}")
                msg = msg.encode()
                server.conn_socket.sendall(msg)
            
            msg = server.conn_socket.recv(1024)
            msg = msg.decode()
            if not server.validate_client_termination_msg(msg):
                nack_msg = http_resp.CLOSE_RESP_404.encode()
                server.conn_socket.sendall(nack_msg)
            else:
                ack_msg = http_resp.CLOSE_RESP_200.encode()
                server.conn_socket.sendall(ack_msg)
            server.conn_socket.close()
            return
                
main()