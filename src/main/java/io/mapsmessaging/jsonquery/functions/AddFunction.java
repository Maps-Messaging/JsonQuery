package io.mapsmessaging.jsonquery.functions;

public final class AddFunction extends AbstractNumericBinaryFunction {

  @Override
  public String getName() {
    return "add";
  }

  @Override
  protected double apply(double left, double right) {
    return left + right;
  }

  @Override
  protected String symbol() {
    return "+";
  }
}
