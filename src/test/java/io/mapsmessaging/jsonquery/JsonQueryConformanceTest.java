/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.jsonquery;

import com.google.gson.*;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

public class JsonQueryConformanceTest {

  private static final Gson GSON = new GsonBuilder().serializeNulls().create();
  /**
   * Wire your engine here.
   */
  private final JsonQueryEngine engine = (input, query) -> {
    try {
      JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
      Function<JsonElement, JsonElement> compiled = compiler.compile(query);
      return compiled.apply(input);
    } catch (Throwable t) {
      throw t;
    }
  };

  private static JsonObject loadSuiteJson(String resourcePath) {
    InputStream stream = JsonQueryConformanceTest.class.getResourceAsStream(resourcePath);
    assertNotNull(stream, "Missing test suite resource: " + resourcePath);

    InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
    JsonElement element = GSON.fromJson(reader, JsonElement.class);
    assertNotNull(element);
    assertTrue(element.isJsonObject());

    return element.getAsJsonObject();
  }

  private static String getString(JsonObject object, String field, String defaultValue) {
    if (!object.has(field) || object.get(field).isJsonNull()) {
      return defaultValue;
    }
    return object.get(field).getAsString();
  }

  private static JsonElement normalizeNull(JsonElement element) {
    return element == null ? JsonNull.INSTANCE : element;
  }

  /**
   * Deep JSON equality with numeric tolerance.
   * - Objects: compare by keys recursively
   * - Arrays: compare by index recursively
   * - Numbers: compare as doubles within epsilon (also handles int/float mismatches)
   */
  private static void assertJsonEquals(JsonElement expected, JsonElement actual, double epsilon) {
    if (expected == actual) {
      return;
    }
    if (expected == null || expected.isJsonNull()) {
      assertTrue(actual == null || actual.isJsonNull(), "Expected null but got: " + actual);
      return;
    }
    assertNotNull(actual, "Expected: " + expected + " but got null");

    if (expected.isJsonPrimitive() && actual.isJsonPrimitive()) {
      var expectedPrimitive = expected.getAsJsonPrimitive();
      var actualPrimitive = actual.getAsJsonPrimitive();

      if (expectedPrimitive.isNumber() && actualPrimitive.isNumber()) {
        double expectedNumber = expectedPrimitive.getAsDouble();
        double actualNumber = actualPrimitive.getAsDouble();
        double delta = Math.abs(expectedNumber - actualNumber);
        assertTrue(
            delta <= epsilon,
            "Number mismatch. expected=" + expectedNumber + " actual=" + actualNumber + " delta=" + delta);
        return;
      }

      assertEquals(expectedPrimitive, actualPrimitive, "Primitive mismatch");
      return;
    }

    if (expected.isJsonArray() && actual.isJsonArray()) {
      JsonArray expectedArray = expected.getAsJsonArray();
      JsonArray actualArray = actual.getAsJsonArray();

      assertEquals(expectedArray.size(), actualArray.size(), "Array size mismatch");
      for (int i = 0; i < expectedArray.size(); i++) {
        assertJsonEquals(expectedArray.get(i), actualArray.get(i), epsilon);
      }
      return;
    }

    if (expected.isJsonObject() && actual.isJsonObject()) {
      JsonObject expectedObject = expected.getAsJsonObject();
      JsonObject actualObject = actual.getAsJsonObject();

      assertEquals(expectedObject.size(), actualObject.size(), "Object field count mismatch");

      for (String key : expectedObject.keySet()) {
        assertTrue(actualObject.has(key), "Missing key: " + key);
        assertJsonEquals(expectedObject.get(key), actualObject.get(key), epsilon);
      }
      return;
    }

    fail("Type mismatch. expected=" + expected + " actual=" + actual);
  }

  @TestFactory
  public List<DynamicTest> conformanceSuite() {
    JsonObject suite = loadSuiteJson("/jsonquery/compile.test.json");

    String version = getString(suite, "version", "unknown");
    JsonArray groups = suite.getAsJsonArray("groups");

    List<DynamicTest> tests = new ArrayList<>();
    for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
      JsonObject group = groups.get(groupIndex).getAsJsonObject();

      String category = getString(group, "category", "unknown");
      String description = getString(group, "description", "");
      JsonArray groupTests = group.getAsJsonArray("tests");

      for (int testIndex = 0; testIndex < groupTests.size(); testIndex++) {
        JsonObject test = groupTests.get(testIndex).getAsJsonObject();
        String name = "v" + version + " :: " + category + " :: " + description + " :: #" + (testIndex + 1);
        tests.add(DynamicTest.dynamicTest(name, () -> runOne(test)));
      }
    }
    return tests;
  }

  private void runOne(JsonObject test) throws Exception {
    JsonElement input = test.has("input") ? test.get("input") : JsonNull.INSTANCE;
    JsonElement query = test.has("query") ? test.get("query") : JsonNull.INSTANCE;

    if (test.has("throws")) {
      String expected = test.get("throws").getAsString();
      Exception exception = assertThrows(Exception.class, () -> engine.execute(input, query));
      String message = exception.getMessage() == null ? "" : exception.getMessage();
      assertTrue(
          message.contains(expected),
          "Expected error to contain: [" + expected + "] but was: [" + message + "]");
      return;
    }

    assertTrue(test.has("output"), "Test missing both 'output' and 'throws'");
    JsonElement expectedOutput = normalizeNull(test.get("output"));
    JsonElement actual = engine.execute(input, query);
    JsonElement actualNormalized = normalizeNull(actual);
    assertJsonEquals(expectedOutput, actualNormalized, 1e-9);
  }

  /**
   * Implement this with your existing compiler/evaluator.
   * It should execute the JSON "query" against "input" and return a JSON result.
   */
  @FunctionalInterface
  public interface JsonQueryEngine {
    JsonElement execute(JsonElement input, JsonElement query) throws Exception;
  }
}

