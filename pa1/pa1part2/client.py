import socket
import http_resp
# import argparse
import string
import random
import sys
import time

class TCPClient:
    def __init__(self, inputs):
        self.server_addr = (inputs['ip'], inputs['port'])
        self.measure_type = inputs['measure']
        self.probes_num = int(inputs['probes'])
        self.msg_size = int(inputs['bytes'])
        self.server_dalay = int(inputs['delay'])
        self.payload = ""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        
    def connect(self):
        self.socket.connect(self.server_addr)

    def generate_payload(self):
        characters = string.digits + string.ascii_letters
        random_string = ''.join(random.choice(characters) for _ in range(self.msg_size))
        self.payload = random_string

    def construct_init_msg(self)-> str:
        return f"s {self.measure_type} {self.probes_num} {self.msg_size} {self.server_dalay}\n"

    def construct_prob_msg(self, seq_num) -> str:
        return f"m {seq_num} {self.payload}\n"

    def construct_termination_msg(self)-> str:
        return 't\n'

def parse_args():
    if len(sys.argv) != 7:
        print("Usage: python client.py <HOST> <PORT> <MEASURE> <MSG_SIZE> <PROBES_NUM> <DELAY>")
        sys.exit(1)
        
    args = {}
    args['ip'] = str(sys.argv[1])
    args['port'] = int(sys.argv[2])
    args['measure'] = str(sys.argv[3])
    args['bytes'] = int(sys.argv[4])
    args['probes'] = int(sys.argv[5])
    args['delay'] = int(sys.argv[6])
    
    return args

def main():
    # 0. Setup client
    inputs = parse_args()
    
    client = TCPClient(inputs)
    client.connect()

    # 1. Connection Phase
    conn_msg = client.construct_init_msg()
    client.socket.send(conn_msg.encode())
    response = client.socket.recv(1024).decode()

    # 2. Probing Phase
    if response == http_resp.CONN_RESP_404:
        print(http_resp.CONN_RESP_404)
        print("CLIENT: terminated")
        return
    elif response == http_resp.CONN_RESP_200:
        client.generate_payload()
        start_ts = time.time_ns()
        for i in range(1, inputs['probes']+1):
            msg = client.construct_prob_msg(i)
            client.socket.send(msg.encode())
            resp = client.socket.recv(client.msg_size*10)
            resp = resp.decode()
            if response == http_resp.CONN_RESP_200:
                continue
            elif resp == http_resp.CONN_RESP_404:
                print(http_resp.CONN_RESP_404)
                return
            else:
                print("CLIENT: unknown resp during probe process:\n", resp)
                client.socket.close()
                return
        end_ts = time.time_ns()    
        
    # 3. Termination Phase
    msg = client.construct_termination_msg()
    client.socket.send(msg.encode())
    resp = client.socket.recv(1024).decode()
    
    client.socket.close()
    
    total_time = (end_ts-start_ts)/1e9
    RTT = total_time/client.probes_num
    tput = client.probes_num / total_time
    if client.measure_type == 'tput':
        print(f"CLIENT-{client.msg_size}-tput-{tput}")
    elif client.measure_type == 'rtt':
        print(f"CLIENT-{client.msg_size}-RTT-{RTT}")
    
    
    if resp == http_resp.CLOSE_RESP_200:
        return
    elif resp == http_resp.CLOSE_RESP_404:
        print(http_resp.CONN_RESP_404)
        return
    
    
main()