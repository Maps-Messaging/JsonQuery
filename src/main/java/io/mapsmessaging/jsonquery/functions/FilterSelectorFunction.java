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
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.util.List;
import java.util.function.Function;

public final class FilterSelectorFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "selector";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCountExact(rawArgs, 1, "1 argument: a JMS selector string");
    ParserExecutor executor = compileSelector(
        JsonQueryGson.requireString(rawArgs.get(0), "filter selector must be a string"));
    return data -> filter(data, executor);
  }

  private static ParserExecutor compileSelector(String selector) {
    try {
      return SelectorParser.compile(selector);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid selector: " + selector, e);
    }
  }

  private static JsonElement filter(JsonElement data, ParserExecutor executor) {
    if (data == null || data.isJsonNull()) {
      return JsonNull.INSTANCE;
    }
    if (!data.isJsonArray()) {
      return executor.evaluate(data) ? data : JsonNull.INSTANCE;
    }

    JsonArray output = new JsonArray();
    for (JsonElement element : data.getAsJsonArray()) {
      if (element != null && element.isJsonObject() && executor.evaluate(element)) {
        output.add(element);
      }
    }
    return output;
  }
}
