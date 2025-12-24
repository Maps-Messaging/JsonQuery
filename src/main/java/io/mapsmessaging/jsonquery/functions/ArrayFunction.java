package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class ArrayFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "array";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs == null || rawArgs.isEmpty()) {
      return data -> new JsonArray();
    }

    List<Function<JsonElement, JsonElement>> compiled = new ArrayList<>(rawArgs.size());
    for (JsonElement arg : rawArgs) {
      compiled.add(compileArg(arg, compiler));
    }

    return data -> {
      JsonArray out = new JsonArray();
      for (Function<JsonElement, JsonElement> expr : compiled) {
        JsonElement value = expr.apply(data);
        out.add(value == null ? JsonNull.INSTANCE : value);
      }
      return out;
    };
  }
}
