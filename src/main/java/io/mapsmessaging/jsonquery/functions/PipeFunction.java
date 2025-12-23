package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PipeFunction implements JsonQueryFunction {
  @Override
  public String getName() {
    return "pipe";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      return data -> data == null ? JsonNull.INSTANCE : data;
    }

    List<Function<JsonElement, JsonElement>> stages = new ArrayList<>(rawArgs.size());
    for (JsonElement stageExpr : rawArgs) {
      stages.add(compiler.compile(stageExpr));
    }

    return data -> {
      JsonElement current = (data == null) ? JsonNull.INSTANCE : data;
      for (Function<JsonElement, JsonElement> stage : stages) {
        current = stage.apply(current);
      }
      return current;
    };
  }
}
