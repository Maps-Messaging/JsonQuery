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

public final class SubstringFunction implements JsonQueryFunction {

  private static Integer readInt(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return null;
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      return null;
    }
    return element.getAsInt();
  }

  private static int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }

  @Override
  public String getName() {
    return "substring";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (rawArgs.isEmpty() || rawArgs.size() > 3) {
      throw new IllegalArgumentException("substring expects 1, 2, or 3 arguments");
    }

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

    return data -> {
      JsonElement valueElement = valueExpression.apply(data);
      if (valueElement == null || valueElement.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!valueElement.isJsonPrimitive() || !valueElement.getAsJsonPrimitive().isString()) {
        return JsonNull.INSTANCE;
      }

      String value = valueElement.getAsString();

      Integer startIndex = readInt(startExpression.apply(data));
      if (startIndex == null) {
        return JsonNull.INSTANCE;
      }

      Integer endIndex = null;
      if (finalEndExpression != null) {
        endIndex = readInt(finalEndExpression.apply(data));
        if (endIndex == null) {
          return JsonNull.INSTANCE;
        }
      }

      int length = value.length();
      int start = clamp(startIndex, 0, length);
      int end = (endIndex == null) ? length : clamp(endIndex, 0, length);

      if (end < start) {
        return new JsonPrimitive("");
      }
      return new JsonPrimitive(value.substring(start, end));
    };
  }
}
