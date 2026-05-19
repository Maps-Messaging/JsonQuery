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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MapObjectFunction implements JsonQueryFunction {
  @Override
  public String getName() {
    return "mapObject";
  }


  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("mapObject expects 1 argument (mapper expression)");
    }

    Function<JsonElement, JsonElement> mapper = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonObject()) {
        return JsonNull.INSTANCE;
      }

      JsonObject input = data.getAsJsonObject();
      JsonObject output = new JsonObject();

      for (Map.Entry<String, JsonElement> entry : input.entrySet()) {
        JsonObject pair = new JsonObject();
        pair.addProperty("key", entry.getKey());
        pair.add("value", entry.getValue() == null ? JsonNull.INSTANCE : entry.getValue());

        JsonElement mapped = mapper.apply(pair);
        if (mapped == null || mapped.isJsonNull() || !mapped.isJsonObject()) {
          continue;
        }

        JsonObject mappedObject = mapped.getAsJsonObject();

        JsonElement newKeyElement = mappedObject.get("key");
        if (newKeyElement == null || newKeyElement.isJsonNull() || !newKeyElement.isJsonPrimitive()
            || !newKeyElement.getAsJsonPrimitive().isString()) {
          continue;
        }
        String newKey = newKeyElement.getAsString();

        JsonElement newValueElement = mappedObject.get("value");
        if (newValueElement == null) {
          newValueElement = JsonNull.INSTANCE;
        }

        output.add(newKey, newValueElement);
      }

      return output;
    };
  }
}
