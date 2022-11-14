# Loading the BenMAP Database from Backup

This document includes details to load and configure the BenMAP database into a newly created PostgreSQL 11.x database server instance with PostGIS [installed and enabled](https://postgis.net/install/).

**Create and load the benmap database.**

```sql
createdb -T template0 --host "[HOST]" --username "postgres" --password benmap
psql -d benmap --host "[HOST]" --username "postgres" --password -f "[PATH TO SQL FILE INPUT]"
```

If you don't have access to a benmap database backup file, please contact the development team for a download link.

The version of the benmap database will be included in the name of the backup file. Please compare that to the patch files located in the db directory in this repo. If needed, run each patch that has a higher version number than the backup that was loaded.

**Create the benmap_system user and give it access to the data**

```bash
psql --host "[HOST]" --username=postgres --password -d benmap
```

```sql
CREATE ROLE benmap_system with LOGIN PASSWORD 'set a password here';
GRANT CONNECT ON DATABASE benmap TO benmap_system;
GRANT USAGE ON SCHEMA data, grids, tiger, tiger_data, topology TO benmap_system;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA data, grids TO benmap_system;
GRANT USAGE, SELECT, UPDATE ON ALL SEQUENCES IN SCHEMA data, grids TO benmap_system;
GRANT EXECUTE ON ALL FUNCTIONS IN SCHEMA data, grids TO benmap_system;
```

**Finally, set up database configuration**

If you are running a local instance of the API, ensure that the bencloud-local.properties file is populated with the correct information

```
postgresql.host=127.0.0.1
postgresql.port=5432
postgresql.database=benmap
postgresql.user=benmap_system
postgresql.password=[the_password]
default.max.tasks.per.user = 0
```