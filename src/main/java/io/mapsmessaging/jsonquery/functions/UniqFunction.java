package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class UniqFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "uniq";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("uniq expects 0 arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray input = data.getAsJsonArray();
      Set<JsonElement> seen = new LinkedHashSet<>();

      for (JsonElement element : input) {
        seen.add(element);
      }

      JsonArray result = new JsonArray();
      for (JsonElement element : seen) {
        result.add(element);
      }

      return result;
    };
  }
}
