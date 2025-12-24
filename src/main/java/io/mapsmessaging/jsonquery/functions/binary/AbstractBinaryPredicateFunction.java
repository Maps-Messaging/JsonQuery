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

package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public abstract class AbstractBinaryPredicateFunction implements JsonQueryFunction {

  protected static boolean isNumber(JsonElement value) {
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber();
  }

  protected static boolean isString(JsonElement value) {
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isString();
  }

  protected static boolean isBoolean(JsonElement value) {
    return value != null && value.isJsonPrimitive() && value.getAsJsonPrimitive().isBoolean();
  }

  protected static int compare(JsonElement leftValue, JsonElement rightValue) {
    if (isNumber(leftValue) && isNumber(rightValue)) {
      double leftNumber = leftValue.getAsDouble();
      double rightNumber = rightValue.getAsDouble();
      return Double.compare(leftNumber, rightNumber);
    }

    if (isString(leftValue) && isString(rightValue)) {
      String leftString = leftValue.getAsString();
      String rightString = rightValue.getAsString();
      return leftString.compareTo(rightString);
    }

    if (isBoolean(leftValue) && isBoolean(rightValue)) {
      boolean leftBoolean = leftValue.getAsBoolean();
      boolean rightBoolean = rightValue.getAsBoolean();
      return Boolean.compare(leftBoolean, rightBoolean);
    }

    return 0;
  }

  protected abstract BiPredicate<JsonElement, JsonElement> predicate();

  @Override
  public final Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 2) {
      throw new IllegalArgumentException(functionName() + " expects 2 arguments");
    }

    Function<JsonElement, JsonElement> leftExpression = compiler.compile(rawArgs.get(0));
    Function<JsonElement, JsonElement> rightExpression = compiler.compile(rawArgs.get(1));
    BiPredicate<JsonElement, JsonElement> binaryPredicate = predicate();

    return data -> {
      JsonElement leftValue = leftExpression.apply(data);
      JsonElement rightValue = rightExpression.apply(data);

      boolean leftIsNull = (leftValue == null || leftValue.isJsonNull());
      boolean rightIsNull = (rightValue == null || rightValue.isJsonNull());

      if (leftIsNull && rightIsNull) {
        return new JsonPrimitive(true);
      }
      if (leftIsNull || rightIsNull) {
        return new JsonPrimitive(false);
      }

      boolean result = binaryPredicate.test(leftValue, rightValue);
      return new JsonPrimitive(result);
    };
  }

  protected String functionName() {
    return getClass().getSimpleName();
  }
}
