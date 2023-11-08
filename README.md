Dropwizard Jakarta XML Web Services
===================================

[![build](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/actions/workflows/build.yml/badge.svg)](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/actions/workflows/build.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-jakarta-xml-ws&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=kiwiproject_dropwizard-jakarta-xml-ws)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=kiwiproject_dropwizard-jakarta-xml-ws&metric=coverage)](https://sonarcloud.io/summary/new_code?id=kiwiproject_dropwizard-jakarta-xml-ws)

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
* Use standard Jakarta XML Web Services annotations, without custom deployment descriptors.
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

Since we are still using this library in our services which use Dropwizard and Jakarta XML Web Services, we decided to import the original repository and continue maintaining it for our own use, and anyone else who might want to use it. _We make no guarantees whatsoever about how long we will maintain it, and also plan to make our own changes such as changing the base package name to org.kiwiproject to be consistent with our other libraries._

All other [kiwiproject](https://github.com/kiwiproject/) projects are MIT-licensed. However, because the original
`dropwizard-jaxws` uses the Apache 2.0 license, we are keeping the Apache 2.0 license (otherwise to switch to MIT we
would have to gain consent of all contributors, which we do not want to do and probably can't since the original
author has not been active since October 2022).

Another thing to note is that we _imported_ this repository from the original, so that it is a "disconnected fork". We
did not want a reference to the original repository since it seems no longer maintained and so no changes here will ever
be pushed back upstream. Thus, while we maintain the history that this is a fork , it is completely disconnected and is
now a standalone (normal) repository.

Migrating from roskart/dropwizard-jaxws
---------------------------------------
_Note that as of November 7, 2023, we have not yet released an initial version._

There are two things you need to do in order to migrate. First, change the Maven coordinates so that the
groupId is org.kiwiproject, the artifactId is dropwizard-jakarta-xml-ws, and choose the latest version.

Second, when we imported this repository, we updated it from the Dropwizard 2.x and JAX-WS to Dropwizard 4.x
and Jakarta XML Web Services, which means that all the package names have changed from `javax` to `jakarta`.
This means you may need to change dependencies to [Jakarta XML Web Services](https://mvnrepository.com/artifact/jakarta.xml).

For the initial 0.5.0 version, we will retain the original package names (`com.roskart.dropwizard.jaxws`).
But in future versions, we will rename the packages to use the `org.kiwiproject` prefix and
then some suffix, e.g. `dropwizard.jakarta.xml.ws` (which matches the actual Jakarta packages which begin
with `jakarta.xml.ws`). Also, we will rename the modules, and may extract the example application into
a separate repository which will not be deployed to Maven Central, as the original repository did.

We will also remove deprecated code in subsequent releases, e.g. the deprecated methods in
`JAXWSBundle`.  Last, we will eventually rename classes containing `JAXWS` in them, for example
rename `JAXWSEnvironment` to `JakartaXmlWsEnvironment` or similar.


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

    private JAXWSBundle jaxWsBundle = new JAXWSBundle();

    @Override
    public void initialize(Bootstrap<MyApplicationConfiguration> bootstrap) {
        bootstrap.addBundle(jaxWsBundle);
    }

    @Override
    public void run(MyApplicationConfiguration configuration, Environment environment) throws Exception {
        jaxWsBundle.publishEndpoint(
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
HelloWorldSOAP helloWorld = jaxWsBundle.getClient(
    new ClientBuilder(HelloWorldSOAP.class, "http://server/path"));
System.out.println(helloWorld.sayHello());
```

Examples
--------
Module `dropwizard-jakarta-xml-ws-example` contains Dropwizard application (`JaxWsExampleApplication`) with the following SOAP
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

* See `JaxWsExampleApplication` for examples on usage of client side Jakarta XML Web Services handler and CXF interceptors.

### Running the examples:

After cloning the repository, go to the dropwizard-jakarta-xml-ws root folder and run:

```bash
mvn package
```

To run the example service:

```bash
java -jar \
  dropwizard-jaxws-example/target/dropwizard-jakarta-xml-ws-example-[version].jar \
  server dropwizard-jaxws-example/config.yaml
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

For example on building fat jar, see `dropwizard-jaxws-example/pom.xml`.

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

Changelog (from the original repository)
----------------------------------------

_Below is the changelog from the [original repository](https://github.com/roskart/dropwizard-jaxws).
Most likely this will be moved into a separate file later. We will use GitHub releases going forward
under kiwiproject 🥝._

### v1.2.3

- Upgraded to CXF 3.5.2 (see Issue #33).
- Upgraded to Dropwizard 2.0.29 (see Issue #33).
- Bump junit from 4.13.1 to 4.13.2 (see Issue #33).
- Bump mockito from 1.9.5 to 1.10.19 (see Issue #33).

### v1.2.2

- Upgraded to CXF 3.4.4 (see Issue #30).
- Upgraded to Dropwizard 2.0.24 (see Issue #30).
- Bump junit from 4.11 to 4.13.1 (see Pull Request #28).

### v1.2.1

- Upgraded to CXF 3.3.6 (see Issue #25).
- Upgraded to Dropwizard 2.0.9 (see Issue #25).

### v1.2.0

- Upgraded to Dropwizard 2.0.0 (see Issue #22).

### v1.1.0

- Invalid username or password returns 403 status code (see Issue #20).
- Null pointer on missing credentials (see Pull request #19).
- Upgraded to Dropwizard 1.3.13.
- Upgraded to CXF 3.2.9.

### v1.0.5

- Added possibility to set binding id on client proxy factory (see Issue #14).
- Upgraded to Dropwizard 1.3.5 (see Pull request #16).
- Upgraded to CXF 3.2.6 (see Issue #17).

### v1.0.4

- JAXWSBundle now returns JAX-WS endpoint (see Issue #13).

### v1.0.3

- Support for providing a property bag to JAX-WS endpoint (see Issue #13).
- Upgraded to Dropwizard 1.2.1.
- Upgraded to CXF 3.2.1.

### v1.0.2

- Upgraded to Dropwizard 1.1.0.
- Upgraded to CXF 3.1.11.

### v1.0.1

- Upgraded to Dropwizard 1.0.2.

### v1.0.0

- Upgraded to Dropwizard 1.0.0.
- Upgraded to CXF 3.1.6.
- Java 8 is used by default.
- Added support for publishedEndpointUrl (see Pull request #9).

### v0.10.2

- Added support for CXF @UseAsyncMethod annotation (see Pull request #8).

### v0.10.1

- Added support for multiple JAXWSBundle instances (see Issue #7).

### v0.10.0

- Upgraded to Dropwizard 0.9.2.

### v0.9.0

- Upgraded to Dropwizard 0.9.1.
- Upgraded to CXF 3.1.4.

### v0.8.0

- Project is now released to Maven Central. Maven coordinates were changed.

### v0.7.0

- Upgraded to Dropwizard 0.8.1.

### v0.6.0

- Upgraded to Dropwizard 0.8.0.
- Upgraded to CXF 3.0.4.

### v0.5.0

- Upgraded to Dropwizard 0.7.1.
- Upgraded to CXF 3.0.0.

### v0.4.0

- Added MTOM support and examples.

### v0.3.0

- Updated JAXWSBundle API: introduced EndpointBuilder and ClientBuilder.
- Added support for CXF interceptors.

### v0.2.0

- Upgraded to Dropwizard 0.7.0.
- Upgraded to CXF 2.7.8.

### v0.1.0

- Initial Release (uses Dropwizard 0.6.2).
