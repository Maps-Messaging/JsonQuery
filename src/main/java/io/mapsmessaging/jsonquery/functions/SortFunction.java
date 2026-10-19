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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

public final class SortFunction extends AbstractFunction {

  private record SortOptions(
      Function<JsonElement, JsonElement> selector,
      boolean descending) {
  }

  @Override
  public String getName() {
    return "sort";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    requireArgCount(rawArgs, 0, 2, "0..2 arguments");
    SortOptions options = parseOptions(rawArgs, compiler);
    Comparator<JsonElement> comparator = comparator(options);

    return data -> sort(data, comparator);
  }

  private static SortOptions parseOptions(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (rawArgs.isEmpty()) {
      return new SortOptions(Function.identity(), false);
    }

    if (rawArgs.size() == 1) {
      JsonElement argument = rawArgs.get(0);
      if (isDirection(argument)) {
        return new SortOptions(Function.identity(), isDesc(argument.getAsString()));
      }
      return new SortOptions(compiler.compile(argument), false);
    }

    JsonElement direction = rawArgs.get(1);
    if (!isDirection(direction)) {
      throw new IllegalArgumentException("sort direction must be \"asc\" or \"desc\"");
    }
    return new SortOptions(
        compiler.compile(rawArgs.get(0)),
        isDesc(direction.getAsString()));
  }

  private static Comparator<JsonElement> comparator(SortOptions options) {
    Comparator<JsonElement> comparator = (left, right) ->
        compareJson(
            safe(options.selector().apply(left)),
            safe(options.selector().apply(right)));

    return options.descending() ? comparator.reversed() : comparator;
  }

  private static JsonElement sort(JsonElement data, Comparator<JsonElement> comparator) {
    if (data == null || data.isJsonNull()) {
      return JsonNull.INSTANCE;
    }
    if (!data.isJsonArray()) {
      throw new IllegalArgumentException("Array expected");
    }

    JsonArray array = data.getAsJsonArray();
    List<JsonElement> elements = new ArrayList<>(array.size());
    array.forEach(elements::add);
    elements.sort(comparator);

    JsonArray result = new JsonArray();
    elements.forEach(result::add);
    return result;
  }

  private static JsonElement safe(JsonElement element) {
    return element == null ? JsonNull.INSTANCE : element;
  }

  private static boolean isDirection(JsonElement element) {
    if (element == null || !element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (!primitive.isString()) {
      return false;
    }
    String value = primitive.getAsString();
    return "asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value);
  }

  private static boolean isDesc(String value) {
    return "desc".equalsIgnoreCase(value);
  }

  private static int compareJson(JsonElement left, JsonElement right) {
    int leftRank = bucketRank(left);
    int rightRank = bucketRank(right);
    if (leftRank != rightRank) {
      return Integer.compare(leftRank, rightRank);
    }
    if (leftRank == 0 || leftRank == 4) {
      return 0;
    }
    return comparePrimitives(left.getAsJsonPrimitive(), right.getAsJsonPrimitive());
  }

  private static int comparePrimitives(JsonPrimitive left, JsonPrimitive right) {
    if (left.isBoolean()) {
      return Boolean.compare(left.getAsBoolean(), right.getAsBoolean());
    }
    if (left.isNumber()) {
      return Double.compare(left.getAsDouble(), right.getAsDouble());
    }
    return left.getAsString().compareTo(right.getAsString());
  }

  private static int bucketRank(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return 0;
    }
    if (!element.isJsonPrimitive()) {
      return 4;
    }

    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isBoolean()) {
      return 1;
    }
    if (primitive.isNumber()) {
      return 2;
    }
    if (primitive.isString()) {
      return 3;
    }
    return 4;
  }
}
