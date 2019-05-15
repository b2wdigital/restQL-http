<p align="center">
  <a href="http://restql.b2w.io">
    <img width="537px" height="180px" alt="restQL" src="./doc/assets/images/logo-text.png?sanitize=true">
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


# Links
* [Documentation](http://doc.restql.b2w.io)
* [Code API](https://cljdoc.org/d/b2wdigital/restql-core): [restQL-clojure](https://github.com/B2W-BIT/restQL-clojure) code documentation
* [restql.b2w.io](http://restql.b2w.io): Project home page
* [game.b2w.io](http://game.b2w.io): A game developed to teach the basics of restQL language
* [restQL-clojure](https://github.com/B2W-BIT/restQL-clojure): If you want to embed restQL directly into your Clojure application,
* [restQL-java](https://github.com/B2W-BIT/restQL-java): If you want to embed restQL directly into your Java application,
* [restQL-manager](https://github.com/B2W-BIT/restQL-manager): To manage saved queries and resources endpoints. restQL-manager requires a MongoDB instance.
* [Tackling microservice query complexity](https://medium.com/b2w-engineering/restql-tackling-microservice-query-complexity-27def5d09b40): Project motivation and history

## Talk with us
* [#restql](https://clojurians.slack.com/messages/C8S6EG8BF): [clojurians](https://clojurians.slack.com) restQL Slack channel
* [@restQL](https://t.me/restQL): restQL Telegram Group

## Who's talking about restQL

* [infoQ: restQL, a Microservices Query Language, Released on GitHub](https://www.infoq.com/news/2018/01/restql-released)
* [infoQ: 微服务查询语言restQL已在GitHub上发布](http://www.infoq.com/cn/news/2018/01/restql-released)
* [OSDN Mag: マイクロサービスクエリ言語「restQL 2.3」公開](https://mag.osdn.jp/18/01/12/160000)
* [Build API's w/ GraphQL, RestQL or RESTful?](https://www.youtube.com/watch?v=OeUGswoYrvA)

# Quick start
Look at [this tutorial](http://doc.restql.b2w.io/#/restql/tutorial/intro)

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
Learn more about [**restQL** query language](http://doc.restql.b2w.io/#/restql/queryLang)

## Run **restQL** HTTP

**restQL** Server allows you to post `ad-hoc` queries and to reference resources pre-configured in the server startup.

1. Download the latest release in the [release page](https://github.com/B2W-BIT/restQL-http/releases),
2. Unzip the package,
3. Edit the file `restql.yml` with the resources you want to invoke,
3. Run bin/run.sh.

Post to http://your-server.ip:9000/run-query the body below and content-type text/plain:

```clojure
curl -H "Content-Type: text/plain" localhost:9000/run-query -d "from planets as allPlanets" 
```

# Help and community
Get involved :) We're looking for contributors, if you're interested ping ricardo.mayerhofer@b2wdigital.com

If you need help you can reach the community on Telegram:
- https://t.me/restQL 

## License

Copyright © 2016-2019 B2W Digital

Distributed under the MIT License.
