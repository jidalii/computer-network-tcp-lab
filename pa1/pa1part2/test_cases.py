import socket
import http_resp

# import argparse
import string
import random
import sys


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


def test_invalid_conn_measure_type():
    inputs = {
        "ip": "localhost",
        "port": 58005,
        "measure": "rTTT",
        "bytes": 1000,
        "probes": 10,
        "delay": 0,
    }

    client = TCPClient(inputs)
    client.connect()

    # 1. Connection Phase
    conn_msg = f"s rrrrtT {client.probes_num} {client.msg_size} {client.server_dalay}\n"
    conn_msg = conn_msg.encode()
    client.socket.sendall(conn_msg)
    response = client.socket.recv(2048).decode()
    return response


def test_invalid_probe_num():
    inputs = {
        "ip": "localhost",
        "port": 58005,
        "measure": "rTTT",
        "bytes": 1000,
        "probes": 1,
        "delay": 0,
    }

    client = TCPClient(inputs)
    client.connect()

    # 1. Connection Phase
    conn_msg = f"s rtt {client.probes_num} {client.msg_size} {client.server_dalay}\n"
    conn_msg = conn_msg.encode()
    client.socket.sendall(conn_msg)
    response = client.socket.recv(2048).decode()

    client.generate_payload()
    msg = client.construct_prob_msg(2)

    print(f"CLIENT sending: {msg}")
    msg = msg.encode()
    client.socket.sendall(msg)
    resp = client.socket.recv(client.msg_size + 10)
    resp = resp.decode()
    return resp


def test_invalid_termination():
    inputs = {
        "ip": "localhost",
        "port": 58005,
        "measure": "rTTT",
        "bytes": 1000,
        "probes": 1,
        "delay": 0,
    }

    client = TCPClient(inputs)
    client.connect()

    # 1. Connection Phase
    conn_msg = f"s rtt {client.probes_num} {client.msg_size} {client.server_dalay}\n"
    conn_msg = conn_msg.encode()
    client.socket.sendall(conn_msg)
    response = client.socket.recv(2048).decode()

    client.generate_payload()
    msg = client.construct_prob_msg(1)

    print(f"CLIENT sending: {msg}")
    msg = msg.encode()
    client.socket.sendall(msg)
    resp = client.socket.recv(client.msg_size + 10)
    resp = resp.decode()
    
    msg = "ttt\n"
    msg = msg.encode()
    client.socket.sendall(msg)
    resp = client.socket.recv(1024).decode()

    client.socket.close()
    
    return resp


def main():
    # print(test_invalid_conn_measure_type())
    # print(test_invalid_probe_num())
    # print(test_invalid_termination())
    return


main()
