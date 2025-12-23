package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class SortFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty() || rawArgs.size() > 2) {
      throw new IllegalArgumentException("sort expects 1 or 2 arguments: sort(keySelector, [\"asc\"|\"desc\"])");
    }

    Function<JsonElement, JsonElement> keySelector = compiler.compile(rawArgs.get(0));

    boolean descending = false;
    if (rawArgs.size() == 2) {
      String direction = JsonQueryGson.requireString(rawArgs.get(1), "sort direction must be a string: \"asc\" or \"desc\"")
          .trim()
          .toLowerCase();

      if ("desc".equals(direction)) {
        descending = true;
      } else if (!"asc".equals(direction)) {
        throw new IllegalArgumentException("sort direction must be \"asc\" or \"desc\"");
      }
    }

    boolean finalDescending = descending;

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return data;
      }

      JsonArray input = data.getAsJsonArray();
      List<SortEntry> entries = new ArrayList<>(input.size());

      for (int i = 0; i < input.size(); i++) {
        JsonElement element = input.get(i);
        JsonElement key = keySelector.apply(element);
        entries.add(new SortEntry(i, element, JsonQueryGson.nullToJsonNull(key)));
      }

      entries.sort((left, right) -> {
        int result = compareKeys(left.getKey(), right.getKey(), finalDescending);
        if (result != 0) {
          return result;
        }
        return Integer.compare(left.getOriginalIndex(), right.getOriginalIndex());
      });

      JsonArray output = new JsonArray();
      for (SortEntry entry : entries) {
        output.add(entry.getElement());
      }
      return output;
    };
  }

  private int compareKeys(JsonElement left, JsonElement right, boolean descending) {
    boolean leftNull = left == null || left.isJsonNull();
    boolean rightNull = right == null || right.isJsonNull();

    if (leftNull && rightNull) {
      return 0;
    }
    if (leftNull) {
      return descending ? -1 : 1;
    }
    if (rightNull) {
      return descending ? 1 : -1;
    }

    int base = compareNonNull(left, right);
    if (base == 0) {
      return 0;
    }
    return descending ? -base : base;
  }

  private int compareNonNull(JsonElement left, JsonElement right) {
    if (left.isJsonPrimitive() && right.isJsonPrimitive()) {
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

      return lp.getAsString().compareTo(rp.getAsString());
    }

    return left.toString().compareTo(right.toString());
  }
}
