# Configuration

## Environment variables:
- `PORT` allows you to specify which port the servers will listen (default is `9000`)
- `MONGO_URL` allows you to specify the connection with restQL-manager database (default is `nil`)
- `EXECUTOR_UTILIZATION` allows you to specify the server executor thread pool target utilization (default is `0.9`) - Learn more at [Manifold's wiki](https://github.com/ztellman/manifold/blob/449d1c63e13d5735e704eba02ed949f862d02596/src/manifold/executor.clj#L165)
- `EXECUTOR_MAX_THREADS` allows you to specify the server executor max threads (default is `512`) - Learn more at [Manifold's wiki](https://github.com/ztellman/manifold/blob/449d1c63e13d5735e704eba02ed949f862d02596/src/manifold/executor.clj#L165)
- `EXECUTOR_CONTROL_PERIOD` allows you to specify the interval, in milliseconds, between use of the controller to adjust the size of the pool (default ius `1000`) - Learn more at [Manifold's wiki](https://github.com/ztellman/manifold/blob/449d1c63e13d5735e704eba02ed949f862d02596/src/manifold/executor.clj#L91)
- `POOL_CONNECTIONS_PER_HOST` allows you to specify the maximum number of simultaneous connections to any host (default is `100`) - Learn more at [Aleph's wiki](https://github.com/ztellman/aleph/blob/5dd8083aa9858ef23ba32dfb05b4db47ec79b22c/src/aleph/http.clj#L96)
- `POOL_TOTAL_CONNECTIONS` allows you to specify the maximum number of connections across all hosts (default is `10000`) - Learn more at [Aleph's wiki](https://github.com/ztellman/aleph/blob/5dd8083aa9858ef23ba32dfb05b4db47ec79b22c/src/aleph/http.clj#L97)
- `POOL_MAX_QUEUE_SIZE` the maximum number of pending acquires from the pool that are allowed before `acquire` will start to throw a Exception (default is `65536`) - Learn more at [Aleph's wiki](https://github.com/ztellman/aleph/blob/5dd8083aa9858ef23ba32dfb05b4db47ec79b22c/src/aleph/http.clj#L100)
- `QUERY_GLOBAL_TIMEOUT` allows you to specify the default timeout, in milliseconds, for each query (default is `30000`)
- `QUERY_RESOURCE_TIMEOUT` allows you to specify the default timeout, in milliseconds, for each resource request (default is `5000`) 
- `CACHE_TTL` allows you to specify the *time to leave*, in milliseconds, to be used to cache the parse of Saved Queries (default is `60000`)
- `CACHE-COUNT` allows you to specify the number of items stored in cache (default is `2000`)
- `MAPPINGS_CACHE_TTL` allows you to specify the *time to leave*, in milliseconds, to be used to cache Resources Mappings URLs (default is `60000`)
- `CORS_ALLOW_ORIGIN` allows you to set the `Access-Control-Allow-Origin` CORS configuration (default is `"*"`)
- `CORS_ALLOW_METHODS` allows you to set the `Access-Control-Allow-Methods` CORS configuration (default is `"GET, POST, PUT, PATH, DELETE, OPTIONS"`)
- `CORS_ALLOW_HEADERS` allows you to set the `Access-Control-Allow-Headers` CORS configuration (default is `"DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"`)
- `CORS_EXPOSE_HEADERS` allows you to set the `Access-Control-Expose-Headers` CORS configuration (default is `"Content-Length,Content-Range"`)
- `TENANT` allows you to choose the tenant which contains your resources
- `ALLOW-ADHOC-QUERIES` if set to `false`, blocks the execution of adhoc queries (via POST HTTP mehtod), useful for performance (default is `true`)
- `RESTQL-CONFIG-FILE` allows you to change the name of the YAML configuration file. Note that it still needs to be located at the `src/resources/` folder (default is `restql.yml`)

**Note:** restQL caches only the mappings and parsed query, not the result of its execution.

## Configuration file:

Allows you to configure the resource mappings and saved queries (may be overwritten by [**restQL-manager**](https://github.com/B2W-BIT/restQL-manager) configurations).

### Location:
By default the configuration file is placed in `src/resources` path. 

## Adding Resources:

Resources can be added either through the `restql.yml` file, through connecting to a mongoDB collection or through an environment variable.

### Files: 

`restql.yml`
``` yml
mappings:
  your-resource: "http://your-resource-url.com"

queries:
  your-namespace:
    your-query:
      - |
        from your-resource
      - |
        use max-age=900
        from your-resource
  your-namespace-2:
    your-query:
      - |
        from your-resource
      - |
        use max-age=900
        from your-resource

cors:
  allow_origin: "*"
  allow_methods: "GET, POST, PUT, PATH, DELETE, OPTIONS"
  allow_headers: "DNT,User-Agent,X-Requested-With,If-Modified-Since,Cache-Control,Content-Type,Range"
  expose_headers: "Content-Length,Content-Range"
```

### Via a mongoDB collection:

There should be a collection called `tenant`, that shold look like that:
```json
{
    "_id" : "NINTENDO",
    "mappings" : {
        "mario-v3" : "http://mario.io/player/:name",
        "zelda" : "http://zelda.io/player/:id",
    }
}
```
Tenants are RestQL's equivalent of an environment. Each tenant has it's own resources.

Then, at the moment of running the container, you should pass `tenant=$tenantId` alongside with the `-Dmongo-url` flag:
```bash
docker run -p 9000:9000 -e JAVA_OPTS="-Dmongo-url=mongodb://my-mongo-ip:27017/restql-server -Dtenant=NINTENDO" restql-server-img
```

### Via an environment variable:

This is very straight forward, simply run:

```shell
planets=https://swapi.co/api/planets/:id ./bin/run.sh
```

## CORS Headers Configuration

Cross-Origin Resource Sharing - CORS, for short - is a specification that enables truly open access across domain-boundaries.
You can configure your own CORS headers either via the `restql.yml` config file or via environment variables.
CORS options follows the precedence `Environment > Config File > Default`.

### Via an environment variable:

```bash
CORS_ALLOW_ORIGIN=${allowed_custom_origin}
CORS_ALLOW_METHODS=${allowed_custom_methods}
CORS_ALLOW_HEADERS=${allowed_custom_headers}
CORS_EXPOSE_HEADERS=${allowed_custom_expose_headers}
```

### Via the configuration file:

```yaml
cors:
    allow_origin: ${allowed_custom_origin}
    allow_methods: ${allowed_custom_methods}
    allow_headers: ${allowed_custom_headers}
    expose_headers: ${allowed_custom_expose_headers}
```

You can check the CORS headers in your application by making an `OPTIONS` HTTP request to any restQL endpoint.