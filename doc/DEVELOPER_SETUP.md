# This documents needs to be updated

1. Install [PostgreSQL 11](https://www.postgresql.org/download/)
    * create benmap_system user
    * enable postgis extensions
2. Restore dump of benmap database  
     See DB_MIGRATION.md for example commands 
3. Install [Java Corretto 17](https://docs.aws.amazon.com/corretto/latest/corretto-8-ug/downloads-list.html)
4. Install [gradle](https://gradle.org/install/)
5. Install [Visual Studio Code](https://code.visualstudio.com/)
6. Install the Extension Pack for Java in VS Code
7. Create local clone of BenCloudServer repository
8. Open BenCloudServer folder in VS Code
9. Configure bencloud-local.properties with IP of your postgres server
10. Debug BenCloudServer as application