pkill -9 -f ghanadb1212
pkill -9 -f ghanadb1313
pkill -9 -f ghanadb1414
mvn clean install
exec -a "ghanadb1212" java -jar -Dserver.port=1212 ~/ghanadb/target/ghanadb-0.0.1.jar &
exec -a "ghanadb1313" java -jar -Dserver.port=1313 ~/ghanadb/target/ghanadb-0.0.1.jar &
exec -a "ghanadb1414" java -jar -Dserver.port=1414 ~/ghanadb/target/ghanadb-0.0.1.jar &