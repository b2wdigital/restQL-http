
<img name="logo" src="./assets/images/logo_text.svg?sanitize=true">

 <br/><br/>

# What is restQL?
**restQL** is a microservice query language that makes easy to fetch information from multiple services in the most efficient manner.

## restQL-http

restQL-http is a server to run restQL queries, making easy to fetch information from multiple services in the most efficient manner. e.g:

```
from search
    with
        role = "hero"

from hero as heroList
    with
        name = search.results.name
```

Links:
* [Tackling microservice query complexity](https://medium.com/b2w-engineering/restql-tackling-microservice-query-complexity-27def5d09b40): Project motivation and history
* [@restQL](https://t.me/restQL): restQL Telegram Group
* [restql.b2w.io](http://restql.b2w.io): Project home page,
* [game.b2w.io](http://game.restql.b2w.io/): A game developed to teach the basics of restQL language,
* [restQL-clojure](https://github.com/B2W-BIT/restQL-clojure): If you want to embed restQL directly into your Clojure application,
* [restQL-java](https://github.com/B2W-BIT/restQL-java): If you want to embed restQL directly into your Java application,
* [restQL-manager](https://github.com/B2W-BIT/restQL-manager): To manage saved queries and resources endpoints (requires a MongoDB instance).
* [Wiki](https://github.com/B2W-BIT/restQL-http/wiki/RestQL-Query-Language): Git Hub documentation.
* [Code](https://cljdoc.org/d/b2wdigital/restql-core): cljdoc of [restQL-clojure](https://github.com/B2W-BIT/restQL-clojure)

Who're talking about restQL:

* [infoQ: restQL, a Microservices Query Language, Released on GitHub](https://www.infoq.com/news/2018/01/restql-released)
* [infoQ: 微服务查询语言restQL已在GitHub上发布](http://www.infoq.com/cn/news/2018/01/restql-released)
* [OSDN Mag: マイクロサービスクエリ言語「restQL 2.3」公開](https://mag.osdn.jp/18/01/12/160000)

## Next steps

1. Learn restQL [query language](/restql/queryLang),
2. Get involved :) We're looking for contributors, if you're interested open a Pull Request at our [GitHub Project](https://github.com/B2W-BIT/restQL-http).

## Help and community

If you need help you can reach the community on Telegram:
- https://t.me/restQL

## Releases

You can find our latest releases at our [release page](https://github.com/B2W-BIT/restQL-http/releases).

## License

Copyright © 2016-2019 B2W Digital

Distributed under the MIT License.
