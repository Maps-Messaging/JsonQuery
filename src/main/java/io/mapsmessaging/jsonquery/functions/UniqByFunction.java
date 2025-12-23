package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class UniqByFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "uniqBy";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("uniqBy expects 1 argument (keySelector)");
    }

    Function<JsonElement, JsonElement> keySelector = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray input = data.getAsJsonArray();
      JsonArray result = new JsonArray();
      Set<JsonElement> seenKeys = new LinkedHashSet<>();

      for (JsonElement element : input) {
        JsonElement key = keySelector.apply(element);
        if (key == null || key.isJsonNull()) {
          continue;
        }
        if (seenKeys.add(key)) {
          result.add(element);
        }
      }

      return result;
    };
  }
}
