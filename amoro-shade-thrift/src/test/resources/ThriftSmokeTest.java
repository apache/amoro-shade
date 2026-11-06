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
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.apache.amoro.shade.thrift.jakarta.annotation.PostConstruct;
import org.apache.amoro.shade.thrift.jakarta.servlet.http.HttpServlet;
import org.apache.amoro.shade.thrift.org.apache.commons.lang3.Validate;
import org.apache.amoro.shade.thrift.org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.amoro.shade.thrift.org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TCompactProtocol;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TField;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TJSONProtocol;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TProtocol;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TProtocolFactory;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TStruct;
import org.apache.amoro.shade.thrift.org.apache.thrift.protocol.TType;
import org.apache.amoro.shade.thrift.org.apache.thrift.server.TServlet;
import org.apache.amoro.shade.thrift.org.apache.thrift.transport.THttpClient;
import org.apache.amoro.shade.thrift.org.apache.thrift.transport.TMemoryBuffer;
import org.apache.amoro.shade.thrift.org.apache.thrift.transport.layered.TFramedTransport;

/** Compiled against the final shaded JAR, without the original dependency classes. */
public class ThriftSmokeTest {
  public static void binaryProtocol() throws Exception {
    protocolRoundTrip(new TBinaryProtocol.Factory());
  }

  public static void compactProtocol() throws Exception {
    protocolRoundTrip(new TCompactProtocol.Factory());
  }

  public static void jsonProtocol() throws Exception {
    protocolRoundTrip(new TJSONProtocol.Factory());
  }

  private static void protocolRoundTrip(TProtocolFactory factory) throws Exception {
    TMemoryBuffer memory = new TMemoryBuffer(256);
    TProtocol writer = factory.getProtocol(memory);
    writer.writeStructBegin(new TStruct("SmokeTest"));
    writer.writeFieldBegin(new TField("message", TType.STRING, (short) 1));
    writer.writeString("Amoro Thrift 中文");
    writer.writeFieldEnd();
    writer.writeFieldBegin(new TField("value", TType.I32, (short) 2));
    writer.writeI32(250);
    writer.writeFieldEnd();
    writer.writeFieldStop();
    writer.writeStructEnd();

    TProtocol reader = factory.getProtocol(memory);
    reader.readStructBegin();
    TField message = reader.readFieldBegin();
    check(message.id == 1 && message.type == TType.STRING, "String field header");
    check("Amoro Thrift 中文".equals(reader.readString()), "UTF-8 round trip");
    reader.readFieldEnd();
    TField value = reader.readFieldBegin();
    check(value.id == 2 && value.type == TType.I32, "Integer field header");
    check(reader.readI32() == 250, "Integer round trip");
    reader.readFieldEnd();
    check(reader.readFieldBegin().type == TType.STOP, "End of struct");
    reader.readStructEnd();
  }

  public static void framedTransport() throws Exception {
    TMemoryBuffer memory = new TMemoryBuffer(256);
    TFramedTransport transport = new TFramedTransport(memory);
    // Two consecutive frames exercise the frame/message accounting changed in 0.25.0.
    for (String message : new String[] {"first frame", "second frame 中文"}) {
      byte[] expected = message.getBytes(StandardCharsets.UTF_8);
      transport.write(expected);
      transport.flush();
      byte[] actual = new byte[expected.length];
      transport.readAll(actual, 0, actual.length);
      check(Arrays.equals(expected, actual), "Framed transport round trip");
    }
    transport.close();
  }

  public static void httpClient() throws Exception {
    HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/thrift",
        exchange -> {
          try {
            byte[] request = exchange.getRequestBody().readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", "application/x-thrift");
            exchange.sendResponseHeaders(200, request.length);
            exchange.getResponseBody().write(request);
          } finally {
            exchange.close();
          }
        });
    server.start();
    try (CloseableHttpClient http = HttpClients.createDefault()) {
      String url = "http://127.0.0.1:" + server.getAddress().getPort() + "/thrift";
      // Cover both the JDK URLConnection path and the relocated HttpComponents path.
      for (THttpClient client :
          new THttpClient[] {new THttpClient(url), new THttpClient(url, http)}) {
        try {
          client.setConnectTimeout(5000);
          client.setReadTimeout(5000);
          byte[] expected = "Amoro HTTP 中文".getBytes(StandardCharsets.UTF_8);
          client.write(expected);
          client.flush();
          byte[] actual = new byte[expected.length];
          client.readAll(actual, 0, actual.length);
          check(Arrays.equals(expected, actual), "HTTP transport round trip");
        } finally {
          client.close();
        }
      }
    } finally {
      server.stop(0);
    }
  }

  public static void servletAndAnnotations() {
    check(TServlet.class.getSuperclass() == HttpServlet.class, "Relocated servlet superclass");
    check(PostConstruct.class.isAnnotation(), "Relocated annotation API");
    Validate.isTrue(true, "Relocated Commons Lang");
  }

  private static void check(boolean condition, String message) {
    if (!condition) {
      throw new AssertionError(message);
    }
  }
}
