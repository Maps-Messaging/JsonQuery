package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

public final class SortFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "sort";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    SortDirection direction = SortDirection.ASC;
    Function<JsonElement, JsonElement> keySelector = data -> data;

    if (rawArgs.isEmpty()) {
      // sort()
    } else if (rawArgs.size() == 1) {
      JsonElement arg0 = rawArgs.get(0);
      if (isDirectionString(arg0)) {
        direction = parseDirection(arg0.getAsString());
      } else {
        keySelector = compiler.compile(arg0);
      }
    } else if (rawArgs.size() == 2) {
      keySelector = compiler.compile(rawArgs.get(0));
      JsonElement arg1 = rawArgs.get(1);
      if (!isDirectionString(arg1)) {
        throw new IllegalArgumentException("sort expects 0..2 arguments: sort([keySelector], [\"asc\"|\"desc\"])");
      }
      direction = parseDirection(arg1.getAsString());
    } else {
      throw new IllegalArgumentException("sort expects 0..2 arguments: sort([keySelector], [\"asc\"|\"desc\"])");
    }

    Comparator<JsonElement> comparator = (left, right) -> comparePrimitives(left, right);
    if (direction == SortDirection.DESC) {
      comparator = comparator.reversed();
    }

    Function<JsonElement, JsonElement> finalKeySelector = keySelector;
    Comparator<JsonElement> finalComparator = comparator;

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray input = data.getAsJsonArray();
      List<JsonElement> items = new ArrayList<>(input.size());
      for (JsonElement e : input) {
        items.add(e == null ? JsonNull.INSTANCE : e);
      }

      items.sort((a, b) -> {
        JsonElement ka = safeApply(finalKeySelector, a);
        JsonElement kb = safeApply(finalKeySelector, b);

        if (ka == null || ka.isJsonNull() || kb == null || kb.isJsonNull()) {
          return 0; // keep relative order (stable)
        }
        return finalComparator.compare(ka, kb);
      });

      JsonArray out = new JsonArray();
      for (JsonElement e : items) {
        out.add(e == null ? JsonNull.INSTANCE : e);
      }
      return out;
    };
  }

  private static JsonElement safeApply(Function<JsonElement, JsonElement> fn, JsonElement value) {
    try {
      JsonElement result = fn.apply(value);
      return result == null ? JsonNull.INSTANCE : result;
    } catch (RuntimeException e) {
      return JsonNull.INSTANCE;
    }
  }

  private static boolean isDirectionString(JsonElement element) {
    if (element == null || element.isJsonNull() || !element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (!primitive.isString()) {
      return false;
    }
    String text = primitive.getAsString();
    return "asc".equalsIgnoreCase(text) || "desc".equalsIgnoreCase(text);
  }

  private static SortDirection parseDirection(String value) {
    String normalized = value.toLowerCase(Locale.ROOT);
    if ("asc".equals(normalized)) {
      return SortDirection.ASC;
    }
    if ("desc".equals(normalized)) {
      return SortDirection.DESC;
    }
    throw new IllegalArgumentException("sort direction must be \"asc\" or \"desc\"");
  }

  private static int comparePrimitives(JsonElement left, JsonElement right) {
    if (!left.isJsonPrimitive() || !right.isJsonPrimitive()) {
      return 0;
    }

    JsonPrimitive lp = left.getAsJsonPrimitive();
    JsonPrimitive rp = right.getAsJsonPrimitive();

    if (lp.isNumber() && rp.isNumber()) {
      return Double.compare(lp.getAsDouble(), rp.getAsDouble());
    }

    if (lp.isString() && rp.isString()) {
      return lp.getAsString().compareTo(rp.getAsString());
    }

    if (lp.isBoolean() && rp.isBoolean()) {
      return Boolean.compare(lp.getAsBoolean(), rp.getAsBoolean());
    }

    return 0;
  }

  private enum SortDirection {
    ASC,
    DESC
  }
}
