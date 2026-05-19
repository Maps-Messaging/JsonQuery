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

package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class AbsFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "abs";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() > 1) {
      throw new IllegalArgumentException("abs expects 0 or 1 arguments");
    }

    Function<JsonElement, JsonElement> expression =
        rawArgs.isEmpty()
            ? data -> data == null ? JsonNull.INSTANCE : data
            : compiler.compile(rawArgs.get(0));

    return data -> {
      JsonElement value = expression.apply(data);
      if (value == null || value.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
        return JsonNull.INSTANCE;
      }

      double number = value.getAsDouble();
      double abs = Math.abs(number);

      // Preserve integer-ness when possible
      if (Math.floor(abs) == abs) {
        return new JsonPrimitive((long) abs);
      }

      return new JsonPrimitive(abs);
    };
  }
}
