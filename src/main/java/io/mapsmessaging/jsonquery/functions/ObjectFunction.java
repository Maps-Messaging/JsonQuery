package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class ObjectFunction extends AbstractFunction {

  @Override
  public String getName() {
    return "object";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: object({key: expr, ...})");

    JsonElement arg = rawArgs.get(0);
    if (arg == null || arg.isJsonNull()) {
      return data -> JsonNull.INSTANCE;
    }
    if (!arg.isJsonObject()) {
      throw new IllegalArgumentException("Object expected");
    }

    JsonObject template = arg.getAsJsonObject();

    Map<String, Function<JsonElement, JsonElement>> compiledFields = new LinkedHashMap<>();
    for (Map.Entry<String, JsonElement> entry : template.entrySet()) {
      Function<JsonElement, JsonElement> valueExpr = compileArg(entry.getValue(), compiler);
      compiledFields.put(entry.getKey(), valueExpr);
    }

    return data -> {
      JsonObject out = new JsonObject();
      for (Map.Entry<String, Function<JsonElement, JsonElement>> field : compiledFields.entrySet()) {
        JsonElement value = field.getValue().apply(data);
        out.add(field.getKey(), value == null ? JsonNull.INSTANCE : value);
      }
      return out;
    };
  }
}
