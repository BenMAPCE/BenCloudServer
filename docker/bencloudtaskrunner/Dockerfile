FROM amazoncorretto:17-al2023

WORKDIR /bencloud

COPY BenCloudServer.jar /bencloud/BenCloudServer.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -XshowSettings:vm -XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -XX:MaxMetaspaceSize=1G -cp /bencloud/BenCloudServer.jar gov.epa.bencloud.server.BenCloudTaskRunner

EXPOSE 4567
