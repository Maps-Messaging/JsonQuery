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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.stringify.JsonQueryStringifier;
import io.mapsmessaging.jsonquery.stringify.Options;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class JsonQueryStringifyConformanceTest {

  private static final String RESOURCE = "jsonquery/stringify.test.json";
  private static final Gson GSON = new Gson();

  @TestFactory
  List<DynamicTest> stringify_suite() {
    JsonObject suite = loadSuiteJson();
    JsonArray groups = suite.getAsJsonArray("groups");
    if (groups == null) {
      fail("No 'groups' in suite");
    }

    List<DynamicTest> tests = new ArrayList<>();

    for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
      JsonObject group = groups.get(groupIndex).getAsJsonObject();

      String category = getString(group, "category", "unknown");
      String description = getString(group, "description", "no description");

      Options options = readOptions(group.get("options"));

      JsonArray groupTests = group.getAsJsonArray("tests");
      if (groupTests == null) {
        continue;
      }

      for (int testIndex = 0; testIndex < groupTests.size(); testIndex++) {
        JsonObject test = groupTests.get(testIndex).getAsJsonObject();

        JsonElement inputAst = test.get("input");
        String expectedOutput = getString(test, "output", null);
        String expectedThrows = getString(test, "throws", null);

        String displayName = String.format(
            "%03d.%03d [%s] %s",
            groupIndex,
            testIndex,
            category,
            description
        );

        tests.add(DynamicTest.dynamicTest(displayName, () -> runOne(inputAst, expectedOutput, expectedThrows, options)));
      }
    }

    return tests;
  }

  private void runOne(JsonElement inputAst, String expectedOutput, String expectedThrows, Options options) {
    assertNotNull(inputAst, "Test case missing 'input'");

    if (expectedThrows != null) {
      RuntimeException ex = assertThrows(RuntimeException.class, () -> { new JsonQueryStringifier().stringify(inputAst); });
      assertEquals(expectedThrows, ex.getMessage());
      return;
    }

    assertNotNull(expectedOutput, "Test case missing 'output'");

    String actual = new JsonQueryStringifier().stringify(inputAst);
    if(!expectedOutput.equals(actual)){
      System.out.println("Input"+inputAst);
      System.out.println("Expected:"+expectedOutput);
      System.out.println("Actual:"+actual);
    }
    assertEquals(expectedOutput, actual);
  }

  private JsonObject loadSuiteJson() {
    InputStream stream = Thread.currentThread().getContextClassLoader().getResourceAsStream(RESOURCE);
    if (stream == null) {
      fail("Resource not found on classpath: " + RESOURCE);
    }

    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      JsonElement parsed = GSON.fromJson(reader, JsonElement.class);
      if (parsed == null || !parsed.isJsonObject()) {
        fail("Suite JSON is not an object: " + RESOURCE);
      }
      return parsed.getAsJsonObject();
    } catch (Exception e) {
      fail("Failed to read suite JSON: " + RESOURCE + " error=" + e.getMessage());
      return null;
    }
  }

  private Options readOptions(JsonElement optionsEl) {
    if (optionsEl == null || optionsEl.isJsonNull()) {
      return new Options();
    }
    if (!optionsEl.isJsonObject()) {
      return new Options();
    }

    JsonObject obj = optionsEl.getAsJsonObject();

    String indentation = getString(obj, "indentation", "  ");
    int maxLineLength = getInt(obj, "maxLineLength", 120);

    return new Options(indentation, maxLineLength);
  }

  private static String getString(JsonObject obj, String key, String defaultValue) {
    JsonElement el = obj.get(key);
    if (el == null || el.isJsonNull()) {
      return defaultValue;
    }
    if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
      return defaultValue;
    }
    return el.getAsString();
  }

  private static int getInt(JsonObject obj, String key, int defaultValue) {
    JsonElement el = obj.get(key);
    if (el == null || el.isJsonNull()) {
      return defaultValue;
    }
    if (!el.isJsonPrimitive() || !el.getAsJsonPrimitive().isNumber()) {
      return defaultValue;
    }
    return el.getAsInt();
  }
}
