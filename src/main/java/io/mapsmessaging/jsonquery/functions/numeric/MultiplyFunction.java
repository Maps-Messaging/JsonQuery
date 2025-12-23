package io.mapsmessaging.jsonquery.functions.numeric;

public final class MultiplyFunction extends AbstractNumericBinaryFunction {

  @Override
  public String getName() {
    return "multiply";
  }

  @Override
  protected double apply(double left, double right) {
    return left * right;
  }

  @Override
  protected String symbol() {
    return "*";
  }
}
