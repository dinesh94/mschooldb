mongodump -u kandapohe -p m0ng0_k@nd@p0he --db kandapohe  --port 26101 --out /home/ec2-user/mongobackup/kandapohe

mongorestore -u kandapohe -p m0ng0_k@nd@p0he --db kandapohe  --port 26101 --out /home/ec2-user/mongobackup/kandapohe

nginx locations
	/usr/share/nginx/html/img/static
	/etc/nginx
	/usr/share/nginx/html
	
nginx config - /etc/nginx

include       sites-available.conf;

sites-available.conf

upstream kandapohe_service {
  server 127.0.0.1:1111;
  server 127.0.0.1:1212;
  server 127.0.0.1:1313;
  server 127.0.0.1:1414;
  server 127.0.0.1:1515;
}

# included in nginx.conf
#server {
#        listen       80 default_server;
#        listen       [::]:80 default_server;
#        server_name  localhost;
#        root         /usr/share/nginx/html;
#	#/mongo is the base context path for all rest call.
#	location /mongo {
#		proxy_set_header X-Real-IP $remote_addr;
#		proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
#		proxy_set_header Host $http_host;
#		proxy_set_header X-NginX-Proxy true;
#		proxy_pass http://kandapohe_service/mongo;
#     proxy_redirect off;
#	}
#	
#	...
#}
 
