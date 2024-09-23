#FROM amazoncorretto:17-alpine
FROM amazoncorretto:17-al2023

WORKDIR /bencloud

COPY BenCloudTaskRunner.jar /bencloud/BenCloudTaskRunner.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -Xms16G -Xmx16G -XX:+UseG1GC -XX:MaxMetaspaceSize=1G -jar /bencloud/BenCloudTaskRunner.jar

EXPOSE 4567
