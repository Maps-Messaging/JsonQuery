package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public final class SortFunction extends AbstractFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "sort";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() > 2) {
      throw new IllegalArgumentException("sort expects 0..2 arguments");
    }

    Function<JsonElement, JsonElement> selector = Function.identity();
    boolean descending = false;

    if (rawArgs.size() == 1) {
      JsonElement arg0 = rawArgs.get(0);
      if (isDirection(arg0)) {
        descending = isDesc(arg0.getAsString());
      } else {
        selector = compileArg(arg0, compiler);
      }
    } else if (rawArgs.size() == 2) {
      selector = compileArg(rawArgs.get(0), compiler);
      JsonElement dir = rawArgs.get(1);
      if (!isDirection(dir)) {
        throw new IllegalArgumentException("sort direction must be \"asc\" or \"desc\"");
      }
      descending = isDesc(dir.getAsString());
    }

    Function<JsonElement, JsonElement> selected = selector;
    Comparator<JsonElement> comparator = (left, right) -> {
      JsonElement leftKey = safe(selected.apply(left));
      JsonElement rightKey = safe(selected.apply(right));
      return compareJson(leftKey, rightKey);
    };

    if (descending) {
      comparator = comparator.reversed();
    }

    Comparator<JsonElement> finalComparator = comparator;

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        throw new IllegalArgumentException("Array expected");
      }

      JsonArray array = data.getAsJsonArray();
      List<JsonElement> list = new ArrayList<>(array.size());
      for (JsonElement element : array) {
        list.add(element);
      }

      list.sort(finalComparator);

      JsonArray out = new JsonArray();
      for (JsonElement element : list) {
        out.add(element);
      }
      return out;
    };
  }

  private static JsonElement safe(JsonElement element) {
    return element == null ? JsonNull.INSTANCE : element;
  }

  private static boolean isDirection(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return false;
    }
    if (!element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (!primitive.isString()) {
      return false;
    }
    String value = primitive.getAsString();
    return "asc".equalsIgnoreCase(value) || "desc".equalsIgnoreCase(value);
  }

  private static boolean isDesc(String value) {
    return "desc".equalsIgnoreCase(value);
  }

  /**
   * Bucketed ordering:
   * null < boolean < number < string < (array/object/other non-scalar bucket)
   *
   * Non-scalars are not mutually comparable -> return 0 when both are non-scalar,
   * so their relative order is preserved (stable sort).
   */
  private static int compareJson(JsonElement left, JsonElement right) {
    left = safe(left);
    right = safe(right);

    int leftRank = bucketRank(left);
    int rightRank = bucketRank(right);

    if (leftRank != rightRank) {
      return Integer.compare(leftRank, rightRank);
    }

    // Same bucket.
    // If both are non-scalar bucket, keep input order.
    if (leftRank == 4) {
      return 0;
    }

    // Scalar comparison inside bucket 0..3
    if (left.isJsonNull()) {
      return 0;
    }

    JsonPrimitive lp = left.getAsJsonPrimitive();
    JsonPrimitive rp = right.getAsJsonPrimitive();

    if (lp.isBoolean()) {
      return Boolean.compare(lp.getAsBoolean(), rp.getAsBoolean());
    }
    if (lp.isNumber()) {
      return Double.compare(lp.getAsDouble(), rp.getAsDouble());
    }
    return lp.getAsString().compareTo(rp.getAsString());
  }

  // 0..3 = scalars, 4 = non-scalar bucket (arrays/objects/etc)
  private static int bucketRank(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return 0;
    }
    if (!element.isJsonPrimitive()) {
      return 4; // array/object -> shove to end
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isBoolean()) {
      return 1;
    }
    if (primitive.isNumber()) {
      return 2;
    }
    if (primitive.isString()) {
      return 3;
    }
    return 4;
  }


  // Returns 0..3 for scalars, -1 for arrays/objects/other weirdness.
  private static int scalarRank(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return 0;
    }
    if (!element.isJsonPrimitive()) {
      return -1;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    if (primitive.isBoolean()) {
      return 1;
    }
    if (primitive.isNumber()) {
      return 2;
    }
    if (primitive.isString()) {
      return 3;
    }
    return -1;
  }


  private static int typeRank(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return 0;
    }
    if (element.isJsonPrimitive()) {
      JsonPrimitive primitive = element.getAsJsonPrimitive();
      if (primitive.isBoolean()) {
        return 1;
      }
      if (primitive.isNumber()) {
        return 2;
      }
      if (primitive.isString()) {
        return 3;
      }
      return 3;
    }
    if (element.isJsonArray()) {
      return 4;
    }
    if (element.isJsonObject()) {
      return 5;
    }
    return 6;
  }
}
