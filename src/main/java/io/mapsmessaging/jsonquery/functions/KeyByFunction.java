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

import com.google.gson.*;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class KeyByFunction implements JsonQueryFunction {

  private static String toKey(JsonElement keyValue) {
    if (keyValue == null || keyValue.isJsonNull()) {
      return null;
    }
    if (!keyValue.isJsonPrimitive()) {
      return null;
    }

    JsonPrimitive primitive = keyValue.getAsJsonPrimitive();
    if (primitive.isString()) {
      return primitive.getAsString();
    }
    if (primitive.isNumber()) {
      return primitive.getAsNumber().toString();
    }
    if (primitive.isBoolean()) {
      return Boolean.toString(primitive.getAsBoolean());
    }
    return null;
  }

  @Override
  public String getName() {
    return "keyBy";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("keyBy expects 1 argument: keyBy(keySelector)");
    }

    Function<JsonElement, JsonElement> keySelector = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonObject keyed = new JsonObject();
      JsonArray input = data.getAsJsonArray();

      for (JsonElement element : input) {
        JsonElement keyValue = keySelector.apply(element);
        String key = toKey(keyValue);
        if (key == null) {
          continue;
        }
        if (keyed.has(key)) {
          continue; // first wins
        }
        keyed.add(key, element);
      }


      return keyed;
    };
  }
}
