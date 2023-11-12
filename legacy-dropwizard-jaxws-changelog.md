# Historical Dropwizard JAX-WS Changelog

_This is the changelog from the [original repository](https://github.com/roskart/dropwizard-jaxws).
We retain it here for historical purposes, since the historical tags do not exist in this repository
due to it being a "disconnected fork" as described in the README._

_New releases will use [GitHub releases](https://github.com/kiwiproject/dropwizard-jakarta-xml-ws/releases) going forward under kiwiproject 🥝._

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
