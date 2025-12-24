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

package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.AbstractFunction;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractNumericBinaryFunction extends AbstractFunction {

  protected abstract double apply(double left, double right);

  protected abstract String symbol();

  @Override
  public final Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: " + getName() + "(a,b)");
    Function<JsonElement, JsonElement> leftExpr = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> rightExpr = compileArg(rawArgs.get(1), compiler);

    return data -> {
      double leftValue = JsonQueryFunction.asNumber(leftExpr.apply(data), "Number expected");
      double rightValue = JsonQueryFunction.asNumber(rightExpr.apply(data), "Number expected");
      double result = apply(leftValue, rightValue);
      return JsonQueryFunction.numberValue(result);
    };
  }
}
