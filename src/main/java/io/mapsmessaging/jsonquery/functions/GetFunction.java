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

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.*;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public final class GetFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "get";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      return data -> data == null ? JsonNull.INSTANCE : data;
    }

    for (JsonElement rawArg : rawArgs) {
      if (rawArg == null || rawArg.isJsonNull() || !rawArg.isJsonPrimitive()) {
        throw new IllegalArgumentException("get expects path segments of type string or number");
      }

      JsonPrimitive primitive = rawArg.getAsJsonPrimitive();
      if (!primitive.isString() && !primitive.isNumber()) {
        throw new IllegalArgumentException("get expects path segments of type string or number");
      }

      if (primitive.isNumber()) {
        BigDecimal bigDecimal = primitive.getAsBigDecimal();
        if (bigDecimal.scale() > 0) {
          throw new IllegalArgumentException("get expects an integer array index");
        }
        if (bigDecimal.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0
            || bigDecimal.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
          throw new IllegalArgumentException("get array index out of int range");
        }
      }
    }

    return data -> {
      JsonElement current = data == null ? JsonNull.INSTANCE : data;

      for (JsonElement rawArg : rawArgs) {
        if (current == null || current.isJsonNull()) {
          return JsonNull.INSTANCE;
        }

        JsonPrimitive primitive = rawArg.getAsJsonPrimitive();

        if (primitive.isString()) {
          if (!current.isJsonObject()) {
            return JsonNull.INSTANCE;
          }

          JsonObject object = current.getAsJsonObject();
          JsonElement next = object.get(primitive.getAsString());
          current = next == null ? JsonNull.INSTANCE : next;
          continue;
        }

        int index = primitive.getAsInt();
        if (!current.isJsonArray()) {
          return JsonNull.INSTANCE;
        }

        JsonArray array = current.getAsJsonArray();
        if (index < 0 || index >= array.size()) {
          return JsonNull.INSTANCE;
        }

        JsonElement next = array.get(index);
        current = next == null ? JsonNull.INSTANCE : next;
      }

      return current == null ? JsonNull.INSTANCE : current;
    };
  }
}
