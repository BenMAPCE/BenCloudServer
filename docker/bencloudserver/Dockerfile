FROM amazoncorretto:8-alpine

ENV postgresql_host bmapdev.cyf9mtuhnz9r.us-east-1.rds.amazonaws.com
ENV postgresql_port 5432
ENV postgresql_database benmap
ENV postgresql_user benmap_system
ENV postgresql_password unknown_password

WORKDIR /bencloud

COPY BenCloudServer.jar /bencloud/BenCloudServer.jar
COPY bencloud-server.properties /bencloud/bencloud-server.properties
ADD config /bencloud/config
ADD www /bencloud/www

ENTRYPOINT java -jar /bencloud/BenCloudServer.jar





