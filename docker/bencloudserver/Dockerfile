FROM amazoncorretto:17-alpine

WORKDIR /bencloud

COPY BenCloudServer.jar /bencloud/BenCloudServer.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
ADD www /bencloud/www
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -Xms3G -Xmx3G -XX:+UseG1GC -XX:MaxMetaspaceSize=1G -jar /bencloud/BenCloudServer.jar

EXPOSE 4567



