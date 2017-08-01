#!/bin/bash
#
# Kills cassandra on all nodes mentioned in the dcl_config_file
#

set -u

if [ $# -ne 1 ]; then
    echo "Usage: "$0" [vicci_dcl_config_file]"
    exit
fi

dcl_config=$1

num_dcs=$(grep num_dcs $dcl_config | awk -F "=" '{ print $2 }')
cassandra_ips=($(grep cassandra_ips $dcl_config | awk -F "=" '{ print $2 }'))
cassandra_ips=($(echo "echo ${cassandra_ips[@]}" | bash))
jobs=($(grep job $dcl_config | awk -F "=" '{ print $2 }'))
jobs=($(echo "echo ${jobs[@]}" | bash))
ips=($(grep node_ips $dcl_config | awk -F "=" '{ print $2 }'))
ips=($(echo "echo ${ips[@]}" | bash))
node_names=($(grep node_name $dcl_config | awk -F "=" '{ print $2 }'))
node_names=($(echo "echo ${node_names[@]}" | bash))

#kill in parallel
set -m #need monitor mode to fg processes
for cli_index in $(seq 0 $((num_dcs - 1))); do
		client=$(echo ${cassandra_ips[$cli_index]} | sed 's/ /\n/g' | head -n $((cli_index+1)) | tail -n 1)
		job=$(echo ${jobs[$cli_index]} | sed 's/ /\n/g' | head -n $((cli_index+1)) | tail -n 1)
		ip=$(echo ${ips[$cli_index]} | sed 's/ /\n/g' | head -n $((cli_index+1)) | tail -n 1)
		node_name=$(echo ${node_names[$cli_index]} | sed 's/ /\n/g' | head -n $((cli_index+1)) | tail -n 1)
    		ssh vdagnely@access.grid5000.fr ssh $client OAR_JOB_ID=$job oarsh $node_name "home/vdagnely/home/vincent/Documents/eiger/eiger/kill_all_cassandra.bash" &
done

for cli_index in $(seq 0 $((num_dcs - 1))); do
    fg
done
