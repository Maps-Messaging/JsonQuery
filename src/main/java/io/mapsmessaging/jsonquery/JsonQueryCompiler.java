/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.functions.FunctionRegistry;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class JsonQueryCompiler {

  private final FunctionRegistry functionRegistry;

  public JsonQueryCompiler(FunctionRegistry functionRegistry) {
    if (functionRegistry == null) {
      throw new IllegalArgumentException("functionRegistry cannot be null");
    }
    this.functionRegistry = functionRegistry;
  }

  public static JsonQueryCompiler createDefault() {
    return new JsonQueryCompiler(FunctionRegistry.builtIns());
  }

  public static JsonQueryCompiler create(FunctionRegistry builtIns, FunctionRegistry custom) {
    return new JsonQueryCompiler(FunctionRegistry.merge(builtIns, custom));
  }

  private static boolean isString(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return false;
    }
    if (!element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString();
  }

  public Function<JsonElement, JsonElement> compile(JsonElement query) {
    if (query == null || query.isJsonNull()) {
      return ignored -> JsonNull.INSTANCE;
    }

    if (query.isJsonArray()) {
      JsonArray array = query.getAsJsonArray();
      if (array.isEmpty()) {
        throw new IllegalArgumentException("Query array cannot be empty");
      }

      JsonElement fnNameElement = array.get(0);
      if (!isString(fnNameElement)) {
        throw new IllegalArgumentException("First element of query array must be a function name string");
      }

      String functionName = fnNameElement.getAsString();
      JsonQueryFunction function = functionRegistry.get(functionName);
      if (function == null) {
        throw new IllegalArgumentException("Unknown function \"" + functionName + "\"");
      }

      List<JsonElement> rawArgs = new ArrayList<>();
      for (int i = 1; i < array.size(); i++) {
        rawArgs.add(array.get(i));
      }

      return function.compile(rawArgs, this);
    }

    if (query.isJsonObject()) {
      throw new IllegalArgumentException("Function notation [\"object\", {...}] expected but got " + query);
    }

    JsonElement staticValue = query;
    return ignored -> staticValue;
  }
}
