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

package io.mapsmessaging.jsonquery.functions;


import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public interface JsonQueryFunction {

  static Function<JsonElement, JsonElement> compileArg(JsonElement rawArg, JsonQueryCompiler compiler) {
    if (rawArg == null || rawArg.isJsonNull()) {
      return input -> JsonNull.INSTANCE;
    }
    if (rawArg.isJsonArray()) {
      return compiler.compile(rawArg.getAsJsonArray());
    }
    return input -> rawArg;
  }

  static JsonNull nullValue() {
    return JsonNull.INSTANCE;
  }

  static boolean isNull(JsonElement element) {
    return element == null || element.isJsonNull();
  }

  static boolean isTruthy(JsonElement element) {
    if (isNull(element)) {
      return false;
    }
    if (!element.isJsonPrimitive()) {
      return true;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isBoolean()) {
      return primitive.getAsBoolean();
    }
    if (primitive.isNumber()) {
      return primitive.getAsDouble() != 0.0;
    }
    if (primitive.isString()) {
      return !primitive.getAsString().isEmpty();
    }
    return true;
  }

  static JsonElement booleanValue(boolean value) {
    return new JsonPrimitive(value);
  }

  static boolean isIntegral(double value) {
    return value == Math.rint(value);
  }

  static JsonElement numberValue(double value) {
    if (isIntegral(value)) {
      return new JsonPrimitive((long) value);
    }
    return new JsonPrimitive(value);
  }

  static String asString(JsonElement element, String errorMessage) {
    if (isNull(element)) {
      throw new IllegalArgumentException(errorMessage);
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return element.getAsString();
  }

  static double asNumber(JsonElement element, String errorMessage) {
    if (isNull(element)) {
      throw new IllegalArgumentException(errorMessage);
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      throw new IllegalArgumentException(errorMessage);
    }
    return element.getAsDouble();
  }

  static int asInt(JsonElement element, String errorMessage) {
    double value = asNumber(element, errorMessage);
    return (int) value;
  }

  String getName();

  Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler
  );


}
