package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

public final class SplitFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "split";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (rawArgs.size() > 2) {
      throw new IllegalArgumentException("split expects 0, 1, or 2 arguments");
    }

    Function<JsonElement, JsonElement> valueExpression;
    if (rawArgs.isEmpty()) {
      valueExpression = data -> data == null ? JsonNull.INSTANCE : data;
    } else {
      valueExpression = compiler.compile(rawArgs.get(0));
    }

    Function<JsonElement, JsonElement> delimiterExpression = null;
    if (rawArgs.size() == 2) {
      delimiterExpression = compiler.compile(rawArgs.get(1));
    }

    Function<JsonElement, JsonElement> finalDelimiterExpression = delimiterExpression;

    return data -> {
      JsonElement valueElement = valueExpression.apply(data);
      if (valueElement == null || valueElement.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!valueElement.isJsonPrimitive() || !valueElement.getAsJsonPrimitive().isString()) {
        return JsonNull.INSTANCE;
      }

      String value = valueElement.getAsString();
      if (value.isEmpty()) {
        return new JsonArray();
      }

      String delimiter = null;
      if (finalDelimiterExpression != null) {
        JsonElement delimiterElement = finalDelimiterExpression.apply(data);
        if (delimiterElement == null || delimiterElement.isJsonNull()) {
          return JsonNull.INSTANCE;
        }
        if (!delimiterElement.isJsonPrimitive() || !delimiterElement.getAsJsonPrimitive().isString()) {
          return JsonNull.INSTANCE;
        }
        delimiter = delimiterElement.getAsString();
      }

      JsonArray out = new JsonArray();

      if (delimiter == null) {
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
          return out;
        }
        for (String part : trimmed.split("\\s+")) {
          out.add(new JsonPrimitive(part));
        }
        return out;
      }

      if (delimiter.isEmpty()) {
        for (int i = 0; i < value.length(); i++) {
          out.add(new JsonPrimitive(String.valueOf(value.charAt(i))));
        }
        return out;
      }

      String[] parts = value.split(Pattern.quote(delimiter), -1);
      for (String part : parts) {
        out.add(new JsonPrimitive(part));
      }
      return out;
    };
  }
}
