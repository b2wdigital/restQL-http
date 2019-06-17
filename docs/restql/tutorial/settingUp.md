# Setting Up

## Connecting to a REST API

To add a resource to RestQL we simply need to edit `restql.yml`, the file you created a few momments ago. Let's add Elon Musk's space business' API:

```yaml
mappings:
  launches: "https://api.spacexdata.com/v3/launches"
```

Any value assigned to that a key in `mappings` should point to a valid API.

## Building Docker image

restQL-http can also be ran as a Docker container.
First, create the `restql.yml` file and pass it's path via an `environment variable`.

```shell
-e JAVA_OPTS="-Drestql-config-file=restql.yml"
```
Then execute the build-dist script from the source code root folder:

```shell
./scripts/build-dist.sh
```

Finally, from the source code root folder, build a Docker image with the command:

```shell
docker build -t restql-server-img .
```

## Running the container

Then run the image as a container with the command:

```shell
docker run -p 9000:9000 restql-server-img
```

Nice! Now `restQL-http` is being served at `localhost:9000`!