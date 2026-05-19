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
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class InFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "in";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: in(value, array)");
    Function<JsonElement, JsonElement> valueExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> arrayExpr = compileArg(rawArgs.get(1), compiler);

    return data -> {
      JsonElement value = valueExpr.apply(data);
      JsonElement arrayValue = arrayExpr.apply(data);

      if (JsonQueryFunction.isNull(arrayValue)) {
        return new JsonPrimitive(false);
      }
      if (!arrayValue.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray array = arrayValue.getAsJsonArray();
      for (JsonElement element : array) {
        if (element == null) {
          continue;
        }
        if (value == null) {
          if (element.isJsonNull()) {
            return new JsonPrimitive(true);
          }
          continue;
        }
        if (value.equals(element)) {
          return new JsonPrimitive(true);
        }
      }
      return new JsonPrimitive(false);
    };
  }
}
