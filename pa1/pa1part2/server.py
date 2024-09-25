import socket
import sys
import http_resp
import time

class TCPServer:
    def __init__(self) -> None:
        self.measure_type = ""
        self.probes_num = 0
        self.msg_size = 0
        self.server_delay = 0
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        self.conn_socket: socket.socket = None
    
    def validate_client_conn_msg(self, msg: str) -> bool:
        if msg[-1] != '\n':
            return False
        msg_arr = msg.split(" ")

        if len(msg_arr) != 5:
            return False

        protocol, measure_type, probes_num, msg_size, server_delay = msg_arr

        if protocol != 's' or measure_type not in {"rtt", "tput"}:
            return False

        try:
            probes_num = int(probes_num)
            msg_size = int(msg_size)
            server_delay = int(server_delay)

            if probes_num <= 0 or msg_size <= 0 or server_delay < 0:
                return False

            self.measure_type = measure_type
            self.probes_num = probes_num
            self.msg_size = msg_size
            self.server_delay = server_delay
            return True
        except ValueError:
            return False

    def validate_client_probe_msg(self, msg: str, seq_num: int) -> bool:
        if msg[-1] != '\n':
            return False
        
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
    if len(sys.argv) != 2:
        print("Usage: python server.py <PORT>")
        sys.exit(1)
    port = int(sys.argv[1])

    server = TCPServer()
    server_address = ("localhost", port)
    server.socket.bind(server_address)
    server.socket.listen(1)

    while True:
        server.conn_socket, client_address = server.socket.accept()
        msg = server.conn_socket.recv(1024).decode()
        is_valid = server.validate_client_conn_msg(msg)
        if not is_valid:
            server.conn_socket.send(http_resp.CONN_RESP_404.encode())
            server.conn_socket.close()
            return
        else:
            server.conn_socket.send(http_resp.CONN_RESP_200.encode())
            for i in range(1, server.probes_num+1):
                msg = server.conn_socket.recv(server.msg_size)
                recv_ts = time.time_ns()
                msg.decode()
                if not server.validate_client_probe_msg(msg, i):
                    server.conn_socket.send(http_resp.PROB_RESP_404.encode())
                end_ts = time.time_ns()
                print(f"recv_ts:{recv_ts},end_ts:{end_ts}")
            
            msg = server.conn_socket.recv(1024).decode()
            if not server.validate_client_termination_msg(msg):
                server.conn_socket.send(http_resp.CLOSE_RESP_404.encode())
            else:
                server.conn_socket.send(http_resp.CLOSE_RESP_200.encode())
                
                
            

main()