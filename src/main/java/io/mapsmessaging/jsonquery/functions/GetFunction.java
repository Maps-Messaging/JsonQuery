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

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

public final class GetFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "get";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (rawArgs.isEmpty()) {
      return data -> data == null ? JsonNull.INSTANCE : data;
    }

    PathSegment[] path = compilePath(rawArgs);
    return data -> resolve(data, path);
  }

  private static PathSegment[] compilePath(List<JsonElement> rawArgs) {
    PathSegment[] path = new PathSegment[rawArgs.size()];
    for (int index = 0; index < rawArgs.size(); index++) {
      path[index] = pathSegment(rawArgs.get(index));
    }
    return path;
  }

  private static PathSegment pathSegment(JsonElement rawArg) {
    if (rawArg == null || !rawArg.isJsonPrimitive()) {
      throw new IllegalArgumentException("get expects path segments of type string or number");
    }

    JsonPrimitive primitive = rawArg.getAsJsonPrimitive();
    if (primitive.isString()) {
      return new ObjectPathSegment(primitive.getAsString());
    }
    if (!primitive.isNumber()) {
      throw new IllegalArgumentException("get expects path segments of type string or number");
    }
    return new ArrayPathSegment(arrayIndex(primitive.getAsBigDecimal()));
  }

  private static int arrayIndex(BigDecimal value) {
    if (value.scale() > 0) {
      throw new IllegalArgumentException("get expects an integer array index");
    }
    if (value.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0
        || value.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
      throw new IllegalArgumentException("get array index out of int range");
    }
    return value.intValue();
  }

  private static JsonElement resolve(JsonElement data, PathSegment[] path) {
    JsonElement current = data == null ? JsonNull.INSTANCE : data;
    for (PathSegment segment : path) {
      current = segment.resolve(current);
      if (current.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
    }
    return current;
  }

  private interface PathSegment {
    JsonElement resolve(JsonElement current);
  }

  private record ObjectPathSegment(String key) implements PathSegment {
    @Override
    public JsonElement resolve(JsonElement current) {
      if (current == null || current.isJsonNull() || !current.isJsonObject()) {
        return JsonNull.INSTANCE;
      }
      JsonObject object = current.getAsJsonObject();
      JsonElement next = object.get(key);
      return next == null ? JsonNull.INSTANCE : next;
    }
  }

  private record ArrayPathSegment(int index) implements PathSegment {
    @Override
    public JsonElement resolve(JsonElement current) {
      if (current == null || current.isJsonNull() || !current.isJsonArray()) {
        return JsonNull.INSTANCE;
      }
      JsonArray array = current.getAsJsonArray();
      if (index < 0 || index >= array.size()) {
        return JsonNull.INSTANCE;
      }
      JsonElement next = array.get(index);
      return next == null ? JsonNull.INSTANCE : next;
    }
  }
}
