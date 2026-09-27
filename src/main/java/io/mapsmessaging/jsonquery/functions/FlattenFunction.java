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

import java.util.List;
import java.util.function.Function;

public final class FlattenFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "flatten";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCountExact(rawArgs, 0, "0 arguments");
    return FlattenFunction::flatten;
  }

  private static JsonElement flatten(JsonElement data) {
    if (data == null || data.isJsonNull() || !data.isJsonArray()) {
      return JsonNull.INSTANCE;
    }

    JsonArray output = new JsonArray();
    for (JsonElement element : data.getAsJsonArray()) {
      append(output, element);
    }
    return output;
  }

  private static void append(JsonArray output, JsonElement element) {
    if (element == null || element.isJsonNull()) {
      output.add(JsonNull.INSTANCE);
    } else if (element.isJsonArray()) {
      for (JsonElement inner : element.getAsJsonArray()) {
        output.add(inner == null ? JsonNull.INSTANCE : inner);
      }
    } else {
      output.add(element);
    }
  }
}
