#!/bin/bash
ipAddress=$(ifconfig eth0 | awk '/Direc. inet/ {split ($2,A,":"); print A[2]}')
yamagiq2 +connect "$ipAddress" +set spectator 1 +set in_grab 0
