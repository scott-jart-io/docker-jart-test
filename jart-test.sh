#!/bin/sh
set -u
mvn ea-async:instrument package
fifo=/tmp/$$.fifo
mkfifo $fifo
/usr/sbin/ip netns exec veth1-ns java -jar ./target/jart-test-0.0.1-SNAPSHOT-shaded.jar veth1 > $fifo &
echo "waiting for server to come up"
head -1 $fifo
cat $fifo & 
sleep 1; rm $fifo &
echo "try echo server: socat - tcp4:10.200.1.2:7"

