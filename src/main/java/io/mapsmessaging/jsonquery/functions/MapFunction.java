package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class MapFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "map";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("map expects 1 argument: a query to apply to each element");
    }

    Function<JsonElement, JsonElement> callback = compiler.compile(rawArgs.get(0));

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return data;
      }

      JsonArray inputArray = data.getAsJsonArray();
      JsonArray outputArray = new JsonArray();

      for (int i = 0; i < inputArray.size(); i++) {
        JsonElement mapped = callback.apply(inputArray.get(i));
        outputArray.add(JsonQueryGson.nullToJsonNull(mapped));
      }

      return outputArray;
    };
  }
}
