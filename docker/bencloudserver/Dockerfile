FROM amazoncorretto:17-alpine

WORKDIR /bencloud

COPY BenCloudServer.jar /bencloud/BenCloudServer.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -Xms14G -Xmx14G -XX:+UseG1GC -XX:MaxMetaspaceSize=1G -jar /bencloud/BenCloudServer.jar

EXPOSE 4567



