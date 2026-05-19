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

package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class ProdFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "prod";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("prod expects 0 arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray array = data.getAsJsonArray();
      if (array.isEmpty()) {
        return JsonNull.INSTANCE;
      }

      double product = 1.0;
      boolean seenNumber = false;

      for (JsonElement element : array) {
        if (element == null || element.isJsonNull()) {
          continue;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
          throw new IllegalArgumentException("Number expected");
        }
        product *= element.getAsDouble();
        seenNumber = true;
      }

      if (!seenNumber) {
        return JsonNull.INSTANCE;
      }

      if (product == Math.rint(product)) {
        return new JsonPrimitive((long) product);
      }
      return new JsonPrimitive(product);
    };
  }
}
