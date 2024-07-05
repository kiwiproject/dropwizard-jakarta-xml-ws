Dropwizard Jakarta XML Web Services
===================================

[![build](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/actions/workflows/build.yml/badge.svg)](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-jakarta-xml-ws&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kiwiproject_dropwizard-jakarta-xml-ws)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-jakarta-xml-ws&metric=coverage)](https://sonarcloud.io/summary/new_code?id=kiwiproject_dropwizard-jakarta-xml-ws)
[![javadoc](https://javadoc.io/badge2/org.kiwiproject/dropwizard-jakarta-xml-ws/javadoc.svg)](https://javadoc.io/doc/org.kiwiproject/dropwizard-jakarta-xml-ws)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache--2.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Maven Central](https://img.shields.io/maven-central/v/org.kiwiproject/dropwizard-jakarta-xml-ws)](https://central.sonatype.com/artifact/org.kiwiproject/dropwizard-jakarta-xml-ws/)

---

🥝 _This README will be updated as we transition from the original dropwizard-jaxws to this repository._ 🥝

---

Introduction
------------

Dropwizard Jakarta XML Web Services is a [Dropwizard](https://www.dropwizard.io/) Bundle that enables building SOAP web
services and clients using [Jakarta XML Web Services](https://jakarta.ee/specifications/xml-web-services/) with Dropwizard.

Features
--------
* Uses [Apache CXF](https://cxf.apache.org/) web services framework (no Spring Framework dependency).
* Java-first and WSDL-first service development.
* Use standard Jakarta XML Web Services annotations without custom deployment descriptors.
* [Metrics](https://metrics.dropwizard.io/) instrumentation: @Metered, @Timed and @ExceptionMetered annotations.
* Dropwizard validation support.
* Dropwizard Hibernate support (@UnitOfWork).
* Dropwizard basic authentication using Dropwizard Authenticator.
* Web service client factory.
* Support for Jakarta XML Web Services handlers, MTOM, CXF interceptors (both client and server side) and
  CXF @UseAsyncMethod annotation.

Background
----------
This library was imported from [roskart/dropwizard-jaxws](https://github.com/roskart/dropwizard-jaxws), which
as of November 2023 seems to be no longer maintained by the original creator.

Since we are still using this library in our REST web services that use Dropwizard and Jakarta XML Web Services,
we decided to import the original repository and continue maintaining it for our own use.
And of course, anyone else who might want to use it.

_We make no guarantees whatsoever about how long we will maintain it, and also plan to make
our own changes such as changing the base package name to org.kiwiproject to be consistent with our other libraries._

All other [kiwiproject](https://github.com/kiwiproject/) projects are MIT-licensed. However, because the original
`dropwizard-jaxws` uses the Apache 2.0 license, we are keeping the Apache 2.0 license (otherwise to switch to MIT we
would have to gain consent of all contributors, which we do not want to do. And, we probably can't since the original
author has not been active since October 2022).

Another thing to note is that we _imported_ this repository from the original, so that it is a "disconnected fork." We
did not want a reference to the original repository since it seems no longer maintained and so no changes here will ever
be pushed back upstream. Thus, while we maintain the history that this is a fork, it is completely disconnected and is
now a standalone (normal) repository.

Migrating from roskart/dropwizard-jaxws
---------------------------------------
There are two things you need to do to migrate. First, change the Maven coordinates so that the
groupId is org.kiwiproject, the artifactId is dropwizard-jakarta-xml-ws, and choose the latest version.

Second, when we imported this repository, we updated it from the Dropwizard 2.x and JAX-WS to Dropwizard 4.x
and Jakarta XML Web Services, which means that all the package names have changed from `javax` to `jakarta`.
This means you will need to change dependencies
to [Jakarta XML Web Services](https://mvnrepository.com/artifact/jakarta.xml).

For the initial [0.5.0](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/releases/tag/v0.5.0) version, we will
retain the original package names (`com.roskart.dropwizard.jaxws`).

Release [0.6.0](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/releases/tag/v0.6.0) will remove
deprecated code, i.e., the deprecated methods in `JAXWSBundle`. It will also
rename the modules so that they are consistent, i.e., rename `dropwizard-jaxws` to `dropwizard-jakarta-xml-ws`.
Finally, 0.6.0 *comments out* the Maven Shade plugin in the POM of `dropwizard-jakarta-xml-ws-example` so
that the JAR deployed to Maven Central is small (a few KB instead of the 40+ MB uber-jar).

_**Release [0.7.0](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/releases/tag/v0.7.0) has multiple
breaking API changes**_. First, it renames the packages to `org.kiwiproject.dropwizard.jakarta.xml.ws` (which
matches the actual Jakarta packages which begin with `jakarta.xml.ws`). Release 0.7.0 also renames the bundle
and environment classes to start with `JakartaXmlWs` instead of `JAXWS`, so they become `JakartaXmlWsBundle`
and `JakartaXmlWsEnvironment` respectively. Finally, it renames the application and configuration classes
in the example application to `JakartaXmlWsExampleApplication` and `JakartaXmlWsExampleConfiguration`
respectively.

We may, in a future release, extract the example application into a separate repository
which will not be deployed to Maven Central, as the original repository did.


Using
-----

To use dropwizard-jakarta-xml-ws in your project, add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>org.kiwiproject</groupId>
    <artifactId>dropwizard-jakarta-xml-ws</artifactId>
    <version>[current-version]</version>
</dependency>
``````

Hello World
-----------

**SOAP service:**

```java
@Metered
@WebService
public HelloWorldSOAP {
    @WebMethod
    public String sayHello() {
        return "Hello world!";
    }
}
```

**Dropwizard application:**

```java
public class MyApplication extends Application<MyApplicationConfiguration> {

  private JakartaXmlWsBundle<MyApplicationConfiguration> jswBundle = new JakartaXmlWsBundle<>();

    @Override
    public void initialize(Bootstrap<MyApplicationConfiguration> bootstrap) {
      bootstrap.addBundle(jswBundle);
    }

    @Override
    public void run(MyApplicationConfiguration configuration, Environment environment) throws Exception {
      jswBundle.publishEndpoint(
            new EndpointBuilder("/hello", new HelloWorldSOAP()));
    }

    public static void main(String[] args) throws Exception {
        new MyApplication().run(args);
    }
}
```

Client
------

Using HelloWorldSOAP web service client:

```java
HelloWorldSOAP helloWorld=jwsBundle.getClient(
    new ClientBuilder(HelloWorldSOAP.class, "http://server/path"));
System.out.println(helloWorld.sayHello());
```

Examples
--------
Module `dropwizard-jakarta-xml-ws-example` contains Dropwizard application (`JakartaXmlWsExampleApplication`) with the
following SOAP
web services and RESTful resources:

* **SimpleService**: A minimal 'hello world' example.

* **JavaFirstService**: Java first development example. `JavaFirstService` interface uses Jakarta XML Web Services annotations.
`JavaFirstServiceImpl` contains service implementation instrumented with Metrics annotations. Service is secured with
basic authentication using `dropwizard-auth`. `BasicAuthenticator` implements Dropwizard `Authenticator`.
`JavaFirstServiceImpl` accesses authenticated user properties via injected Jakarta XML Web Services `WebServiceContext`.

* **WsdlFirstService**: WSDL first development example. WSDL is stored in `resources/META-INF/WsdlFirstService.wsdl`.
Code is generated using `cxf-codegen-plugin` which is configured in `pom.xml`. `WsdlFirstServiceImpl` contains service
implementation with blocking and non-blocking methods. `WsdlFirstServiceHandler` contains server-side Jakarta XML Web Services handler.

* **HibernateExampleService**: `dropwizard-hibernate` example. `HibernateExampleService` implements the service.
`@UnitOfWork` annotations are used for defining transactional boundaries. `@Valid` annotation is used for parameter
validation on `createPerson` method. `HibernateExampleService` accesses the database through `PersonDAO`. Embedded H2
database is used. Database configuration is stored in Dropwizard config file `config.yaml`.

* **MtomService**: WSDL first MTOM attachment example. WSDL is stored in `resources/META-INF/MtomService.wsdl`.
Code is generated using `cxf-codegen-plugin` which is configured in `pom.xml`. `MtomServiceImpl` contains service
implementation with MTOM enabled.

* **AccessProtectedServiceResource**: Dropwizard RESTful service which uses `JavaFirstService` client to invoke
`JavaFirstService` SOAP web service on the same host. User credentials are provided to access protected service.

* **AccessWsdlFirstServiceResource**: Dropwizard RESTful service which uses `WsdlFirstService` client to invoke
`WsdlFirstService` SOAP web service on the same host. `WsdlFirstClientHandler` contains client-side
Jakarta XML Web Services handler.

* **AccessMtomServiceResource**: Dropwizard RESTful service which uses `MtomService` client to invoke
`MtomService` SOAP web service on the same host as an example for client side MTOM support.

* See `JakartaXmlWsExampleApplication` for examples on usage of client side Jakarta XML Web Services handler and CXF
  interceptors.

### Running the examples:

_Before doing anything else, edit pom.xml in the dropwizard-jakarta-xml-ws root folder and uncomment the
maven-shade-plugin. This is temporary until the examples are moved into a separate repository._

After cloning the repository, go to the dropwizard-jakarta-xml-ws root folder and run:

```bash
mvn package
```

To run the example service:

```bash
java -jar \
  dropwizard-jakarta-xml-ws-example/target/dropwizard-jakarta-xml-ws-example-[version].jar \
  server dropwizard-jakarta-xml-ws-example/config.yaml
```

Notes
-----

### Building FAT jar

When using `maven-shade-plugin` for building fat jar, you must add the following `transformer` element to plugin
configuration:

```xml
<transformer implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
    <resource>META-INF/cxf/bus-extensions.txt</resource>
</transformer>
```

For example on building fat jar, see `dropwizard-jakarta-xml-ws-example/pom.xml`.

When using Gradle and a recent version of [shadowJar](https://github.com/johnrengelman/shadow) use the following snippet:

```groovy
shadowJar {
    // ...
    append('META-INF/cxf/bus-extensions.txt')
}
```

License
-------
Apache Software License 2.0, see [LICENSE](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/blob/main/LICENSE).

Changelog
---------

The original repository listed its complete change log at the end of this README.

We have moved it [here](legacy-dropwizard-jaxws-changelog.md) for historical purposes.

Releases in this repository use [GitHub releases](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/releases).
