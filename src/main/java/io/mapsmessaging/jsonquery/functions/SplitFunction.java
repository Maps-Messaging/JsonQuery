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
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class SplitFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "split";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCount(rawArgs, 0, 2, "0, 1, or 2 arguments");

    Function<JsonElement, JsonElement> valueExpression =
        rawArgs.isEmpty()
            ? data -> data == null ? JsonNull.INSTANCE : data
            : compiler.compile(rawArgs.get(0));

    Function<JsonElement, JsonElement> delimiterExpression =
        rawArgs.size() == 2 ? compiler.compile(rawArgs.get(1)) : null;

    return data -> split(
        valueExpression.apply(data),
        delimiterExpression == null ? null : delimiterExpression.apply(data),
        delimiterExpression != null);
  }

  private static JsonElement split(
      JsonElement valueElement,
      JsonElement delimiterElement,
      boolean hasDelimiter) {

    String value = stringValue(valueElement);
    if (value == null) {
      return JsonNull.INSTANCE;
    }
    if (value.isEmpty()) {
      return new JsonArray();
    }

    if (!hasDelimiter) {
      return splitWhitespace(value);
    }

    String delimiter = stringValue(delimiterElement);
    if (delimiter == null) {
      return JsonNull.INSTANCE;
    }
    return delimiter.isEmpty() ? splitCharacters(value) : splitLiteral(value, delimiter);
  }

  private static String stringValue(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString() ? primitive.getAsString() : null;
  }

  private static JsonArray splitWhitespace(String value) {
    JsonArray result = new JsonArray();
    String trimmed = value.trim();
    if (trimmed.isEmpty()) {
      return result;
    }
    for (String part : trimmed.split("\\s+")) {
      result.add(part);
    }
    return result;
  }

  private static JsonArray splitCharacters(String value) {
    JsonArray result = new JsonArray();
    for (int index = 0; index < value.length(); index++) {
      result.add(String.valueOf(value.charAt(index)));
    }
    return result;
  }

  private static JsonArray splitLiteral(String value, String delimiter) {
    JsonArray result = new JsonArray();
    for (String part : value.split(Pattern.quote(delimiter), -1)) {
      result.add(part);
    }
    return result;
  }
}
