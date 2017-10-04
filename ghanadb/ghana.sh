#!/bin/sh
mvn -f /home/ec2-user/ghanadb/pom.xml clean install 
killall java
sudo nohup java -jar /home/ec2-user/ghanadb/target/ghanadb-0.0.1.jar &