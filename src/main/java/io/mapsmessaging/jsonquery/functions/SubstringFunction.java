package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class SubstringFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "substring";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (rawArgs.isEmpty() || rawArgs.size() > 3) {
      throw new IllegalArgumentException("substring expects 1, 2, or 3 arguments");
    }

    Function<JsonElement, JsonElement> valueExpression;
    Function<JsonElement, JsonElement> startExpression;
    Function<JsonElement, JsonElement> endExpression = null;

    if (rawArgs.size() == 1) {
      valueExpression = data -> data == null ? JsonNull.INSTANCE : data;
      startExpression = compiler.compile(rawArgs.get(0));
    } else {
      valueExpression = compiler.compile(rawArgs.get(0));
      startExpression = compiler.compile(rawArgs.get(1));
      if (rawArgs.size() == 3) {
        endExpression = compiler.compile(rawArgs.get(2));
      }
    }

    Function<JsonElement, JsonElement> finalEndExpression = endExpression;

    return data -> {
      JsonElement valueElement = valueExpression.apply(data);
      if (valueElement == null || valueElement.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!valueElement.isJsonPrimitive() || !valueElement.getAsJsonPrimitive().isString()) {
        return JsonNull.INSTANCE;
      }

      String value = valueElement.getAsString();

      Integer startIndex = readInt(startExpression.apply(data));
      if (startIndex == null) {
        return JsonNull.INSTANCE;
      }

      Integer endIndex = null;
      if (finalEndExpression != null) {
        endIndex = readInt(finalEndExpression.apply(data));
        if (endIndex == null) {
          return JsonNull.INSTANCE;
        }
      }

      int length = value.length();
      int start = clamp(startIndex, 0, length);
      int end = (endIndex == null) ? length : clamp(endIndex, 0, length);

      if (end < start) {
        int tmp = start;
        start = end;
        end = tmp;
      }

      return new JsonPrimitive(value.substring(start, end));
    };
  }

  private static Integer readInt(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return null;
    }
    if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
      return null;
    }
    return element.getAsInt();
  }

  private static int clamp(int value, int min, int max) {
    if (value < min) {
      return min;
    }
    if (value > max) {
      return max;
    }
    return value;
  }
}
