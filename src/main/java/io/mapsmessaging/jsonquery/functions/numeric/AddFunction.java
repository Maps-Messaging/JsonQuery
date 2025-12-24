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

package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.AbstractFunction;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class AddFunction extends AbstractFunction {

  private static boolean isStringy(JsonElement element) {
    if (!element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString();
  }

  private static String asString(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return "null";
    }
    if (element.isJsonPrimitive()) {
      JsonPrimitive primitive = element.getAsJsonPrimitive();
      if (primitive.isString()) {
        return primitive.getAsString();
      }
      if (primitive.isNumber() || primitive.isBoolean()) {
        return primitive.getAsString();
      }
    }
    // objects/arrays: stringify in a stable way
    return element.toString();
  }

  @Override
  public String getName() {
    return "add";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: add(a,b)");
    Function<JsonElement, JsonElement> leftExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> rightExpr = compileArg(rawArgs.get(1), compiler);

    return data -> {
      JsonElement left = leftExpr.apply(data);
      JsonElement right = rightExpr.apply(data);

      if (left == null || left.isJsonNull() || right == null || right.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (isStringy(left) || isStringy(right)) {
        return new JsonPrimitive(asString(left) + asString(right));
      }

      double leftValue = JsonQueryFunction.asNumber(left, "Number expected");
      double rightValue = JsonQueryFunction.asNumber(right, "Number expected");
      return JsonQueryFunction.numberValue(leftValue + rightValue);
    };
  }
}
