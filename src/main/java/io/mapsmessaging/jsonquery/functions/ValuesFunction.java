package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ValuesFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "values";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("values expects 0 arguments");
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
        JsonElement value = entry.getValue();
        result.add(value == null ? JsonNull.INSTANCE : value);
      }
      return result;
    };
  }
}
