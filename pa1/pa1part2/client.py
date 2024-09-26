import socket
import http_resp
import argparse
import string
import random
import time

class TCPClient:
    def __init__(self, inputs):
        self.server_addr = (inputs.ip, inputs.port)
        self.measure_type = inputs.measure
        self.probes_num = int(inputs.probes)
        self.msg_size = int(inputs.bytes)
        self.server_dalay = int(inputs.delay)
        self.payload = ""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        
    def connect(self):
        self.socket.connect(self.server_addr)

    def generate_payload(self):
        characters = string.ascii_letters + string.digits
        random_string = ''.join(random.choice(characters) for _ in range(self.msg_size))
        return random_string

    def construct_init_msg(self)-> str:
        return f"s {self.measure_type} {self.probes_num} {self.msg_size} {self.server_dalay}\n"

    def construct_prob_msg(self, seq_num) -> str:
        return f"m {seq_num} {self.payload}\n"

    def construct_termination_msg(self)-> str:
        return 't\n'

def parse_args():
    parser = argparse.ArgumentParser(description='Process some network parameters.')

    parser.add_argument('ip', type=str, help='Host IP')
    parser.add_argument('port', type=int, help='Port number')

    parser.add_argument('-t', '--measure', type=str, choices=['rtt', 'tput'], default='rtt',
                        help='Measurement Type, e.g., rtt')
    parser.add_argument('-b', '--bytes', type=int, help='Size of the data in bytes', default=1)
    parser.add_argument('-p', '--probes', type=int, help='Number of probes', default=1)
    parser.add_argument('-d', '--delay', type=int, help='The delay time in ms', default=0)

    args = parser.parse_args()
    return args

def main():
    # 0. Setup client
    inputs = parse_args()
    
    client = TCPClient(inputs)
    client.connect()


    # 1. Connection Phase
    conn_msg = client.construct_init_msg()
    print(f"CLIENT-conn_msg:{conn_msg}")
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
        for i in range(1, inputs.probes+1):
            # print(f"CLIENT-sending msg num#{i}")
            msg = client.construct_prob_msg(i)
            client.socket.send(msg.encode())
            resp = client.socket.recv(client.msg_size).decode()
            if response == http_resp.CONN_RESP_200:
                continue
            elif resp == http_resp.CONN_RESP_404:
                print(http_resp.CONN_RESP_404)
                print("CLIENT: terminated")
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
    print(f"CLIENT-total_time:{total_time}")
    print(f"CLIENT-RTT:{RTT}")
    print(f"CLIENT-tput:{tput}")
    
    if resp == http_resp.CLOSE_RESP_200:
        return
    elif resp == http_resp.CLOSE_RESP_404:
        print(http_resp.CONN_RESP_404)
        return
    print("CLIENT: terminated")
    
    
main()