/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.amoro.shade.thrift;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.LoggerFactory;

class ThriftShadeIT {
  @TempDir static Path classes;

  private static Path shadedJar;
  private static URLClassLoader loader;

  @BeforeAll
  static void compileConsumer() throws Exception {
    shadedJar = Path.of(System.getProperty("shadedJar"));
    assertTrue(Files.isRegularFile(shadedJar), "Run verify to build the shaded JAR first");
    URL slf4j = LoggerFactory.class.getProtectionDomain().getCodeSource().getLocation();
    Path source = Path.of(ThriftShadeIT.class.getResource("/ThriftSmokeTest.java").toURI());
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    assertNotNull(compiler, "The integration tests require a JDK");
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    int result =
        compiler.run(
            null,
            errors,
            errors,
            "--release",
            "11",
            "-encoding",
            "UTF-8",
            "-proc:none",
            "-classpath",
            shadedJar + File.pathSeparator + Path.of(slf4j.toURI()),
            "-d",
            classes.toString(),
            source.toString());
    assertEquals(0, result, errors.toString("UTF-8"));
    // Only the packaged JAR and the intentionally external logging API are visible.
    loader =
        new URLClassLoader(
            new URL[] {classes.toUri().toURL(), shadedJar.toUri().toURL(), slf4j},
            ClassLoader.getPlatformClassLoader());
  }

  @AfterAll
  static void closeConsumer() throws Exception {
    if (loader != null) {
      loader.close();
    }
  }

  @Test
  void containsOnlyRelocatedClassesCompatibleWithJava11() throws Exception {
    try (JarFile jar = new JarFile(shadedJar.toFile())) {
      assertNotNull(
          jar.getJarEntry("org/apache/amoro/shade/thrift/org/apache/thrift/TException.class"));
      Enumeration<JarEntry> entries = jar.entries();
      while (entries.hasMoreElements()) {
        JarEntry entry = entries.nextElement();
        String name = entry.getName();
        assertFalse(name.startsWith("META-INF/maven/"), name);
        if (!name.endsWith(".class")) {
          continue;
        }
        assertFalse(
            name.endsWith("module-info.class"),
            "Original module descriptors do not describe the relocated JAR: " + name);
        assertTrue(name.startsWith("org/apache/amoro/shade/thrift/"), name);
        try (DataInputStream input = new DataInputStream(jar.getInputStream(entry))) {
          assertEquals(0xCAFEBABE, input.readInt(), name);
          input.readUnsignedShort();
          assertTrue(input.readUnsignedShort() <= 55, "Requires a newer JDK: " + name);
        }
      }
      assertNotNull(jar.getJarEntry("META-INF/LICENSE"));
      assertNotNull(jar.getJarEntry("META-INF/NOTICE"));
    }
  }

  @Test
  void binaryProtocolRoundTrip() throws Exception {
    runConsumer("binaryProtocol");
  }

  @Test
  void compactProtocolRoundTrip() throws Exception {
    runConsumer("compactProtocol");
  }

  @Test
  void jsonProtocolRoundTrip() throws Exception {
    runConsumer("jsonProtocol");
  }

  @Test
  void framedTransportRoundTrip() throws Exception {
    runConsumer("framedTransport");
  }

  @Test
  void httpClientRoundTrip() throws Exception {
    runConsumer("httpClient");
  }

  @Test
  void servletAndAnnotationDependenciesAreRelocated() throws Exception {
    runConsumer("servletAndAnnotations");
  }

  private static void runConsumer(String method) throws Exception {
    loader.loadClass("ThriftSmokeTest").getMethod(method).invoke(null);
  }
}
