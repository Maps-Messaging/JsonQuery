package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Function;

public abstract class AbstractBinaryPredicateFunction implements JsonQueryFunction {

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

      if (leftValue == null || leftValue.isJsonNull() || rightValue == null || rightValue.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      boolean result = binaryPredicate.test(leftValue, rightValue);
      return new JsonPrimitive(result);
    };
  }

  protected String functionName() {
    return getClass().getSimpleName();
  }

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
}
