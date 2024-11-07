import sys

f1 = "output.txt"
f2 = "PA2_part2_trace (1).txt"


file1 = open(f1, "r")
file2 = open(f2, "r")

i = 0
while i < 60000:

    l1 = file1.readline()
    while "EVENT time: " not in l1:
        l1 = file1.readline()

    l2 = file2.readline()
    while "EVENT time: " not in l2:
        l2 = file2.readline()

    if l1 != l2:
        print(l1)
        sys.exit()

    i += 1

# Close the file
file1.close()
file2.close()
