package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class JoinFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "join";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() > 1) {
      throw new IllegalArgumentException("join expects 0 or 1 arguments");
    }

    final String separator;
    if (rawArgs.isEmpty()) {
      separator = ",";
    } else {
      JsonElement arg = rawArgs.get(0);
      if (!arg.isJsonPrimitive() || !arg.getAsJsonPrimitive().isString()) {
        throw new IllegalArgumentException("join separator must be a string");
      }
      separator = arg.getAsString();
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray array = data.getAsJsonArray();
      StringBuilder sb = new StringBuilder();

      boolean first = true;
      for (JsonElement element : array) {
        if (!first) {
          sb.append(separator);
        }
        first = false;

        if (element == null || element.isJsonNull()) {
          sb.append("null");
        } else if (element.isJsonPrimitive()) {
          sb.append(element.getAsJsonPrimitive().getAsString());
        } else {
          sb.append(element.toString());
        }
      }

      return new JsonPrimitive(sb.toString());
    };
  }
}
