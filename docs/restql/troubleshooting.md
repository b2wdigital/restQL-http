# Troubleshooting

If you're having problems invoking the underlying resource, restQL offers a debug option which will give more details about the resource restQL is trying to call, including the called URL and its parameters. 

To enable debug mode add the query parameter `_debug=true` in your request. E.g.:

```bash
curl -d "from planets as allPlanets" -H "Content-Type: text/plain" localhost:9000/run-query?_debug=true  
```
This will add the following field under response `details`:
```json
{<...>
    "debug": {
        "url": "https://swapi.co/api/planets/",
        "timeout": 5000,
        "response-time": 1261,
        "request-headers": {
          "restql-query-control": "ad-hoc",
          "user-agent": "insomnia/6.3.2",
          "accept": "*/*"
        }
        <...>
    }
```
For more information, you can contact the restQL team at our communication channels:
* [@restQL](https://t.me/restQL): restQL Telegram Group
* <restql@b2wdigital.com>: restQL team e-mail