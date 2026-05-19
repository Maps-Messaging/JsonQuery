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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class UniqByFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "uniqBy";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("uniqBy expects 1 argument (keySelector)");
    }

    Function<JsonElement, JsonElement> keySelector = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray input = data.getAsJsonArray();
      JsonArray result = new JsonArray();
      Set<JsonElement> seenKeys = new LinkedHashSet<>();

      for (JsonElement element : input) {
        JsonElement key = keySelector.apply(element);
        if (key == null || key.isJsonNull()) {
          continue;
        }
        if (seenKeys.add(key)) {
          result.add(element);
        }
      }

      return result;
    };
  }
}
