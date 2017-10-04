# micro-mongo

## 2. Use below command to run this service

	mvn spring-boot:run -Dserver.port=1212 &
	mvn spring-boot:run -Dserver.port=1313 &
	mvn spring-boot:run -Dserver.port=1414 &

## 3. The service can be accesses using below url

	http://ec2-35-154-212-219.ap-south-1.compute.amazonaws.com/mongo/rest/swagger-ui.html

Find the process running on window

	netstat -o -n -a | findstr 0.0:90


Reference 
1. http://mongodb.github.io/mongo-java-driver/2.13/getting-started/quick-tour/


http://ec2-35-160-105-209.us-west-2.compute.amazonaws.com:8080/

r

http://ode.ninja/how-to-install-a-jenkins-ec2-for-continuous-integration/

cat /var/lib/jenkins/secrets/initialAdminPassword

wget -O /etc/yum.repos.d/jenkins.repo http://pkg.jenkins-ci.org/redhat-stable/jenkins.repo



sudo bash
pkill -9 -f ghanadb
exec -a "ghanadb" java -jar /var/lib/jenkins/workspace/demo/target/ghanadb-0.0.1.jar &

kill $(ps -e | grep java | awk '{ print $1 }')
nohup java -Dname=ghanadb -jar /var/lib/jenkins/workspace/demo/target/ghanadb-0.0.1.jar

sudo sh /home/ec2-user/deploy.sh
sudo service jenkins start

pkill -9 -f demo

exec -a "demo" java -jar /var/lib/jenkins/workspace/demo/target/ghanadb-0.0.1.jar &