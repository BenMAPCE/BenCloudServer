# This documents needs to be updated

# Deploying the API (BenCloudServer) and UI (BenCloudApp) to the IEc Sandbox Instance

## Build and Deploy the API

**Stop the API application**

`ssh ec2-user@18.118.212.202`

`cd bencloud`

`./stop-bencloud.sh`


**Build and upload to server**

`cd BenCloudServer`

`gradle fatJar`

`scp build/libs/BenCloudServer.jar ec2-user@18.118.212.202:~/bencloud`

**Start the API application**

`./start-bencloud.sh`

Log location: ~/bencloud/logs/bencloud.log

## Build and Deploy the UI

**Build the single page app**

`cd BenCloudApp`

`quasar build`

`zip -r spa.zip dist/spa`

**Upload the UI to the server**

`scp dist/spa.zip ec2-user@18.118.212.202:~/`

**Deploy the UI on the server**

`sudo unzip spa.zip -d /var/www/`

## Viewing the Application

<http://18.118.212.202/>

## Database Maintenance

**Remove all tasks and results**  
`truncate data.task_worker;`  
`truncate data.task_queue;`  
`truncate data.task_complete;`  
`truncate data.hif_result;`  
`truncate data.hif_result_function_config;`  
`truncate data.hif_result_dataset;`  
`truncate data.valuation_result_dataset;`  
`truncate data.valuation_result_function_config;`  
`truncate data.valuation_result;`  
`vacuum analyze;`  

**Remove a specific air quality layer**  
`delete from data.air_quality_cell where air_quality_layer_id = :id;`  
`delete from data.air_quality_layer where id = :id;`  

**Dump a backup of the database**  
An example of backing up the local database on my mac  

`cd /Applications/Postgres.app/Contents/Versions/11/bin;`

`./pg_dump --file "/Users/jimanderton/Downloads/bencloud-20210909-11.11.sql" --host "localhost" --port "5432" --username "benmap_system" --no-owner --no-privileges --exclude-schema=stage --password --verbose --format=c --blobs "benmap"`

**Restore a backup of the database**  
An example of restoring the database to the AWS RDS instance  

`./dropdb benmap --host "database-1.c9mglzx6cyy8.us-east-2.rds.amazonaws.com" --port "5432" --username "benmap_system" --password `

`./createdb -T template0 benmap --host "database-1.c9mglzx6cyy8.us-east-2.rds.amazonaws.com" --port "5432" --username "benmap_system" --password`

`./pg_restore -d benmap --host "database-1.c9mglzx6cyy8.us-east-2.rds.amazonaws.com" --port "5432" --username "benmap_system" --password --verbose "/Users/jimanderton/Downloads/bencloud-20210909-11.11.sql"`

