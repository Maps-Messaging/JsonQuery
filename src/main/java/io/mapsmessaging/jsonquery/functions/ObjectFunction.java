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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ObjectFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "object";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: object({key: expr, ...})");

    JsonElement arg = rawArgs.get(0);
    if (arg == null || arg.isJsonNull()) {
      return data -> JsonNull.INSTANCE;
    }
    if (!arg.isJsonObject()) {
      throw new IllegalArgumentException("Object expected");
    }

    JsonObject template = arg.getAsJsonObject();

    Map<String, Function<JsonElement, JsonElement>> compiledFields = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : template.entrySet()) {
      Function<JsonElement, JsonElement> valueExpr = compileArg(entry.getValue(), compiler);
      compiledFields.put(entry.getKey(), valueExpr);
    }

    return data -> {
      JsonObject out = new JsonObject();
      for (Map.Entry<String, Function<JsonElement, JsonElement>> field : compiledFields.entrySet()) {
        JsonElement value = field.getValue().apply(data);
        out.add(field.getKey(), value == null ? JsonNull.INSTANCE : value);
      }
      return out;
    };
  }
}
