#!/bin/bash
ipAddress=$(ifconfig eth0 | awk '/Direc. inet/ {split ($2,A,":"); print A[2]}')
len=${#ipAddress}
if [ "$len" = 0 ] ; then
	ipAddress=$(ifconfig wlan0 | awk '/Direc. inet/ {split ($2,A,":"); print A[2]}')
fi

yamagiq2 +set dedicated 1 +set deathmatch 1 +set maxclients 32 +map q2dm1 +set in_grab 0  +set ip "$ipAddress"
