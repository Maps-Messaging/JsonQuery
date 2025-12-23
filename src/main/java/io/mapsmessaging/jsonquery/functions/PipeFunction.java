package io.mapsmessaging.jsonquery.functions;


import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PipeFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      throw new IllegalArgumentException("pipe expects at least one stage");
    }

    List<Function<JsonElement, JsonElement>> stages = new ArrayList<>();
    for (JsonElement stageQuery : rawArgs) {
      stages.add(compiler.compile(stageQuery));
    }

    return data -> {
      JsonElement current = data;
      for (Function<JsonElement, JsonElement> stage : stages) {
        current = stage.apply(current);
      }
      return current;
    };
  }
}
