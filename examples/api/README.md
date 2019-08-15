# Running API
In this example we will run the **restQL-http** with docker.

#### Step 1
Clone repository and enter on this example directory

```sh
git clone https://github.com/B2W-BIT/restQL-http
cd restQL-http/examples/api
```

#### Step 2
Set up the `restql.yml` with `resource mappings` and `saved queries`

```yml
mappings:
  launches: https://api.spacexdata.com/v3/launches/:flight_number

queries:
  spacex:
    get-launch:
      - |
        from launches
          with flight_number = $number
      - |
        from launches
          with
            flight_number = $number
          only
            rocket.rocket_name
            rocket.second_stage.payloads.payload_type
            rocket.second_stage.payloads.nationality
```

#### Step 3
Start the `restQL-http` official docker image with `restql.yml` configured
```sh
docker run -p 9000:9000 -v ${PWD}/restql.yml:/usr/src/restql-http/restql.yml b2wdigital/restql-http:latest
```

#### Step 4
Make an `AD-HOC` request
```sh
curl --request POST \
     --url "http://localhost:9000/run-query" \
     --header 'content-type: text/plain' \
     --data 'from launches with flight_number = 18'
```
#### Step 5
Make an `'Saved Query'` request

With first version of the saved query
```sh
curl --request GET \
     --url "http://localhost:9000/run-query/spacex/get-launch/1?number=18"
```

With second version of the saved query
```sh
curl --request GET \
     --url "http://localhost:9000/run-query/spacex/get-launch/2?number=18"
```

Learn more about **restQL** with our [docs](http://docs.restql.b2w.io/#/restql/queryLang)