# TomcatRedirectToRootValve


## Overview

TomcatRedirectToRootValve is a custom Tomcat Valve that redirects all incoming requests 
to the root context (/). The original request URI and context path are stored as request attributes, 
allowing applications to access the original request information.


## Building the Project

To build the project, use the following Maven command:

```bash
mvn clean install
```
This will generate the JAR file redirect2root-1.0.0.jar in the target/ directory.


## Deployment Instructions

To deploy the Valve in your Tomcat installation:

Copy the generated JAR file to the Tomcat lib/ directory:

```bash
cp target/redirect2root-1.0.0.jar $TOMCAT_HOME/lib/
```

Replace $TOMCAT_HOME with the path to your Tomcat installation.

Register the Valve in server.xml under the <Host> configuration:

```xml
<Host name="localhost" appBase="webapps" unpackWARs="true" autoDeploy="true">
    <Valve className="my.custom.valve.RedirectToRootValve" />
</Host>
```

Restart Tomcat for the changes to take effect:

```bash
$TOMCAT_HOME/bin/shutdown.sh
$TOMCAT_HOME/bin/startup.sh
```


## Functionality

Redirects all requests to / (root context).

Stores the original request details as attributes:

Original Request URI: Available under the attribute originalRequestURI

Original Context Path: Available under the attribute originalContextPath

Example Usage

If a user requests:

```
http://example.com/app/page
```

The Valve will redirect them to:

```bash
http://example.com/
```

While preserving the original request information:

```java
String originalUri = (String) request.getAttribute("originalRequestURI");
String originalContext = (String) request.getAttribute("originalContextPath");
```


## License

This project is licensed under the BSD 3 License. See the LICENSE file for details.