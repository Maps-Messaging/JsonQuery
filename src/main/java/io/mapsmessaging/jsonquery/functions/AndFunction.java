package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class AndFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "and";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 2, "2 arguments: and(a,b)");
    Function<JsonElement, JsonElement> left = compileArg(rawArgs.get(0), compiler);
    Function<JsonElement, JsonElement> right = compileArg(rawArgs.get(1), compiler);

    return data -> {
      JsonElement leftValue = left.apply(data);
      if (!JsonQueryFunction.isTruthy(leftValue)) {
        return new JsonPrimitive(false);
      }
      JsonElement rightValue = right.apply(data);
      return new JsonPrimitive(JsonQueryFunction.isTruthy(rightValue));
    };
  }
}
