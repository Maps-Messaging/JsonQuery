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
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MapObjectFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "mapObject";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCountExact(rawArgs, 1, "1 argument (mapper expression)");
    Function<JsonElement, JsonElement> mapper = compileArg(rawArgs.get(0), compiler);

    return data -> mapObject(data, mapper);
  }

  private static JsonElement mapObject(
      JsonElement data,
      Function<JsonElement, JsonElement> mapper) {

    if (data == null || data.isJsonNull() || !data.isJsonObject()) {
      return JsonNull.INSTANCE;
    }

    JsonObject output = new JsonObject();
    for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
      addMappedEntry(output, entry, mapper);
    }
    return output;
  }

  private static void addMappedEntry(
      JsonObject output,
      Map.Entry<String, JsonElement> entry,
      Function<JsonElement, JsonElement> mapper) {

    JsonElement mapped = mapper.apply(pair(entry));
    if (mapped == null || !mapped.isJsonObject()) {
      return;
    }

    JsonObject mappedObject = mapped.getAsJsonObject();
    String key = stringValue(mappedObject.get("key"));
    if (key == null) {
      return;
    }

    JsonElement value = mappedObject.get("value");
    output.add(key, value == null ? JsonNull.INSTANCE : value);
  }

  private static JsonObject pair(Map.Entry<String, JsonElement> entry) {
    JsonObject pair = new JsonObject();
    pair.addProperty("key", entry.getKey());
    JsonElement value = entry.getValue();
    pair.add("value", value == null ? JsonNull.INSTANCE : value);
    return pair;
  }

  private static String stringValue(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString() ? primitive.getAsString() : null;
  }
}
