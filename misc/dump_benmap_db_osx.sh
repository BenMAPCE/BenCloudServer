cd /Applications/Postgres.app/Contents/Versions/11/bin;
./pg_dump --file "/Users/jimanderton/Downloads/bencloud-20210909-11.11.sql" --host "localhost" --port "5432" --username "benmap_system" --no-owner --no-privileges --exclude-schema=stage --password --verbose --format=c --blobs "benmap"
