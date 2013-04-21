#!/bin/bash
ipAddress=$(ifconfig eth0 | awk '/Direc. inet/ {split ($2,A,":"); print A[2]}')
len=${#ipAddress}
if [ "$len" = 0 ] ; then
	ipAddress=$(ifconfig wlan0 | awk '/Direc. inet/ {split ($2,A,":"); print A[2]}')
fi
yamagiq2 +connect "[$ipAddress""]:27910" +set spectator 1 +set in_grab 0
