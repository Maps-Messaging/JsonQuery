package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonElement;

import java.util.function.BiPredicate;


public final class EqFunction extends AbstractBinaryPredicateFunction {
  @Override
  public String getName() {
    return "eq";
  }

  @Override
  protected BiPredicate<JsonElement, JsonElement> predicate() {
    return (leftValue, rightValue) -> {
      if ((isNumber(leftValue) && isNumber(rightValue))
          || (isString(leftValue) && isString(rightValue))
          || (isBoolean(leftValue) && isBoolean(rightValue))) {
        return compare(leftValue, rightValue) == 0;
      }
      return false;
    };
  }

  @Override
  protected String functionName() {
    return "eq";
  }
}

