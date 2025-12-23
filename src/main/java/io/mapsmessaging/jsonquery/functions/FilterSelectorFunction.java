package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class FilterSelectorFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("filter expects 1 argument");
    }

    Function<JsonElement, JsonElement> predicate = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray input = data.getAsJsonArray();
      JsonArray output = new JsonArray();

      for (JsonElement element : input) {
        JsonElement predicateResult = predicate.apply(element);
        if (JsonQueryTruthiness.isTruthy(predicateResult)) {
          output.add(element);
        }
      }

      return output;
    };
  }
}
