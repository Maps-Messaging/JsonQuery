package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MapValuesFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "mapValues";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("mapValues expects 1 argument (valueMapper)");
    }

    Function<JsonElement, JsonElement> valueMapper = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonObject()) {
        return JsonNull.INSTANCE;
      }

      JsonObject input = data.getAsJsonObject();
      JsonObject output = new JsonObject();

      for (Map.Entry<String, JsonElement> entry : input.entrySet()) {
        JsonElement mapped = valueMapper.apply(entry.getValue());
        output.add(entry.getKey(), mapped == null ? JsonNull.INSTANCE : mapped);
      }

      return output;
    };
  }
}
