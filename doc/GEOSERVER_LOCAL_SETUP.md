# GeoServer Local Setup

GeoServer is used to publish grid definition layers (imported shapefiles) as WMS/WFS services. In production it runs in Kubernetes; locally it runs via Docker Compose using the [BenCloudGeoServer](https://github.com/BenMAPCE/BenCloudGeoServer) repository.

GeoServer is only exercised when importing or deleting a custom grid definition. If you are not working on that feature, you can skip this setup — missing GeoServer config will only cause those specific operations to fail.

## Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) installed and running
- Local clone of the [BenCloudGeoServer repository](https://github.com/BenMAPCE/BenCloudGeoServer)
- Local benmap PostgreSQL database already set up (see DEVELOPER_SETUP.md)

## Steps

1. **Create a local data directory** for GeoServer to persist its configuration:
    * Create the folder `C:\Users\<your-username>\benmapfiles\geoserver`
    * GeoServer will write workspace/datastore config here; it survives container restarts

2. **Update `docker-compose.yml`** in `BenCloudGeoServer\docker\bencloudgeoserver\` — two changes are needed before first start:

    * **Volume path** — replace the existing path with your local data directory
        ```yaml
        volumes:
          - C:\Users\<your-username>\benmapfiles\geoserver:/opt/geoserver/data_dir
        ```
    * **Port** — the Quasar dev server uses 8080 by default, so map GeoServer to 8083 instead:
        ```yaml
        ports:
          - "8083:8080"
        ```

3. **Build and start GeoServer**:
    * Open a terminal in `BenCloudGeoServer\docker\bencloudgeoserver\`
    * Run:
        ```
        docker-compose up -d
        ```
    * GeoServer will be available at http://localhost:8083/geoserver

4. **Log in to the GeoServer admin UI**:
    * URL: http://localhost:8083/geoserver/web
    * Username: `benmapadmin`
    * Password: `benmap`

5. **Create a Workspace**:
    * In the left panel, click Data > Workspaces > Add new workspace
    * Name: `bencloud`
    * Namespace URI: `http://bencloud`
    * Click Save

6. **Create a PostGIS Datastore**:
    * In the left panel, click Data > Stores > Add new store
    * Select PostGIS from the list
    * Set the workspace to `bencloud`
    * Data Source Name: `benmap` (or any name — note it, you'll need it below)
    * Fill in the connection parameters to match your local database:
        * host: `host.docker.internal` *(use this, not 127.0.0.1 — Docker needs to reach the host machine's Postgres)*
        * port: `5432`
        * database: `benmap`
        * schema: `grids`
        * user: `benmap_system`
        * passwd: `<your benmap_system password>`
    * Click Save

7. **Add GeoServer properties to `bencloud-local.properties`** in the BenCloudServer folder:
    ```properties
    geoserver.url=http://localhost:8083/geoserver/rest
    geoserver.workspace=bencloud
    geoserver.store=benmap
    geoserver.username=benmapadmin
    geoserver.password=benmap
    ```
    * Set `geoserver.store` to whatever Data Source Name you chose in step 6

## BenCloudApp (Frontend) Configuration

When running the Quasar dev server locally, the browser blocks WFS requests from `localhost:8080` (Quasar) to `localhost:8083` (GeoServer) due to the browser's same-origin policy. On the server this is not an issue because both are behind the same reverse proxy.

The fix is already in place in `bencloud-quasar/quasar.conf.js` and `bencloud-quasar/.quasar.env.json`: the dev server proxies `/geoserver` requests to GeoServer, and the `development` environment uses a relative base URL so requests go through that proxy.

**`quasar.conf.js` — dev server proxy:**
```js
devServer: {
  proxy: {
    '/geoserver': {
      target: 'http://localhost:8083',
      changeOrigin: true
    }
  }
}
```

**`.quasar.env.json` — relative `GEOSERVER_BASE_URL` for `development`:**
```json
"GEOSERVER_BASE_URL": "/geoserver"
```

No additional frontend changes are needed. Restart the Quasar dev server after any changes to `quasar.conf.js`.

## Publishing Layers for Existing Grid Definitions

When restoring a database dump, grid definitions will already exist in the `grid_definition` table but will have no corresponding GeoServer layer (layers are normally published during grid import). The review-grids map will not show those grids until their layers are published.

To publish an existing grid manually:

1. In the GeoServer admin UI, go to Data > Layers > Add a new resource
2. Select the `bencloud:benmap` datastore
3. Find the `g_<uuid>` table for the grid and click **Publish**
4. Under "Coordinate Reference Systems", set Native SRS to `EPSG:4269`
5. Under "Bounding Boxes", click **Compute from data** then **Compute from native bounds**
6. Click Save

Repeat for each grid definition that needs a layer. The table name for each grid is stored in the `table_name` column of the `grid_definition` table (format: `grids.g_<uuid>`).

## Stopping / Restarting

```
# Stop (keeps data)
docker-compose down

# Start again
docker-compose up -d
```

GeoServer configuration (workspaces, datastores, published layers) persists in your local data directory, so steps 5 and 6 only need to be done once.

## Troubleshooting

- **Container fails to start**: make sure Docker Desktop is running and port 8083 is not already in use
- **Datastore connection fails**: use `host.docker.internal` as the host (not `localhost` or `127.0.0.1`) so the container can reach PostgreSQL on the host machine
- **Layer publish fails during grid import**: check that the `grids` schema exists in your benmap database and that `benmap_system` has access to it
- **`geoserver.url` not picked up**: confirm `bencloud-local.properties` exists in the BenCloudServer root folder and contains all four `geoserver.*` properties
