package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class KeysFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "keys";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("keys expects 0 arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonObject()) {
        return JsonNull.INSTANCE;
      }

      JsonArray result = new JsonArray();
      for (Map.Entry<String, JsonElement> entry : data.getAsJsonObject().entrySet()) {
        result.add(entry.getKey());
      }
      return result;
    };
  }
}
