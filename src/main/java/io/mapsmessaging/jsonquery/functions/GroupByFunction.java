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
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class GroupByFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "groupBy";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCountExact(rawArgs, 1, "1 argument: groupBy(keySelector)");
    Function<JsonElement, JsonElement> keySelector = compileArg(rawArgs.get(0), compiler);

    return data -> group(data, keySelector);
  }

  private static JsonElement group(
      JsonElement data,
      Function<JsonElement, JsonElement> keySelector) {

    if (data == null || data.isJsonNull() || !data.isJsonArray()) {
      return JsonNull.INSTANCE;
    }

    JsonObject grouped = new JsonObject();
    for (JsonElement element : data.getAsJsonArray()) {
      String key = toGroupKey(keySelector.apply(element));
      if (key != null) {
        bucket(grouped, key).add(element);
      }
    }
    return grouped;
  }

  private static JsonArray bucket(JsonObject grouped, String key) {
    JsonElement existing = grouped.get(key);
    if (existing != null && existing.isJsonArray()) {
      return existing.getAsJsonArray();
    }

    JsonArray bucket = new JsonArray();
    grouped.add(key, bucket);
    return bucket;
  }

  private static String toGroupKey(JsonElement keyValue) {
    if (keyValue == null || !keyValue.isJsonPrimitive()) {
      return null;
    }

    JsonPrimitive primitive = keyValue.getAsJsonPrimitive();
    if (primitive.isString()) {
      return primitive.getAsString();
    }
    if (primitive.isNumber()) {
      return primitive.getAsNumber().toString();
    }
    return primitive.isBoolean() ? Boolean.toString(primitive.getAsBoolean()) : null;
  }
}
