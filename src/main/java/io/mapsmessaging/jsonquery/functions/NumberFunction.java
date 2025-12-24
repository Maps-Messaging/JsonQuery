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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class NumberFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "number";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: number(value)");
    Function<JsonElement, JsonElement> arg = compileArg(rawArgs.get(0), compiler);

    return data -> {
      JsonElement value = arg.apply(data);
      if (JsonQueryFunction.isNull(value)) {
        return JsonNull.INSTANCE;
      }
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
        return value;
      }
      if (value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()) {
        String text = value.getAsString().trim();
        if (text.isEmpty()) {
          return JsonNull.INSTANCE;
        }
        try {
          double parsed = Double.parseDouble(text);
          return JsonQueryFunction.numberValue(parsed);
        } catch (NumberFormatException exception) {
          return JsonNull.INSTANCE;
        }
      }
      return JsonNull.INSTANCE;
    };
  }
}
