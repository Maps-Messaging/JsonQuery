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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class FilterFunction implements JsonQueryFunction {

  private static boolean isTruthy(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return false;
    }

    if (value.isJsonPrimitive()) {
      JsonPrimitive primitive = value.getAsJsonPrimitive();

      if (primitive.isBoolean()) {
        return primitive.getAsBoolean();
      }

      if (primitive.isNumber()) {
        return primitive.getAsDouble() != 0.0d;
      }

      return true;
    }

    return true;
  }

  @Override
  public String getName() {
    return "filter";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("filter expects 1 argument");
    }

    JsonElement predicateExpression = rawArgs.get(0);
    Function<JsonElement, JsonElement> predicate = compiler.compile(predicateExpression);

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (!data.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray inputArray = data.getAsJsonArray();
      JsonArray outputArray = new JsonArray();

      for (int index = 0; index < inputArray.size(); index++) {
        JsonElement element = inputArray.get(index);
        JsonElement predicateResult = predicate.apply(element);

        if (isTruthy(predicateResult)) {
          outputArray.add(element);
        }
      }

      return outputArray;
    };
  }
}
