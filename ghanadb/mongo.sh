#!/bin/sh
#/etc/init.d/mongod
sudo mongo --eval "db.getSiblingDB('admin').shutdownServer()"
sudo service mongod stop
sudo nohup mongod