package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class MapKeysFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "mapKeys";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("mapKeys expects 1 argument (key mapper expression)");
    }

    Function<JsonElement, JsonElement> keyMapper = compiler.compile(rawArgs.get(0));

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
        JsonElement mappedKeyElement = keyMapper.apply(new com.google.gson.JsonPrimitive(entry.getKey()));
        if (mappedKeyElement == null || mappedKeyElement.isJsonNull()
            || !mappedKeyElement.isJsonPrimitive()
            || !mappedKeyElement.getAsJsonPrimitive().isString()) {
          continue;
        }

        String mappedKey = mappedKeyElement.getAsString();
        JsonElement value = entry.getValue() == null ? JsonNull.INSTANCE : entry.getValue();
        output.add(mappedKey, value);
      }

      return output;
    };
  }
}
