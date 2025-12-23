package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonElement;

import java.util.function.BiPredicate;

public final class LtFunction extends AbstractBinaryPredicateFunction {
  @Override
  public String getName() {
    return "lt";
  }

  @Override
  protected BiPredicate<JsonElement, JsonElement> predicate() {
    return (leftValue, rightValue) -> {
      Integer result = compare(leftValue, rightValue);
      return result != null && result < 0;
    };
  }
  @Override
  protected String functionName() {
    return "lt";
  }

}
