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

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

import static io.mapsmessaging.jsonquery.functions.JsonQueryGson.isString;
import static io.mapsmessaging.jsonquery.functions.binary.AbstractBinaryPredicateFunction.isBoolean;
import static io.mapsmessaging.jsonquery.functions.binary.AbstractBinaryPredicateFunction.isNumber;

public final class NeFunction implements JsonQueryFunction {
  private static boolean deepEquals(JsonElement left, JsonElement right) {
    if (left == null || left.isJsonNull()) {
      return right == null || right.isJsonNull();
    }
    if (right == null || right.isJsonNull()) {
      return false;
    }

    if (left.isJsonPrimitive() && right.isJsonPrimitive()) {
      if ((isNumber(left) && isNumber(right))
          || (isString(left) && isString(right))
          || (isBoolean(left) && isBoolean(right))) {
        return AbstractBinaryPredicateFunction.compare(left, right) == 0;
      }
      return false;
    }

    if (left.isJsonArray() && right.isJsonArray()) {
      var la = left.getAsJsonArray();
      var ra = right.getAsJsonArray();
      if (la.size() != ra.size()) {
        return false;
      }
      for (int i = 0; i < la.size(); i++) {
        if (!deepEquals(la.get(i), ra.get(i))) {
          return false;
        }
      }
      return true;
    }

    if (left.isJsonObject() && right.isJsonObject()) {
      var lo = left.getAsJsonObject();
      var ro = right.getAsJsonObject();
      if (lo.size() != ro.size()) {
        return false;
      }
      for (var entry : lo.entrySet()) {
        String key = entry.getKey();
        if (!ro.has(key)) {
          return false;
        }
        if (!deepEquals(entry.getValue(), ro.get(key))) {
          return false;
        }
      }
      return true;
    }

    return false;
  }

  @Override
  public String getName() {
    return "ne";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 2) {
      throw new IllegalArgumentException("ne expects 2 arguments");
    }

    Function<JsonElement, JsonElement> leftExpression = compiler.compile(rawArgs.get(0));
    Function<JsonElement, JsonElement> rightExpression = compiler.compile(rawArgs.get(1));

    return data -> {
      JsonElement leftValue = leftExpression.apply(data);
      JsonElement rightValue = rightExpression.apply(data);

      boolean leftIsNull = (leftValue == null || leftValue.isJsonNull());
      boolean rightIsNull = (rightValue == null || rightValue.isJsonNull());

      // ne(null, null) = false
      if (leftIsNull && rightIsNull) {
        return new JsonPrimitive(false);
      }

      // ne(null, x) = true, ne(x, null) = true
      if (leftIsNull || rightIsNull) {
        return new JsonPrimitive(true);
      }

      // Same-type primitives: normal inequality
      if (isNumber(leftValue) && isNumber(rightValue)) {
        return new JsonPrimitive(Double.compare(leftValue.getAsDouble(), rightValue.getAsDouble()) != 0);
      }
      if (isString(leftValue) && isString(rightValue)) {
        return new JsonPrimitive(!leftValue.getAsString().equals(rightValue.getAsString()));
      }
      if (isBoolean(leftValue) && isBoolean(rightValue)) {
        return new JsonPrimitive(leftValue.getAsBoolean() != rightValue.getAsBoolean());
      }

      // For arrays/objects (and any other non-primitive combos):
      return new JsonPrimitive(!deepEquals(leftValue, rightValue));

    };
  }

}
