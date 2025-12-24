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
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class IfFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "if";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 3, "3 arguments: if(condition, thenExpr, elseExpr)");
    Function<JsonElement, JsonElement> condition = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> thenExpr = compileArg(rawArgs.get(1), compiler);
    Function<JsonElement, JsonElement> elseExpr = compileArg(rawArgs.get(2), compiler);

    return data -> {
      JsonElement conditionValue = condition.apply(data);

      boolean cond;
      if (conditionValue == null || conditionValue.isJsonNull()) {
        cond = false;
      } else if (conditionValue.isJsonPrimitive()) {
        if (conditionValue.getAsJsonPrimitive().isBoolean()) {
          cond = conditionValue.getAsBoolean();
        } else if (conditionValue.getAsJsonPrimitive().isNumber()) {
          cond = conditionValue.getAsDouble() != 0.0;
        } else {
          // strings (including "") count as true
          cond = true;
        }
      } else {
        // arrays/objects count as true
        cond = true;
      }

      if (cond) {
        return thenExpr.apply(data);
      }
      return elseExpr.apply(data);
    };


  }
}
