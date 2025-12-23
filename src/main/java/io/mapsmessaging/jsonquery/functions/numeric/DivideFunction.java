package io.mapsmessaging.jsonquery.functions.numeric;

public final class DivideFunction extends AbstractNumericBinaryFunction {

  @Override
  public String getName() {
    return "divide";
  }

  @Override
  protected double apply(double left, double right) {
    return left / right;
  }

  @Override
  protected String symbol() {
    return "/";
  }
}
