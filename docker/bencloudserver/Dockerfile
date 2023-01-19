FROM amazoncorretto:17-alpine

WORKDIR /bencloud

COPY BenCloudServer.jar /bencloud/BenCloudServer.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -Xms15G -Xmx15G -XX:+UseG1GC -XX:MaxMetaspaceSize=1G -jar /bencloud/BenCloudServer.jar >/bencloud/logs/console.log  2>&1

EXPOSE 4567



