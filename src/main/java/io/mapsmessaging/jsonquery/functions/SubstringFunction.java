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
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class SubstringFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "substring";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCount(rawArgs, 1, 3, "1, 2, or 3 arguments");

    Function<JsonElement, JsonElement> valueExpression;
    Function<JsonElement, JsonElement> startExpression;
    Function<JsonElement, JsonElement> endExpression = null;

    if (rawArgs.size() == 1) {
      valueExpression = data -> data == null ? JsonNull.INSTANCE : data;
      startExpression = compiler.compile(rawArgs.get(0));
    } else {
      valueExpression = compiler.compile(rawArgs.get(0));
      startExpression = compiler.compile(rawArgs.get(1));
      if (rawArgs.size() == 3) {
        endExpression = compiler.compile(rawArgs.get(2));
      }
    }

    Function<JsonElement, JsonElement> finalEndExpression = endExpression;
    return data -> substring(
        valueExpression.apply(data),
        startExpression.apply(data),
        finalEndExpression == null ? null : finalEndExpression.apply(data),
        finalEndExpression != null);
  }

  private static JsonElement substring(
      JsonElement valueElement,
      JsonElement startElement,
      JsonElement endElement,
      boolean hasEnd) {

    String value = stringValue(valueElement);
    Integer startIndex = intValue(startElement);
    Integer endIndex = hasEnd ? intValue(endElement) : null;

    if (value == null || startIndex == null || (hasEnd && endIndex == null)) {
      return JsonNull.INSTANCE;
    }

    int length = value.length();
    int start = clamp(startIndex, 0, length);
    int end = hasEnd ? clamp(endIndex, 0, length) : length;

    return new JsonPrimitive(end < start ? "" : value.substring(start, end));
  }

  private static String stringValue(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString() ? primitive.getAsString() : null;
  }

  private static Integer intValue(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isNumber() ? primitive.getAsInt() : null;
  }

  private static int clamp(int value, int min, int max) {
    return Math.max(min, Math.min(value, max));
  }
}
