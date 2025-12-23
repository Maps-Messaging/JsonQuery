package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class FlattenFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "flatten";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("flatten expects 0 arguments");
    }

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
        if (element == null || element.isJsonNull()) {
          output.add(JsonNull.INSTANCE);
          continue;
        }

        if (element.isJsonArray()) {
          for (JsonElement inner : element.getAsJsonArray()) {
            output.add(inner == null ? JsonNull.INSTANCE : inner);
          }
        } else {
          output.add(element);
        }
      }

      return output;
    };
  }
}
