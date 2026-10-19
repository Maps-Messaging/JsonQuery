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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PickFunction extends AbstractFunction {

  private record LiteralSelector(
      String key,
      Function<JsonElement, JsonElement> selector) {
  }

  private record Selectors(
      List<LiteralSelector> literal,
      List<Function<JsonElement, JsonElement>> dynamic) {
  }

  @Override
  public String getName() {
    return "pick";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (rawArgs.isEmpty()) {
      throw new IllegalArgumentException("pick expects at least one selector");
    }

    Selectors selectors = compileSelectors(normalizeSelectors(rawArgs), compiler);
    Function<JsonElement, JsonElement> pickOne = element -> pick(element, selectors);
    return data -> apply(data, pickOne);
  }

  private static Selectors compileSelectors(
      List<JsonElement> selectorElements,
      JsonQueryCompiler compiler) {

    List<LiteralSelector> literal = new ArrayList<>();
    List<Function<JsonElement, JsonElement>> dynamic = new ArrayList<>();

    for (JsonElement selectorElement : selectorElements) {
      List<String> path = LiteralGetPath.strings(selectorElement);
      Function<JsonElement, JsonElement> selector = compiler.compile(selectorElement);
      if (path == null) {
        dynamic.add(selector);
      } else {
        literal.add(new LiteralSelector(path.get(path.size() - 1), selector));
      }
    }
    return new Selectors(List.copyOf(literal), List.copyOf(dynamic));
  }

  private static JsonElement apply(
      JsonElement data,
      Function<JsonElement, JsonElement> pickOne) {

    if (data == null || data.isJsonNull()) {
      return JsonNull.INSTANCE;
    }
    if (!data.isJsonArray()) {
      return pickOne.apply(data);
    }

    JsonArray output = new JsonArray();
    for (JsonElement element : data.getAsJsonArray()) {
      output.add(pickOne.apply(element));
    }
    return output;
  }

  private static JsonElement pick(JsonElement element, Selectors selectors) {
    if (element == null || !element.isJsonObject()) {
      return JsonNull.INSTANCE;
    }

    JsonObject result = new JsonObject();
    addLiteralSelections(result, element, selectors.literal());
    addDynamicSelections(result, element.getAsJsonObject(), selectors.dynamic());
    return result;
  }

  private static void addLiteralSelections(
      JsonObject result,
      JsonElement element,
      List<LiteralSelector> selectors) {

    for (LiteralSelector selector : selectors) {
      JsonElement value = selector.selector().apply(element);
      if (value != null && !value.isJsonNull()) {
        result.add(selector.key(), value);
      }
    }
  }

  private static void addDynamicSelections(
      JsonObject result,
      JsonObject source,
      List<Function<JsonElement, JsonElement>> selectors) {

    for (Function<JsonElement, JsonElement> selector : selectors) {
      String key = string(selector.apply(source));
      if (key != null && source.has(key)) {
        result.add(key, source.get(key));
      }
    }
  }

  private static String string(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString() ? primitive.getAsString() : null;
  }

  private static List<JsonElement> normalizeSelectors(List<JsonElement> rawArgs) {
    if (rawArgs.size() == 1 && rawArgs.get(0) != null && rawArgs.get(0).isJsonArray()) {
      List<JsonElement> selectors = new ArrayList<>();
      rawArgs.get(0).getAsJsonArray().forEach(selectors::add);
      return selectors;
    }
    return List.copyOf(rawArgs);
  }
}
