# REST Query Language

## Query Syntax

The clause order matters when making restQL queries. The following is a full reference to the query syntax, available clauses and order.

```
[ [ use modifier = value ] ]

METHOD resource-name [as some-alias] [in some-resource]
  [ headers HEADERS ]
  [ timeout INTEGER_VALUE ]
  [ with WITH_CLAUSES ]
  [ [only FILTERS] OR [hidden] ]
  [ [ignore-errors] ]
```

## Methods

restQL supports the most popular HTTP methods (GET, POST, PUT and DELETE).

* **from** - HTTP GET
* **to** - HTTP POST
* **into** - HTTP PUT
* **update** - HTTP PATCH
* **delete** - HTTP DELETE

In queries with `to`, `into` and `update`, the `with` params will be used as the body of request. For example:

```restql
to users
  with
    id       = "user.id"
    username = "user.name"
    password = "super.secret"
```

Maps to the following call:

```shell
POST http://some.api/users/
BODY { "id": "user.id", "username": "user.name", "password": "super.secret" }
```

## Starting a query

```restql
from planets as earth
    with
        name = "earth"
```

In this example, `planets` is the resource being queried, `earth` is the bound variable name to store the result. The parameters used in the query item are within a `with` clause, which simply contains a list of key/value pairs.

You can put more than one item in the same line, separating them by spaces, so the two examples below are equivalent:

```restql
//  You can write comments like this

from planets as a
    with
        size = "big", type = "gas"
```

```restql
//  The bound variable name can be the same
//  as the resource without problems

from planets as a
    with
        size = "big"
        type = "gas"
```

## Parameters format

Inside the `with` clause, you must specify a list of key/value pairs, the values can be one of the following types:

- A string, which must be enclosed in **double quotes**
- A number, that can have a floating point. Scientific notation is not supported.
- A boolean: either `true` or `false` in **lower case**
- The null value: `null`
- A list of values. Lists are enclosed in **square brackets** and separated by either **commas** or **newlines**
- A key/value structure. Structures must be enclosed in **curly braces**, with each pair separated from each other with a **comma** or a **newline**. The key and the value must be separated with a **colon**, similar to a `json` object
- A value from another query item, in which case we use the name of the bound variable followed by a **dot** specifying the field. You can keep adding dots to go arbitrarily deep within the chained value

Here is an example containing all the types mentioned above:

```restql
from superheroes as protagonist
    with
        name = "Super Duper"          // String type
        level = 15                    // Number Type
        honored = true                // Boolean Type
        lastDefeat = null             // Null Value
        using = ["sword", "shield"]   // List Type
        stats = {health: 100,
                 magic: 100}          // KeyValue Type

from superheroes as sidekick
    with
        id = protagonist.sidekick.id  // Chaining Type
```

## Using Variables

Alongside directly typing the value of a parameter within the `with` clause, it is possible to define variables that will have their values replaced via query parameters:

```restql
from superheroes
    with
        name = $heroName
        level = $heroLevel
        powers = $heroPowers
```
Those parameters' values will be defined in the url:
```
localhost:9000/run-query?heroName="Superman"&heroLevel=99&heroPowers=["flight","heat vision","super strenght"]
```

## Specifying Headers

Right before the `with` clause, you can add a `headers` clause to write the headers you want to send within that Query Item. The headers are a list of key/value pairs, like the `with` clause items, but the values must all be strings.

```restql
// example using headers
from superheroes as hero
headers
    Authorization = "Basic user:pass"
    Accept        = "application/json"
with
    id = 1
```
It is important to state that headers present in the query will substitute any request headers with the same name it has, so, in the earlier example, even if the request already has an `Authorization` header, it will be replaced by `"Basic user:pass"` 


## Timeout Control

A specific Query Item has the default timeout of 5 seconds, which is pretty high. To change the timeout value for any Query Item, use the `timeout` clause, which accepts simply an integer value, representing the **milliseconds** to wait before the request times out.

The `timeout` clause appears **before** the `with` clause, but **after** the `headers` clause

```restql
from superheroes as hero
headers
    Authorization = "Basic user:pass"
    Accept        = "application/json"
timeout 200
with
    id = 1
```

## Expanding and flattening

Whenever restQL finds a List value in a `with` parameter, it will perform an **expansion**, which means it will make one request for each item in the list. Suppose we want to fetch the `superheroes` with ids 1, 2 and 3:

```restql
// this query will make THREE requests to the superheroes resource
from superheroes as party
    with
        id = [1, 2, 3]
```
In this case, restQL will perform the following HTTP calls:

`GET http://some.api/superhero?id=1`

`GET http://some.api/superhero?id=2`

`GET http://some.api/superhero?id=3`

If this behaviour is not what you want, but rather pass all values in a single request, you can set the expansion of any parameter to false by using the **apply** operator `->` with the `flatten` function.

```restql
// now only ONE request will be performed
from superheroes as fused
    with
        id = [1, 2, 3] -> flatten
```

Using `flatten`, restQL will perform just **one** HTTP call, as follows:

`GET http://some.api/superhero?id=1&id=2&id=3`

## Encoding Values

Some APIs may accept formatted values as parameters, and this problem is usually solved in restQL by `encoders`.
Whenever restQL finds a Key/Value structure as a parameter, it will transform it to string using an encoder.

To choose an encoder, you must also use the `->` operator. Here is an example:

```restql
from superheroes as hero
    with
        stats = {health: 100,
                 magic: 100} -> json   // encode this value as a json string
```

By default, if no encoder is specified, the `json` encoder will be used, so the above example is redundant.
The rules in this case for the name in the right of the `->` operator are simple: if it's not `flatten`, it will be treated as an encoder, and will use one with the same name.

Here is an example using encoders in different ways

```restql
from superheroes as hero
    with
        bag = {capacity: 10} -> base64
```

Note that the base64 encoder is presented simply as an explanation of the syntax and they don't ship with the installation of the restQL-http. You can add more encoders by installing plugins or by developing your own.


## Selecting the returned fields

When the response of a given resource is bloated you may want to filter the fields in order to reduce query payload. You can do this by adding an `only` clause to the end of a Query Item, simply listing the fields you want:

```restql
from superheroes as hero
    with
        id = 1
    only
        name
        items
        skills.id
        skills.name
        nicknames -> matches("^Super")
```

For the sub-elements, like `skills.id` and `skills.name` above, the fields `id` and `name` will be nested in a `skills` top level field.

In the case of the `nicknames`, we applied a filter by using the `-> matches` operator. Filters can be installed/added in the same way encoders can, by either installing plugins or developing your own.

One last scenario would be that only certain fields should be filtered, but all the others should return normally. Using the `only` clause would be tedious if we had to list every single field, only to filter one or two. In that case you can use the `*` selector, and it will return all other fields that are not listed:

```restql
from superheroes as hero
    with
        id = 1
    only
        nicknames -> matches("^Super"),
        *
```

If you want to query a resource only to chain the invocation to another resource you can use the `hidden` modifier. This will suppress this resource in the query response. e.g.:

```restql
from hero
    with
        name = "Restman"
    hidden

from sidekick
    with
        hero = hero.id
```

## Aggregating result in another resource

With `IN` aggregation you can easily append a resource result to another.

```restql
from hero
    with
        name = "Restman"

from sidekick in hero.sidekick
    with
        hero = hero.sidekickId
```

The query above will be aggregated as the result bellow:

```json
{
    "hero": {
        "details": {...},
        "result": {
            "id": 1,
            "name": "Restman",
            "sidekickId": 10,
            "sidekick": {
                "id": 10,
                "name": "Super"
            }
        }
    },
    "sidekick": {
        "details": {...}
    }
}
```

## Ignoring error of a given resource

By default restQL returns the highest HTTP status code returned by the queried resources. If you'd like restQL to ignore a given resource when calculating the return status code you can use ignore-error modifier on that resource.

This is useful when querying critical and non-critical resources in the same query. As an example, we might want to get all available products and their ratings, but if we get an error from rating we want to ignore it and show the products anyway. For cases like these, we can use the `ignore-errors` expression, as follows:

```restql
from products as product

from ratings
  with
    productId = product.id
  ignore-errors
```

The query above will return a success HTTP status code even when the ratings resources returns an error.

### Cache Control Header

By default restQL returns the lowest cache-control value among the queried resources. If you'd like to fix a cache-control value in your query you can set the `use` modifier in the first statement of your the query. 

```restql
use max-age = 600

from products
```

The query above will make restQL return an extra header `Cache-Control: max-age=600`, enabling a proxy to cache the request for 10 minutes (600 seconds).
