#!/bin/sh
PID=$(cat ~/.daemon.pid)
rm -f ~/.daemon.pid
kill $PID
