# RestQL JS

`RestQL JS` is a node package available at NPM

## Installation

To install the node package, simply run 

```shell
npm i @b2wdigital/restql
```

## Node Example

```javascript
var restlq = require('@b2wdigital/restql')
 
// executeQuery(mappings, query, params, options) => <Promise>
restql
  .executeQuery(
    {user: "http://your.api.url/users/:name"},
    "from user with name = $name",
    { name: "Duke Nukem" })
  .then(response => console.log(response))
  .catch(error => console.log(error))
```

Learn more at our [NPM Page](https://www.npmjs.com/package/@b2wdigital/restql).