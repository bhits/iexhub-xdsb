# Short Description

The IExHub XDS.b provides RESTful endpoints to facilitate information exchange to and from an HIE.

# Full Description

# Supported Source Code Tags and Current `Dockerfile` Link

[`0.0.1 (latest)`](https://github.com/bhits-dev/iexhub-xdsb/releases/tag/0.0.1)

[`Current Dockerfile`](../iexhub-xdsb/src/main/docker/Dockerfile)

For more information about this image, the source code, and its history, please see the [GitHub repository](https://github.com/bhits-dev/iexhub-xdsb).

# What is the IExHub XDS.b API?

The IExHub XDS.b (iexhub-xdsb) Spring Boot project provides RESTful endpoints to allow applications to interoperate with standards-based Health Information Exchange (HIE) organizations. In particular, two endpoints are provided for sharing clinical records facilitating Cross-Enterprise Document Sharing (XDS.b) operations: (1) to get a patient's health data from the HIE, which in turn uses ITI-18 Registry Stored Query and ITI-43 Retrieve Document Set transactions and (2) to publish a patient's clinical record using ITI-41 Provide and Register Document transactions.

For more information and related downloads for Consent2Share, please visit [Consent2Share](https://bhits-dev.github.io/consent2share/).

# How to Use This Image

## Start a iexhub-xdsb Instance

Be sure to familiarize yourself with the repository's [README.md](https://github.com/bhits-dev/iexhub-xdsb) file before starting the instance.

`docker run  --name iexhub-xdsb -d bhitsdev/iexhub-xdsb:latest <additional program arguments>`

*NOTE: In order for this project to fully function as a microservice in the Consent2Share application, it is required to setup the dependency microservices and the support level infrastructure. Please refer to the Consent2Share Deployment Guide in the corresponding Consent2Share release (see [Consent2Share Releases Page](https://github.com/bhits-dev/consent2share/releases)) for instructions to setup the Consent2Share infrastructure.*
 
## Configure

The Spring profiles `application-default` and `docker` are activated by default when building images.

This project can run with the default configuration which is from three places: `bootstrap.yml`, `application.yml`, and the data which the [`Configuration Server`](https://github.com/bhits-dev/config-server) reads from the `Configuration Data Git Repository`. Both `bootstrap.yml` and `application.yml` files are located in the class path of the running application.

We **recommend** overriding the configuration as needed in the `Configuration Data Git Repository`, which is used by the `Configuration Server`.

Also, [Spring Boot](https://projects.spring.io/spring-boot/) supports other ways to override the default configuration to configure the project for a certain deployment environment. 

The following is an example to override the default database password:

`docker run -d bhitsdev/iexhub-xdsb:latest --spring.port=80`

## Environment Variables

When you start the IEXHUB_XDSB image, you can edit the configuration of the IEXHUB_XDSB instance by passing one or more environment variables on the command line. 

### JAR_FILE

This environment variable is used to setup which jar file will run. You need to mount the jar file to the root of container.

`docker run --name iexhub-xdsb -e JAR_FILE="iexhub-xdsb-latest.jar" -v "/path/on/dockerhost/iexhub-xdsb-latest.jar:/iexhub-xdsb-latest.jar" -d bhitsdev/iexhub-xdsb:latest`

### JAVA_OPTS 

This environment variable is used to setup a JVM argument, such as memory configuration.

`docker run --name iexhub-xdsb -e "JAVA_OPTS=-Xms512m -Xmx700m -Xss1m" -d bhitsdev/iexhub-xdsb:latest`

### DEFAULT_PROGRAM_ARGS 

This environment variable is used to setup an application argument. The default value is: "--spring.profiles.active=application-default, docker".

`docker run --name iexhub-xdsb -e DEFAULT_PROGRAM_ARGS="--spring.profiles.active=application-default,ssl,docker" -d bhitsdev/iexhub-xdsb:latest`

# Supported Docker Versions

This image is officially supported on Docker version 1.13.0.

Support for older versions (down to 1.6) is provided on a best-effort basis.

Please see the [Docker installation documentation](https://docs.docker.com/engine/installation/) for details on how to upgrade your Docker daemon.

# License

View [license](https://github.com/bhits-dev/iexhub-xdsb/blob/master/LICENSE) information for the software contained in this image.

# User Feedback

## Documentation
 
Documentation for this image is stored in the [bhitsdev/iexhub-xdsb](https://github.com/bhits-dev/iexhub-xdsb) GitHub repository. Be sure to familiarize yourself with the repository's README.md file before attempting a pull request.

## Issues

If you have any problems with or questions about this image, please contact us through a [GitHub issue](https://github.com/bhits-dev/iexhub-xdsb/issues).

