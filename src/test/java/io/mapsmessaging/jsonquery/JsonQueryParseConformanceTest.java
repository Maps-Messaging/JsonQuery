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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class JsonQueryParseConformanceTest {

  private static final String SUITE_RESOURCE = "jsonquery/parse.test.json";

  /**
   * Wire this to YOUR parser.
   * <p>
   * Must return the AST as a Gson JsonElement, matching the suite format:
   * ["get","name"], ["pipe", ...], literals, etc.
   */
  private static JsonElement parseToAst(String input) {
    try {
      return JsonQueryParser.parse(input);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static JsonObject loadSuiteJson() {
    InputStream stream = JsonQueryParseConformanceTest.class.getClassLoader().getResourceAsStream(SUITE_RESOURCE);
    if (stream == null) {
      throw new IllegalStateException("Resource not found: " + SUITE_RESOURCE);
    }

    try (Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
      JsonElement root = JsonParser.parseReader(reader);
      if (!root.isJsonObject()) {
        throw new IllegalStateException("Suite root must be a JSON object");
      }
      return root.getAsJsonObject();
    } catch (Exception e) {
      throw new IllegalStateException("Failed to load suite: " + SUITE_RESOURCE, e);
    }
  }

  private static String getString(JsonObject obj, String key) {
    JsonElement el = obj.get(key);
    if (el == null || !el.isJsonPrimitive() || !el.getAsJsonPrimitive().isString()) {
      throw new IllegalStateException("Expected string field '" + key + "' in: " + obj);
    }
    return el.getAsString();
  }

  @TestFactory
  List<DynamicTest> parseSuite() {
    JsonObject suite = loadSuiteJson();
    List<DynamicTest> tests = new ArrayList<>();

    for (JsonElement groupElement : suite.getAsJsonArray("groups")) {
      JsonObject group = groupElement.getAsJsonObject();
      String category = getString(group, "category");
      String description = getString(group, "description");

      for (JsonElement testElement : group.getAsJsonArray("tests")) {
        JsonObject test = testElement.getAsJsonObject();

        String input = getString(test, "input");
        String displayName = category + " :: " + description + " :: " + input;

        if (test.has("output")) {
          JsonElement expectedAst = test.get("output");
          tests.add(DynamicTest.dynamicTest(displayName, () -> {
            JsonElement actualAst = parseToAst(input);
            assertEquals(expectedAst, actualAst, () ->
                "AST mismatch\ninput: " + input + "\nexpected: " + expectedAst + "\nactual: " + actualAst
            );
          }));
        } else if (test.has("throws")) {
          String expectedMessage = getString(test, "throws");
          tests.add(DynamicTest.dynamicTest(displayName, () -> {
            Exception ex = assertThrows(Exception.class, () -> parseToAst(input));
            String msg = ex.getCause().getMessage();
            assertEquals(expectedMessage, msg, () ->
                "Exception message mismatch\ninput: " + input + "\nexpected: " + expectedMessage + "\nactual: " + ex.getMessage()
            );
          }));
        } else {
          tests.add(DynamicTest.dynamicTest(displayName, () -> fail("Test case missing 'output' or 'throws'")));
        }
      }
    }

    assertFalse(tests.isEmpty(), "No tests loaded from parse suite");
    return tests;
  }
}
