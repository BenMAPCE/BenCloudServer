# BenCloudServer

The BenMAP web tool is comprised of two components: the API and the User Interface (UI).

The API is implemented as a Java-based REST service and is hosted in the BenCloudServer repository.

The UI is implemented as a Quasar-based single page application and is hosted in the BenCloudApp repository.

Both components are implemented in EPA's Kubernetes-based DevSecOps environment. In that context, they run as docker containers deployed via Gitlab CI/CD. Details can be found in the docker and k8s folders as well as the .gitlab-ci.yml file.

Outside of this environment, each tool can be run directly and can be hosted on most platforms for local development, testing, or analysis work.

    See the doc directory for important information regarding database configuration, security, developer setup, etc.

## Run the BenCloud Server inside Eclipse

right click BenCloudServer.java and select "Run as ... Java Application"

## Build the BenCloud Server

To build the .jar file for testing run:

**gradle fatJar**

(assumes you are running gradle 6.x)

(this needs to be done before building a Docker image)


## Build docker image for local testing

From the top level source directory

**docker image build . -t bc** (where bc is the image name)

You can then run

**docker images** (and see something like below)

| REPOSITORY | TAG | IMAGE ID | CREATED | SIZE |
| --- | --- | --- | --- | --- |
| bc | latest | ba723880f347 | 3 minutes ago | 317MB |

If you want to run the image issue a command like

**docker run -dit --publish 8080:4567 bc**

the **bc** is the name you gave the image
the **8080:4567** translated the SparkJava default port of 4567 to be exposed on 8080

When you run it, you will see a long string like:

cd6c0dc21defafc9894af4e72e80cbc40237c07f94ab40258082d401e0618f0a

which is the container instance id

So in your browser you should be able to go to **http://localhost:8080** and see the server running

If you then issue the command

**docker ps -a**

You will see a list of the containers, as below

| CONTAINER ID | IMAGE | COMMAND | CREATED | STATUS | PORTS | NAMES |
| --- | --- | --- | --- | --- | --- | --- |
| cd6c0dc21def | bc | "/bin/sh -c 'java -j…" | 50 seconds ago | Up 48 seconds | 0.0.0.0:8080->4567/tcp | cranky_shirley |

If you want to run a command, in your image, you can do so. But if you run a command that doesn't finish, and you control-c out, it will stop your container.

Command examples you can run, that self finish are:

**docker exec cd6 ls**

where the **cd6** if the first 3 of the container id, in the table above.

You can run top and let it run and exit on it's own like:

**docker exec cd6 top -n 5**

where the **-n 5** will auto exit after 5 iterations

To stop the container run

**docker stop cd6**

If you then run

**docker ps -a**

you will see it is stopped

| CONTAINER ID | IMAGE | COMMAND | CREATED | STATUS | PORTS | NAMES |
| --- | --- | --- | --- | --- | --- | --- |
| cd6c0dc21def | bc | "/bin/sh -c 'java -j…" | 11 minutes ago | Exited (143) 42 seconds ago | 0.0.0.0:8080->4567/tcp | cranky_shirley |

***(more to come...)***

