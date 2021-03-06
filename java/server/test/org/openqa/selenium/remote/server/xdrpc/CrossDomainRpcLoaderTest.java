// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package org.openqa.selenium.remote.server.xdrpc;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.openqa.selenium.json.Json.MAP_TYPE;

import com.google.common.collect.ImmutableMap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.json.Json;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;

public class CrossDomainRpcLoaderTest {

  private HttpServletRequest mockRequest;

  @Before
  public void setUp() {
    mockRequest = mock(HttpServletRequest.class);
  }

  @Test
  public void jsonRequestMustHaveAMethod() throws IOException {
    HttpServletRequest mockRequest = createJsonRequest(null, "/", "data");
    try {
      new CrossDomainRpcLoader().loadRpc(mockRequest);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void jsonRequestMustHaveAPath() throws IOException {
    HttpServletRequest mockRequest = createJsonRequest("GET", null, "data");
    try {
      new CrossDomainRpcLoader().loadRpc(mockRequest);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void jsonRequestMustHaveAData() throws IOException {
    HttpServletRequest mockRequest = createJsonRequest("GET", "/", null);
    try {
      new CrossDomainRpcLoader().loadRpc(mockRequest);
      fail();
    } catch (IllegalArgumentException expected) {
    }
  }

  @Test
  public void rpcRequestDataInitializedWithDataAsAString() throws IOException {
    JsonObject json = new JsonObject();
    json.addProperty("foo", "bar");
    HttpServletRequest mockRequest = createJsonRequest("GET", "/", json);

    CrossDomainRpc rpc = new CrossDomainRpcLoader().loadRpc(mockRequest);

    Object data = new Json().toType(rpc.getData(), MAP_TYPE);

    assertEquals(ImmutableMap.of("foo", "bar"), data);
  }

  private HttpServletRequest createJsonRequest(final String method,
      final String path, final Object data) throws IOException {
    when(mockRequest.getHeader("content-type")).thenReturn("application/json");

    JsonObject json = new JsonObject();
    json.addProperty("method", method);
    json.addProperty("path", path);
    if (data instanceof JsonElement) {
      json.add("data", (JsonElement) data);
    } else {
      json.addProperty("data", (String) data);
    }
    final ByteArrayInputStream stream = new ByteArrayInputStream(json.toString().getBytes(UTF_8));
    when(mockRequest.getInputStream()).thenReturn(new ServletInputStream() {
      @Override
      public int read() {
        return stream.read();
      }

      @Override
      public boolean isFinished() {
        return false;
      }

      @Override
      public boolean isReady() {
        return true;
      }

      @Override
      public void setReadListener(ReadListener readListener) {
        throw new UnsupportedOperationException();
      }
    });

    return mockRequest;
  }
}
