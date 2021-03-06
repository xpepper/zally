[![Build Status](https://travis-ci.org/zalando/zally.svg?branch=master)](https://travis-ci.org/zalando/zally)
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/05a7515011504c06b1cb35ede27ac7d4)](https://www.codacy.com/app/zally/zally?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=zalando/zally&amp;utm_campaign=Badge_Grade)
[![Codacy Badge](https://api.codacy.com/project/badge/Coverage/05a7515011504c06b1cb35ede27ac7d4)](https://www.codacy.com/app/zally/zally?utm_source=github.com&utm_medium=referral&utm_content=zalando/zally&utm_campaign=Badge_Coverage)

<img src="logo.png" width="200" height="200" />

### Zally: A minimalistic, simple-to-use API linter

... for OpenAPI 3 and Swagger 2 API specifications.

Its standard configuration will check your APIs against the rules defined in [Zalando's RESTful Guidelines](http://zalando.github.io/restful-api-guidelines/), but anyone can use it **out-of-the-box**.

Zally's easy-to-use [CLI](cli/README.md) uses the server in the background so that you can check your API *on the spot*. It also features an intuitive [Web UI](web-ui/README.md) that shows implemented rules and lints external files and (with its online editor) API definitions.

More about Zally:
- OpenAPI-friendly: accepts [OpenAPI 3 and Swagger Specifications](https://github.com/OAI/OpenAPI-Specification) .yaml and JSON formats; includes a server that lints your OpenAPI files; and parses OpenAPI files using [swagger-parser](https://github.com/swagger-api/swagger-parser)
- Using `x-zally-ignore` extension in your API definition, you can disable rules for a specific API
- Applying rule changes is only necessary in the server component
- API-specific code written in Java 8 with [Spring Boot](https://github.com/spring-projects/spring-boot) for better integration
- Rule implementation is optimal/possible in [Kotlin](https://kotlinlang.org/)

### Technical Dependencies

- Kotlin and Java 8 (server) with Spring Boot 
- Golang 1.7+: for CLI
- Node.js 7.6+: for web UI

### Installation and Usage

To give Zally a quick try, first run the server locally and then use the CLI tool.

The [Server Readme](server/README.md), [CLI Readme](cli/README.md) and [Web UI Readme](web-ui/README.md) include more detailed installation steps for each component.

### Quick start guide

You can build and run the whole Zally stack (web-ui, server and database) by
executing this script:

```bash
./build-and-run.sh
```

Web UI is accessible on `http://localhost:8080`; Zally server on `http://localhost:8000`

*To run zally with github integration*:
```bash
export GITHUB_OAUTH_TOKEN=your_github_oauth_token
export GITHUB_SECRET=your_github_secret

./build-and-run.sh --bark
``` 
Github webhook endpoint is accessible on http://localhost:8081/github_webhook
More details about how to register[Webhooks](https://developer.github.com/webhooks/) 

### Contributing

Zally welcomes contributions from the open source community. To get started, take a look at our [contributing guidelines](CONTRIBUTING). Then check our [Project Board](https://github.com/zalando/zally/projects/1) and [Issues Tracker](https://github.com/zalando/zally/issues) for ideas. 

#### Roadmap
For Zally [version 1.5](https://github.com/zalando/zally/milestone/3), we're focusing on:
- Making Zally easier to extend and adjust to custom guidelines and rules
- Better integration testing approaches
- Making further rules compatible with OpenAPI 3
- Providing more utilities for check developers
- Improving check execution process
- Provide high-quality documentation for check developers, operators and users

If you have ideas for these items, please let us know.

### Contact

Feel free to contact one the [maintainers](MAINTAINERS).


### License

MIT license with an exception. See [license file](LICENSE).
