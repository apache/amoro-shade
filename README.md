<!--
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Apache Amoro (incubating) Shaded Dependencies

This repository contains a number of shaded dependencies for the [Apache Amoro](https://amoro.apache.org/) project.

The purpose of these dependencies is to provide a single instance of a shaded dependency in the Amoro distribution, instead of each individual module shading the dependency.

Shaded dependencies contained here do not expose any transitive dependencies. They may or may not be self-contained.

When using these dependencies it is recommended to work directly against the shaded namespaces.

## Build and Validation

Use **JDK 11** by default. The Java compilation target is 11, and CI uses Temurin 11.
The build rejects JDK 8. Maven uses the JDK selected by `JAVA_HOME`; confirm the
active version with `mvn -version` before building. Maven 3.6.3 or later is recommended.

On macOS, select JDK 11 in the current terminal:

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
mvn clean verify
```

On Linux or Windows, set `JAVA_HOME` to your local JDK 11 installation directory.
To validate only the Thrift module and its parent project:

```bash
mvn -pl amoro-shade-thrift -am clean verify
```

The Thrift dependency version is **0.25.0**. The current artifact coordinates are
`org.apache.amoro:amoro-shade-thrift:0.25.0-0.9-SNAPSHOT`, and the output JAR is
`amoro-shade-thrift/target/amoro-shade-thrift-0.25.0-0.9-SNAPSHOT.jar`.
The relocated namespace remains `org.apache.amoro.shade.thrift.org.apache.thrift`.
Consumers must provide the SLF4J API; it is not bundled in this JAR.

The `verify` phase compiles and runs consumer tests against only the packaged
shaded JAR and the SLF4J API. These tests cover Binary/Compact/JSON protocols,
consecutive frames, both HTTP client paths, Servlet/Annotation relocation, and
Java 11 bytecode compatibility. `mvn test` does not run these integration tests,
which require the packaged artifact.
Consumers must separately validate the Thrift compiler, generated code in the
main Amoro repository, and service integration.

## Sources

We currently do not release jars containing the shaded sources due to the unanswered legal questions raised [here](https://github.com/apache/flink-shaded/issues/25).

However, it is possible to build these jars locally by cloning the repository and calling `mvn clean package -Dshade-sources`.

## About

Apache Amoro (incubating) is an open source project of [The Apache Software Foundation](https://apache.org/) (ASF).
