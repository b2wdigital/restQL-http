<p align="center">
  <a href="http://restql.b2w.io">
    <img width="537px" height="180px" alt="restQL" src="./docs/assets/images/logo_text.png?sanitize=true">
  </a>
</p>

<p align="center">
  restQL-http is a server to run <strong>restQL</strong> queries, making easy to fetch information from multiple services in the most efficient manner
</p>


<p align="center">
  <a href="https://travis-ci.org/B2W-BIT/restQL-http" title="restQL on travis-ci">
    <img src="https://travis-ci.org/B2W-BIT/restQL-http.svg?branch=master" alt="restQL on travis-ci">
  </a>
</p>

# Getting Started

## Running restQL HTTP

restQL server allows you to post ad-hoc queries and to reference resources pre-configured in the server startup.

1. Make sure you have Java 11 or superior installed,
2. Download the latest release in the [release page](https://github.com/B2W-BIT/restQL-http/releases),
3. Unzip the package,
4. Configure [resource mappings](http://docs.restql.b2w.io/#/restql/resource-mappings),
5. Run bin/run.sh.

Post to http://your-server.ip:9000/run-query the body below and content-type text/plain:

```clojure
curl -H "Content-Type: text/plain" localhost:9000/run-query -d "from planets as allPlanets" 
```

For a more complex example follow [this tutorial](http://docs.restql.b2w.io/#/restql/tutorial/intro). Alternatively you can use our [official docker image](https://hub.docker.com/r/b2wdigital/restql-http).

## Our query language
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
e.g:
```restQL
from search
    with
        role = "hero"

from hero as heroList
    with
        name = search.results.name
```
Learn more about [**restQL** query language](http://docs.restql.b2w.io/#/restql/query-language)

# Links
* [Docs](http://docs.restql.b2w.io)
* [Code API](https://cljdoc.org/d/b2wdigital/restql-core): [restQL-clojure](https://github.com/B2W-BIT/restQL-clojure) code documentation
* [restQL-clojure](https://github.com/B2W-BIT/restQL-clojure): If you want to embed restQL directly into your Clojure application,
* [restQL-java](https://github.com/B2W-BIT/restQL-java): If you want to embed restQL directly into your Java application,
* [restQL-manager](https://github.com/B2W-BIT/restQL-manager): To manage saved queries and resources endpoints. restQL-manager requires a MongoDB instance.
* [Tackling microservice query complexity](https://medium.com/b2w-engineering/restql-tackling-microservice-query-complexity-27def5d09b40): Project motivation and history

## Reach the community
* [#restql](https://clojurians.slack.com/messages/C8S6EG8BF): [clojurians](https://clojurians.slack.com) restQL Slack channel
* [@restQL](https://t.me/restQL): restQL Telegram Group

## Who's talking about restQL

* [infoQ: restQL, a Microservices Query Language, Released on GitHub](https://www.infoq.com/news/2018/01/restql-released)
* [infoQ: 微服务查询语言restQL已在GitHub上发布](http://www.infoq.com/cn/news/2018/01/restql-released)
* [OSDN Mag: マイクロサービスクエリ言語「restQL 2.3」公開](https://mag.osdn.jp/18/01/12/160000)
* [Build API's w/ GraphQL, RestQL or RESTful?](https://www.youtube.com/watch?v=OeUGswoYrvA)

## To Do
- [ ] Create endpoints for creating and updating queries and resources.
- [ ] Implement a *soft cache reset* when queries or resources are created or updated.  

## License

Copyright © 2016-2019 B2W Digital

Distributed under the MIT License.
