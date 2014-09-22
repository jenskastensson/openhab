#!/bin/sh
PID=$(cat /volume1/homes/proservx/.daemon.pid)
rm -f /volume1/homes/proservx/.daemon.pid
kill $PID
