package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class ArrayFunction implements JsonQueryFunction {
  @Override
  public String getName() {
    return "array";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      throw new IllegalArgumentException("array expects at least 1 argument");
    }

    JsonArray template = new JsonArray();
    for (JsonElement arg : rawArgs) {
      template.add(arg);
    }

    JsonArray copy = deepCopyArray(template);

    return data -> deepCopyArray(copy);
  }

  private static JsonArray deepCopyArray(JsonArray array) {
    return JsonParser.parseString(array.toString()).getAsJsonArray();
  }
}
