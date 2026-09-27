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

package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static io.mapsmessaging.jsonquery.functions.JsonQueryGson.isString;
import static io.mapsmessaging.jsonquery.functions.binary.AbstractBinaryPredicateFunction.isBoolean;
import static io.mapsmessaging.jsonquery.functions.binary.AbstractBinaryPredicateFunction.isNumber;

public final class NeFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "ne";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (rawArgs.size() != 2) {
      throw new IllegalArgumentException("ne expects 2 arguments");
    }

    Function<JsonElement, JsonElement> leftExpression = compiler.compile(rawArgs.get(0));
    Function<JsonElement, JsonElement> rightExpression = compiler.compile(rawArgs.get(1));

    return data -> new JsonPrimitive(
        !deepEquals(leftExpression.apply(data), rightExpression.apply(data)));
  }

  private static boolean deepEquals(JsonElement left, JsonElement right) {
    if (isNull(left)) {
      return isNull(right);
    }
    if (isNull(right)) {
      return false;
    }
    if (left.isJsonPrimitive() && right.isJsonPrimitive()) {
      return primitiveEquals(left, right);
    }
    if (left.isJsonArray() && right.isJsonArray()) {
      return arrayEquals(left.getAsJsonArray(), right.getAsJsonArray());
    }
    if (left.isJsonObject() && right.isJsonObject()) {
      return objectEquals(left.getAsJsonObject(), right.getAsJsonObject());
    }
    return false;
  }

  private static boolean primitiveEquals(JsonElement left, JsonElement right) {
    boolean comparable =
        (isNumber(left) && isNumber(right))
            || (isString(left) && isString(right))
            || (isBoolean(left) && isBoolean(right));

    return comparable && AbstractBinaryPredicateFunction.compare(left, right) == 0;
  }

  private static boolean arrayEquals(JsonArray left, JsonArray right) {
    if (left.size() != right.size()) {
      return false;
    }
    for (int index = 0; index < left.size(); index++) {
      if (!deepEquals(left.get(index), right.get(index))) {
        return false;
      }
    }
    return true;
  }

  private static boolean objectEquals(JsonObject left, JsonObject right) {
    if (left.size() != right.size()) {
      return false;
    }
    for (Map.Entry<String, JsonElement> entry : left.entrySet()) {
      if (!right.has(entry.getKey())
          || !deepEquals(entry.getValue(), right.get(entry.getKey()))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isNull(JsonElement element) {
    return element == null || element.isJsonNull();
  }
}
