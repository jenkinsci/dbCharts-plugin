mvn package
#mkdir -p target/work/webapp/WEB-INF/lib/
cp target/dbCharts/WEB-INF/lib/* target/work/webapp/WEB-INF/lib/
MAVEN_OPTS="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=n -XX:MaxPermSize=256m" mvn hpi:run
cp target/dbCharts/WEB-INF/lib/* target/work/webapp/WEB-INF/lib/

