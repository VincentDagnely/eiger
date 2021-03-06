#!/bin/bash
#
# Launches multiple cassandra instances on VICCI
#

set -u

if [ $# -ne 1 ]; then
    echo "Usage: "$0" [vicci_dcl_config_file]"
    exit
fi

dcl_config=$1
wait=wait

num_dcs=$(grep num_dcs $dcl_config | awk -F "=" '{ print $2 }')
ips=($(grep cassandra_ips $dcl_config | awk -F "=" '{ print $2 }'))
ips=($(echo "echo ${ips[@]}" | bash))
sites=($(grep sites $dcl_config | awk -F "=" '{ print $2 }'))
sites=($(echo "echo ${sites[@]}" | bash))
#Seed from all the nodes
seeds=$(echo ${ips[@]} | sed 's/ /, /g')
echo "seeds=$seeds"

nodes_per_dc=$((${#ips[@]} / num_dcs))
total_nodes=$((num_dcs * nodes_per_dc))
#sanity check
if [ $(($nodes_per_dc * $num_dcs)) -ne ${#ips[@]} ]; then 
    echo ${nodes_per_dc}" * "${num_dcs}" != "${#ips[@]};
    exit
fi

echo 0
#clean up all nodes in parallel
for client in ${ips[@]}; do
	echo $client
    ssh vdagnely@access.grid5000.fr ssh $client "/home/vdagnely/home/vincent/Documents/eiger/eiger/kill_all_cassandra.bash"
	echo "z"
    ssh vdagnely@access.grid5000.fr ssh $client "rm /home/vdagnely/home/vincent/Documents/eiger/eiger/*hprof 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "rm /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra-vanilla/*hprof 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "rm /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/cassandra*log 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "rm /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/cassandra*log* 2> /dev/null"
	echo "a"
    ssh vdagnely@access.grid5000.fr ssh $client "rm -rf /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/data/* 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "rm -rf /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/commitlog/* 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "rm -rf /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/saved_caches/* 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "rm -rf /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/stdout/* 2> /dev/null"
	echo "b"
    ssh vdagnely@access.grid5000.fr ssh $client "mkdir /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "mkdir /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/data 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "mkdir /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/commitlog 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "mkdir /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/saved_caches 2> /dev/null"
    ssh vdagnely@access.grid5000.fr ssh $client "mkdir /home/vdagnely/home/vincent/Documents/eiger/eiger/cassandra_var/stdout 2> /dev/null"
done
echo 1
#wait
echo 1.5
unset ip
echo 1.7
set +m

echo 2
#create the topo file, we must eventually write this to conf/cassandra-topology.properties
#because that location is hardcoded into cassandra
topo_file=conf/vicci/cassandra-topology.properties
echo -n "" > $topo_file
for dc in $(seq 0 $((num_dcs - 1))); do
    for n in $(seq 0 $((nodes_per_dc - 1))); do
        global_node_num=$((dc * nodes_per_dc + n))
	local_ip=$(echo ${ips[global_node_num]})
        #update the topology describing file
        #we only care about splitting things up by datacenter for now
        echo $local_ip=DC$dc:RAC1 >> $topo_file
    done
done
#echo "default=DC0:RAC1" >> $topo_file
unset dc
unset n
unset global_node_num
unset local_ip
echo 4
for dc in $(seq 0 $((num_dcs - 1))); do
    for n in $(seq 0 $((nodes_per_dc - 1))); do
	echo dc+":"+n
        global_node_num=$((dc * nodes_per_dc + n))
	local_ip=$(echo ${ips[global_node_num]})
	local_site=$(echo ${sites[global_node_num]})
        # tokens can't be identical even though we want them to be ... so for now let's get them as close as possible
        token=$(echo "${n}*(2^127)/${nodes_per_dc} + $dc" | bc)
        # Using tokens for evenly splitting type 4 uuids now
        #token=$(./uuid_token.py ${dc} ${n} ${nodes_per_dc})
        echo $token" @ "$local_ip

        conf_file=${num_dcs}x${nodes_per_dc}_${dc}_${n}.yaml
        log4j_file=log4j-server_${global_node_num}.properties

        #create the custom config file for this node
        sed 's/INITIAL_TOKEN/'$token'/g' conf/cassandra_VICCI_BASE.yaml \
	    | sed 's/LISTEN_ADDRESS/'$local_ip'/g' \
            | sed 's/RPC_ADDRESS/'$local_ip'/g' \
            | sed 's/SEEDS/'"$seeds"'/g' \
            > conf/vicci/$conf_file
        
	#| sed 's/NODE_NUM/'$global_node_num'/g' \

        sed 's/LOG_FILE/\/home\/vdagnely\/home\/vincent\/Documents\/eiger\/eiger\/cassandra_var\/cassandra_system.'$global_node_num'.log/g' conf/log4j-server_BASE.properties > conf/vicci/$log4j_file
	echo 7
        #set -x
	#copy over conf files
	
	scp -o StrictHostKeyChecking=no ${topo_file} vdagnely@access.grid5000.fr:/home/vdagnely/$local_site/home/vincent/Documents/eiger/eiger/conf/ 2>&1 | awk '{ print "'$local_ip': "$0 }'
	scp -o StrictHostKeyChecking=no conf/vicci/${conf_file} vdagnely@access.grid5000.fr:/home/vdagnely/$local_site/home/vincent/Documents/eiger/eiger/conf/ 2>&1 | awk '{ print "'$local_ip': "$0 }'
	scp -o StrictHostKeyChecking=no conf/vicci/${log4j_file} vdagnely@access.grid5000.fr:/home/vdagnely/$local_site/home/vincent/Documents/eiger/eiger/conf/ 2>&1 | awk '{ print "'$local_ip': "$0 }'

        #put this in ssh commands to modify JVM options
        #export JVM_OPTS="-Xms32M -Xmn64M"
	echo 8
	echo "/home/vdagnely/home/vincent/Documents/eiger/eiger/bin/cassandra -Dcassandra.config=${conf_file} -Dlog4j.configuration=${log4j_file}"
	ssh vdagnely@access.grid5000.fr ssh $local_ip "/home/vdagnely/home/vincent/Documents/eiger/eiger/bin/cassandra -Dcassandra.config=${conf_file} -Dlog4j.configuration=${log4j_file}" > /home/vincent/Documents/eiger/eiger/cassandra_var/stdout/${dc}_${n}.out 2>&1

	done
        #set +x
done
echo 10

timeout=60
if [ "$wait" != "" ]; then
    #wait until all nodes have joined the ring
    normal_nodes=0
    echo "Nodes up and normal: "
    wait_time=0
    while [ "${normal_nodes}" -ne "${total_nodes}" ]; do
	echo 12
        sleep 5
	echo 13
        normal_nodes=$(ssh vdagnely@access.grid5000.fr ssh ${ips[0]} '/home/vdagnely/home/vincent/Documents/eiger/eiger/bin/nodetool -h localhost ring 2>&1 | grep "Normal" | wc -l')
        echo "  "$normal_nodes
	wait_time=$((wait_time+5))
	if [[ $wait_time -ge 60 ]]; then
	    echo "timeout waiting for nodes to come up ... you'll need to try again"
	    exit 1
	fi
    done
	echo 14
    sleep 5
	echo 15
fi
