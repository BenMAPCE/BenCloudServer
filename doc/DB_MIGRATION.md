# Database Migration

The BenMAP cloud database is derived from the data that ships with the BenMAP-CE desktop tool. This is accomplished by running a repeatable, automated process implemented using the open-source [Pentaho Data Integration](https://sourceforge.net/projects/pentaho/files/Pentaho-9.3/client-tools/pdi-ce-9.3.0.0-428.zip/download) tool. 

To configure the Postgres and Firebird database connections in Pentaho Data Integration on Windows 10, the following steps are required:
1.	Replace the included lib/jaybird-2.1.6.jar file with jaybird-full-4.0.3.java8.jar
2.	Copy the following from the BenMAP-CE\BenMAP\FireBirdHelper\bin\x64\Release in the desktop repo to the pentaho-data-integration\lib folder
    *	Intl/ folder
    *	Fbembed.dll
    *	Icudt30.dll
    *	Icuin30.dll
    *	Icuuc30.dll
3.	Download and add to the lib folder (not sure if this is required?)
    *	Jna-platform-5.8.0.jar
    *	Jna-5.8.0.jar
4.	Configure the Pentaho and Firebird JNDI connections in jdbc.properties

`benmap_firebird/type=javax.sql.DataSource`
`benmap_firebird/driver=org.firebirdsql.jdbc.FBDriver`
`benmap_firebird/url=jdbc:firebirdsql:embedded:C:/repos/BenMAP-Cloud-Data-Migration/BENMAP50.FDB?encoding=WIN1251`
`benmap_firebird/user=sysdba`
`benmap_firebird/password=masterkey`

`benmap_postgres/type=javax.sql.DataSource`
`benmap_postgres/driver=org.postgresql.Driver`
`benmap_postgres/url=jdbc:postgresql://[HOST_NAME]:5432/benmap`
`benmap_postgres/user=benmap_system`
`benmap_postgres/password=[PASSWORD]`

Once the drivers and connections are configured, open the Pentaho Data Integration tool by running spoon.bat. Then, open run_all.kjb from the db/migration folder in the BenCloudServer repository. 

Note that in order to run the full process, you first need to have the population data staged by running the stage_population ktrs separately.

## Dumping the benmap database to a file

`./pg_dump --file "[PATH TO SQL FILE OUTPUT]" --host "[HOST]" --port "5432" --username "benmap_system" --no-owner --no-privileges --exclude-schema=stage --password --verbose --format=plain --blobs "benmap"`

## Creating and loading from a file
`./createdb -T template0 --host "[HOST]" --port "5432" --username "benmap_system" --password benmap`

`./psql -d benmap --host "[HOST]" --port "5432" --username "benmap_system" --password -f "[PATH TO SQL FILE INPUT]"`