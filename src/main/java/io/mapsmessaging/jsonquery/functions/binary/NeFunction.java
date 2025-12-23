package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonElement;

import java.util.function.BiPredicate;

public final class NeFunction extends AbstractBinaryPredicateFunction {
  @Override
  public String getName() {
    return "ne";
  }

  @Override
  protected BiPredicate<JsonElement, JsonElement> predicate() {
    return (leftValue, rightValue) -> {
      if (isNumber(leftValue) && isNumber(rightValue)) {
        return Double.compare(leftValue.getAsDouble(), rightValue.getAsDouble()) != 0;
      }
      if (isString(leftValue) && isString(rightValue)) {
        return !leftValue.getAsString().equals(rightValue.getAsString());
      }
      if (isBoolean(leftValue) && isBoolean(rightValue)) {
        return leftValue.getAsBoolean() != rightValue.getAsBoolean();
      }
      return false;
    };
  }
  @Override
  protected String functionName() {
    return "ne";
  }

}
