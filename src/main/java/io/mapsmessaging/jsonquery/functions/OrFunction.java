package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class OrFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "or";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: or(a,b)");
    Function<JsonElement, JsonElement> left = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> right = compileArg(rawArgs.get(1), compiler);

    return data -> {
      JsonElement leftValue = left.apply(data);
      if (JsonQueryFunction.isTruthy(leftValue)) {
        return new JsonPrimitive(true);
      }
      JsonElement rightValue = right.apply(data);
      return new JsonPrimitive(JsonQueryFunction.isTruthy(rightValue));
    };
  }
}
