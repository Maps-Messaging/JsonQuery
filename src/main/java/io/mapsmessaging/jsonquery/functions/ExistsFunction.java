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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ExistsFunction extends AbstractFunction {

  private static List<String> tryExtractLiteralGetPath(JsonElement expr) {
    if (expr == null || !expr.isJsonArray()) {
      return null;
    }
    JsonArray array = expr.getAsJsonArray();
    if (array.size() < 2) {
      return null;
    }

    JsonElement op = array.get(0);
    if (op == null || !op.isJsonPrimitive() || !op.getAsJsonPrimitive().isString()) {
      return null;
    }
    if (!"get".equals(op.getAsString())) {
      return null;
    }

    List<String> path = new ArrayList<>();
    for (int i = 1; i < array.size(); i++) {
      JsonElement segment = array.get(i);
      if (segment == null || !segment.isJsonPrimitive()) {
        return null;
      }
      JsonPrimitive primitive = segment.getAsJsonPrimitive();
      if (!primitive.isString()) {
        return null;
      }
      path.add(primitive.getAsString());
    }
    return path;
  }

  private static boolean pathExists(JsonElement data, List<String> path) {
    if (data == null || data.isJsonNull()) {
      return false;
    }

    JsonElement current = data;

    for (int i = 0; i < path.size(); i++) {
      if (current == null || current.isJsonNull() || !current.isJsonObject()) {
        return false;
      }

      JsonObject obj = current.getAsJsonObject();
      String key = path.get(i);

      if (!obj.has(key)) {
        return false;
      }

      current = obj.get(key);

      // If we still have more segments to traverse, null can't be traversed.
      if (i < path.size() - 1 && (current == null || current.isJsonNull())) {
        return false;
      }
    }

    // Final key exists even if its value is JsonNull
    return true;
  }

  @Override
  public String getName() {
    return "exists";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: exists(expr)");

    JsonElement rawExpr = rawArgs.get(0);
    List<String> getPath = tryExtractLiteralGetPath(rawExpr);

    Function<JsonElement, JsonElement> arg = compileArg(rawExpr, compiler);

    if (getPath != null) {
      return data -> new JsonPrimitive(pathExists(data, getPath));
    }

    return data -> {
      JsonElement value = arg.apply(data);
      return new JsonPrimitive(value != null && !value.isJsonNull());
    };
  }
}
