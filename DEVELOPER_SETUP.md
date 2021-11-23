1. Install [PostgreSQL 11](https://www.postgresql.org/download/)
    * create benmap_system user
    * enable postgis extensions
    
1. Restore dump of benmap database  
 > An example of restoring the local database on my mac  
    `cd /Applications/Postgres.app/Contents/Versions/11/bin;`  
    `./createdb -T template0 benmap --host "database-1.c9mglzx6cyy8.us-east-2.rds.amazonaws.com" --port "5432" --username "benmap_system" --password`  
    `./pg_restore -d benmap --host "localhost" --port "5432" --username "benmap_system" --password --verbose "/Users/jimanderton/Downloads/bencloud-20210909-11.11.sql"`  
3. Install [Java Corretto 8](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
1. Install [gradle](https://gradle.org/install/)
1. Install [Eclipse](https://www.eclipse.org/downloads/)
1. Create local clone of BenCloudServer repository
1. Import BenCloudServer into Eclipse as gradle project
1. Configure bencloud-local.properties with IP of your postgres server
1. Debug BenCloudServer as application