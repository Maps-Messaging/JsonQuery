package io.mapsmessaging.jsonquery.functions.logic;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.AbstractFunction;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class AndFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "and";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      return data -> (data == null ? com.google.gson.JsonNull.INSTANCE : data);
    }

    List<Function<JsonElement, JsonElement>> expressions = new ArrayList<>();
    for (JsonElement arg : rawArgs) {
      expressions.add(compileArg(arg, compiler));
    }

    return data -> {
      for (Function<JsonElement, JsonElement> expr : expressions) {
        JsonElement value = expr.apply(data);
        if (!JsonQueryFunction.isTruthy(value)) {
          return new JsonPrimitive(false);
        }
      }
      return new JsonPrimitive(true);
    };
  }

}
