# BenCloudServer Database Configuration

The application is configured so you can run locally from inside Eclipse, or create a local docker instance.

To run locally inside Eclipse, you need to create a file **bencloud-local.properties** with the following contents:

    postgresql.host=192.168.0.152  
    postgresql.port=5432. 
    postgresql.database=docker  
    postgresql.user=docker  
    postgresql.password=docker    

***Replace the variable values above with ones applicable to you.*** This file does not *(and should not)* get checked int Git.

When running inside docker (local or deployed) the database properties are set in the **Dockerfile** as below:  

    ENV postgresql_host 192.168.0.152  
    ENV postgresql_port 5432  
    ENV postgresql_database docker  
    ENV postgresql_user docker  
    ENV postgresql_password docker 

The application checks for the **bencloud-local.properties** first, then falls back to checking environment variables.

If you get issues when connecting to your local database. You may need to add an entry to your **pg_hba.conf**. 

Most likely it will be like below (as docker for mac uses the IP address listed), but you may need to set a different one. When you get the error, it will tell you what IP address doesn't have permission.

    host    all             all             192.168.0.152/0          trust

Once you do this, you will need to restart postgresql.
