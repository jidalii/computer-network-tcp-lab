import socket
import http_resp
import argparse
import string
import random

class TCPClient:
    def __init__(self, inputs):
        self.server_addr = (inputs.ip, inputs.port)
        self.measure_type = inputs.measure_type
        self.probes_num = inputs.probes_num
        self.msg_size = inputs.msg_size
        self.server_dalay = inputs.server_dalay
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

    parser.add_argument('-t', '--type', type=str, choices=['rtt', 'tput'], default='rtt',
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
    client.socket.send(conn_msg.encode())
    response = client.socket.recv(1024).decode()

    # 2. Probing Phase
    if response == http_resp.CONN_RESP_200:
        return
    elif response == http_resp.RESP_200:
        client.generate_payload()
        for i in range(1, inputs.probes+1):
            msg = client.construct_prob_msg(i)
            client.socket.send(msg.encode())
            resp = client.socket.recv(1024).decode()
            if response == http_resp.CONN_RESP_200:
                pass
            elif resp == http_resp.CONN_RESP_404:
                return
            else:
                print("CLIENT: unknown resp during probe process:\n", resp)
                client.socket.close()
                return
            
    # 3. Termination Phase
    msg = client.construct_termination_msg()
    client.socket.send(msg.encode())
    resp = client.socket.recv(1024).decode()
    
    client.socket.close()
    if resp == http_resp.CLOSE_RESP_200:
        return
    elif resp == http_resp.CLOSE_RESP_404:
        return
    
main()