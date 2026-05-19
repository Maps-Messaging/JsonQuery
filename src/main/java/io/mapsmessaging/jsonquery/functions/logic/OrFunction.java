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

package io.mapsmessaging.jsonquery.functions.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.AbstractFunction;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class OrFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "or";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {

    // zero-arg: identity
    if (rawArgs.isEmpty()) {
      return data -> (data == null ? JsonNull.INSTANCE : data);
    }

    List<Function<JsonElement, JsonElement>> expressions = new ArrayList<>();
    for (JsonElement arg : rawArgs) {
      expressions.add(compileArg(arg, compiler));
    }

    return data -> {
      for (Function<JsonElement, JsonElement> expr : expressions) {
        JsonElement value = expr.apply(data);
        if (JsonQueryFunction.isTruthy(value)) {
          return new JsonPrimitive(true);
        }
      }
      return new JsonPrimitive(false);
    };
  }
}
