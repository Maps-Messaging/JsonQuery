package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractFunction implements JsonQueryFunction {

  @Override
  public abstract String getName();

  protected final void requireArgCount(List<JsonElement> rawArgs, int min, int max, String usage) {
    int size = rawArgs.size();
    if (size < min || size > max) {
      throw new IllegalArgumentException(getName() + " expects " + usage);
    }
  }

  protected final void requireArgCountExact(List<JsonElement> rawArgs, int expected, String usage) {
    if (rawArgs.size() != expected) {
      throw new IllegalArgumentException(getName() + " expects " + usage);
    }
  }

  protected final Function<JsonElement, JsonElement> compileArg(JsonElement rawArg, JsonQueryCompiler compiler) {
    return JsonQueryFunction.compileArg(rawArg, compiler);
  }
}
