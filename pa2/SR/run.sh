#!/bin/bash

# Define parameters
messages=1000
avg_delay=200
window_size=12
timeout=30
trace_level=3

# Define different random seeds and probability values
seeds=(1234 5678 91011)
loss_rates=(0.0 0.1 0.2 0.3 0.4 0.5)
corrupt_rates=(0.0 0.1 0.2 0.3 0.4 0.5)

# Create a results folder
mkdir -p results

# Loop through seeds, loss rates, and corruption rates
for seed in "${seeds[@]}"; do
    for loss in "${loss_rates[@]}"; do
        for corrupt in "${corrupt_rates[@]}"; do
            # Run the simulation and save the output
            output_file="./results/output_seed${seed}_loss${loss}_corrupt${corrupt}.txt"
            echo -e "${messages}\n${loss}\n${corrupt}\n${avg_delay}\n${window_size}\n${timeout}\n${trace_level}\n${seed}" | java Project > "$output_file"
        done
    done
done