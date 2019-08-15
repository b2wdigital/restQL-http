# Running API with Manager
In this example we will run the **restQL-http** and **restQL-manager** with docker compose

#### Step 1
Clone repository and enter on this example directory

```sh
git clone https://github.com/B2W-BIT/restQL-http
cd restQL-http/examples/api-and-manager
```

#### Step 2
Start `restQL-http`, `restQL-manager` and `mongodb` with docker compose

```sh
docker-compose up
```

#### Step 2
Access restQL-manager in your browser at `http://localhost:3000`

#### Step 3
To set up the resources that will be used whe need to go to resources tab (`http://localhost:3000/resources-editor`), click at `Add New Resource` button and fill with data as in the above example.

| Field | Value
| --- | --- |
| Authorization Key | *Empty* |
| Resource Name | launches |
| Resource Url | https://api.spacexdata.com/v3/launches/:flight_number |

\* To set up Authorization Key go to restQL docs.

#### Step 4
Go back to initial page, by clicking at `Query Editor` button and setup a new query.

```
from launches
  with flight_number = $number
```

You can try the new query by clicking at `Run Query` and save with `Save Query` button and data as the above example.

| Field | Value
| --- | --- |
| Namespace | spacex |
| Query Name | get-launch |

#### Step 5
Make an `AD-HOC` request on restql-http
```sh
curl --request POST \
     --url "http://localhost:9000/run-query" \
     --header 'content-type: text/plain' \
     --data 'from launches with flight_number = 18'
```
#### Step 6
Make an `'Saved Query'` request on restql-http

```sh
curl --request GET \
     --url "http://localhost:9000/run-query/spacex/get-launch/1?number=18"
```

Learn more about **restQL** with our [docs](http://docs.restql.b2w.io/#/restql/queryLang)