import socket
import http_resp

import string
import random
import sys
import time


class TCPClient:
    def __init__(self, inputs):
        self.server_addr = (inputs["ip"], inputs["port"])
        self.measure_type = inputs["measure"]
        self.probes_num = int(inputs["probes"])
        self.msg_size = int(inputs["bytes"])
        self.server_dalay = int(inputs["delay"])
        self.payload = ""
        self.socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)

    def connect(self):
        self.socket.connect(self.server_addr)

    def generate_payload(self):
        characters = string.digits + string.ascii_letters
        random_string = "".join(random.choice(characters) for _ in range(self.msg_size))
        self.payload = random_string

    def construct_init_msg(self) -> str:
        return f"s {self.measure_type} {self.probes_num} {self.msg_size} {self.server_dalay}\n"

    def construct_prob_msg(self, seq_num) -> str:
        return f"m {seq_num} {self.payload}\n"

    def construct_termination_msg(self) -> str:
        return "t\n"

    def summary(self) -> str:
        return f"""server_addr: {self.server_addr}\n\
measure_type: {self.measure_type}\n\
probes_number: {self.probes_num}\n\
msg_size: {self.msg_size}\n\
server_dalay: {self.server_dalay}"""


def parse_args():
    if len(sys.argv) != 7:
        print(
            "Usage: python client.py <HOST> <PORT> <MEASURE> <MSG_SIZE> <PROBES_NUM> <DELAY>"
        )
        sys.exit(1)

    args = {}
    args["ip"] = str(sys.argv[1])
    args["port"] = int(sys.argv[2])
    args["measure"] = str(sys.argv[3])
    args["bytes"] = int(sys.argv[4])
    args["probes"] = int(sys.argv[5])
    args["delay"] = int(sys.argv[6])

    return args


def main():
    # 0. Setup client
    inputs = parse_args()

    client = TCPClient(inputs)
    client.connect()

    # 1. Connection Phase
    conn_msg = client.construct_init_msg()
    conn_msg = conn_msg.encode()
    client.socket.sendall(conn_msg)
    response = client.socket.recv(2048).decode()
    
    time_ls = []
    total_data = 0

    # 2. Probing Phase
    if response == http_resp.CONN_RESP_404:
        print(f"CLIENT encountered error: {http_resp.CONN_RESP_404}")
        print("CLIENT: terminated")
        return
    elif response == http_resp.CONN_RESP_200:
        print("CLIENT:connected to the server")
        for i in range(1, inputs["probes"] + 1):
            client.generate_payload()
            msg = client.construct_prob_msg(i)
            
            print(f"CLIENT sending: {msg}")
            msg = msg.encode()
            start_ts = time.time_ns()
            client.socket.sendall(msg)
            resp = client.socket.recv(len(msg)+20)
            end_ts = time.time_ns()
            time_ls.append(end_ts-start_ts)
            total_data += len(resp)
            resp = resp.decode()
            if response == http_resp.CONN_RESP_200:
                continue
            elif resp == http_resp.PROB_RESP_404:
                print(f"CLIENT encountered error: {http_resp.PROB_RESP_404}")
                client.socket.close()
                print("CLIENT: terminated")
                return
            else:
                print("CLIENT: unknown resp during probe process:\n", resp)
                client.socket.close()
                print("CLIENT: terminated")
                return
        

    # 3. Termination Phase
    msg = client.construct_termination_msg()
    msg = msg.encode()
    client.socket.sendall(msg)
    resp = client.socket.recv(1024).decode()

    client.socket.close()

    total_time = (sum(time_ls)/ len(time_ls)) / 1e9
    RTT = total_time / client.probes_num
    tput = total_data / total_time
    
    print("-" * 30)
    print("***** SUMMARY *****")
    print(client.summary())
    if client.measure_type == "tput":
        print(f"RESULT:{client.msg_size},tput,{tput}")
    elif client.measure_type == "rtt":
        print(f"RESULT:{client.msg_size},rtt,{RTT}")
    print("-" * 30)
    print()

    if resp == http_resp.CLOSE_RESP_200:
        return
    elif resp == http_resp.CLOSE_RESP_404:
        print(http_resp.CLOSE_RESP_404)
        return
    print("CLIENT: terminated")


main()
