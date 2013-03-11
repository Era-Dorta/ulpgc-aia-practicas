#!/bin/bash
ipAddress=$(ifconfig eth0 | awk '/Direc. inet/ {split ($2,A,":"); print A[2]}')
yamagiq2 +connect "[$ipAddress""]:27910"
