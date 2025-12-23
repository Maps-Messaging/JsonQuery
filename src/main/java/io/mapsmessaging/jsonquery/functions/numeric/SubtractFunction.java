package io.mapsmessaging.jsonquery.functions.numeric;

public final class SubtractFunction extends AbstractNumericBinaryFunction {

  @Override
  public String getName() {
    return "subtract";
  }

  @Override
  protected double apply(double left, double right) {
    return left - right;
  }

  @Override
  protected String symbol() {
    return "-";
  }
}
