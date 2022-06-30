# This document is a draft and needs to be updated

1. Install [PostgreSQL 11](https://www.postgresql.org/download/)
    * Default inputs in the setup wizard should work fine
    * Enable postgis extensions
        * After installation, make sure the box is checked to launch the Stack Builder at exit
        * In the Stack Builder window, install the PostGIS bundle under Categories > Spatial Extensions
    * Create benmap_system user
        * Run:
          * psql –username=postgres
          * when prompted, enter the password used during installation
        * Once in, run:
          * CREATE ROLE benmap_system with SUPERUSER LOGIN PASSWORD ‘set a password here’
        * In the PostgreSQL program file folder, navigate to PostgreSQL/11/data
    * In pg_hba.conf, add:
        * "host	all	benmap_system	127.0.0.1/32	md5" to the bottom of the file
    * In postgresql.conf, make sure that listen_addresses = "*"
    * Run:
        * psql –username=benmap_system 
        * and enter the password set when creating the role. No errors should occur.
    * Enable the postgis extensions
        * CREATE EXTENSION postgis;
        * CREATE EXTENSION fuzzystrmatch;
        * CREATE EXTENSION postgis_tiger_geocoder;
        * CREATE EXTENSION postgis_topology;
2. Restore dump of benmap database. See DB_MIGRATION.md for example commands 
    * Download the database gz file and extract the contents
    * Run:
        * psql -d benmap –host “<hostname>” –port "5432" –username [benmap_system] –password -f "[path to extracted database contents]"

3. Install [Java Corretto 17](https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/downloads-list.html)
4. Install [gradle](https://gradle.org/install/)
5. Install [Visual Studio Code](https://code.visualstudio.com/)
6. Install the Extension Pack for Java in VS Code
7. Create local clone of [BenCloudServer repository](https://github.com/BenMAPCE/BenCloudServer)
8. Open BenCloudServer folder in VS Code
9. Create bencloud-local.properties with IP and password of your postgres server
    * In the BenCloudServer folder, create a file called bencloud-local.properties
    * Example contents:
  
        postgresql.host=127.0.0.1 

        postgresql.port=5432 

        postgresql.database=benmap 

        postgresql.user=benmap_system 

        postgresql.password=password

10. Debug BenCloudServer as application
    * Navigate to BenCloudServer\src\main\java\gov\epa\bencloud\server. Right click BenCloudServer.java and debug