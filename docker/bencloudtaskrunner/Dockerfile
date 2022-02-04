FROM amazoncorretto:17-alpine

WORKDIR /bencloud

COPY BenCloudTaskRunner.jar /bencloud/BenCloudTaskRunner.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
ADD www /bencloud/www
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -Xmx6G -jar /bencloud/BenCloudTaskRunner.jar

EXPOSE 4567



