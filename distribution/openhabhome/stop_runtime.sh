#!/bin/sh
PID=$(cat /var/services/homes/proservx/.daemon.pid)
rm -f /var/services/homes/proservx/.daemon.pid
kill $PID
