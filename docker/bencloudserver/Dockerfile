FROM amazoncorretto:17-alpine

#ENV postgresql_host the_host
#ENV postgresql_port 5432
#ENV postgresql_database benmap
#ENV postgresql_user benmap_system
#ENV postgresql_password the_password

WORKDIR /bencloud

COPY BenCloudServer.jar /bencloud/BenCloudServer.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
ADD www /bencloud/www
RUN mkdir /bencloud/logs
RUN chmod 777 /bencloud/logs

ENTRYPOINT java -Xmx6G -jar /bencloud/BenCloudServer.jar

EXPOSE 4567



