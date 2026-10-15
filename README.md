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

## 编译与验证

默认使用 **JDK 11**，Java 编译目标为 11，CI 使用 Temurin 11。构建会拒绝 JDK 8；
Maven 使用当前 `JAVA_HOME` 指向的 JDK，开始前请通过 `mvn -version` 确认。
建议使用 Maven 3.6.3 或更新版本。

macOS 可以在当前终端选择 JDK 11：

```bash
export JAVA_HOME="$(/usr/libexec/java_home -v 11)"
export PATH="$JAVA_HOME/bin:$PATH"
mvn -version
mvn clean verify
```

Linux/Windows 请将 `JAVA_HOME` 指向本机 JDK 11 安装目录。仅验证 Thrift 模块及其父项目：

```bash
mvn -pl amoro-shade-thrift -am clean verify
```

Thrift 依赖版本为 **0.25.0**，当前产物坐标为
`org.apache.amoro:amoro-shade-thrift:0.25.0-0.9-SNAPSHOT`，输出到
`amoro-shade-thrift/target/amoro-shade-thrift-0.25.0-0.9-SNAPSHOT.jar`。
包名仍使用 `org.apache.amoro.shade.thrift.org.apache.thrift`。
SLF4J API 保持由使用方提供，不打入该 JAR。

`verify` 阶段会编译并运行仅依赖成品 shaded JAR 与 SLF4J API 的消费端测试，
覆盖 Binary/Compact/JSON 协议、连续帧传输、两种 HTTP 客户端、Servlet/Annotation 重定位，
并检查类文件的 Java 11 兼容性。`mvn test` 不执行这些打包后的集成测试。
Thrift 编译器和 Amoro 主仓库的生成代码及服务联调需要在使用方另外验证。

## Sources

We currently do not release jars containing the shaded sources due to the unanswered legal questions raised [here](https://github.com/apache/flink-shaded/issues/25).

However, it is possible to build these jars locally by cloning the repository and calling `mvn clean package -Dshade-sources`.

## About

Apache Amoro (incubating) is an open source project of [The Apache Software Foundation](https://apache.org/) (ASF).
