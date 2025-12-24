package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.function.BiPredicate;

public final class EqFunction extends AbstractBinaryPredicateFunction {

  @Override
  public String getName() {
    return "eq";
  }

  @Override
  protected BiPredicate<JsonElement, JsonElement> predicate() {
    return EqFunction::deepEquals;
  }

  @Override
  protected String functionName() {
    return "eq";
  }

  private static boolean deepEquals(JsonElement left, JsonElement right) {
    if (left == null || left.isJsonNull()) {
      return right == null || right.isJsonNull();
    }
    if (right == null || right.isJsonNull()) {
      return false;
    }

    // primitives
    if (left.isJsonPrimitive() && right.isJsonPrimitive()) {
      if ((isNumber(left) && isNumber(right))
          || (isString(left) && isString(right))
          || (isBoolean(left) && isBoolean(right))) {
        return compare(left, right) == 0;
      }
      return false;
    }

    // arrays: order matters
    if (left.isJsonArray() && right.isJsonArray()) {
      JsonArray la = left.getAsJsonArray();
      JsonArray ra = right.getAsJsonArray();
      if (la.size() != ra.size()) {
        return false;
      }
      for (int i = 0; i < la.size(); i++) {
        if (!deepEquals(la.get(i), ra.get(i))) {
          return false;
        }
      }
      return true;
    }

    // objects: key order does NOT matter
    if (left.isJsonObject() && right.isJsonObject()) {
      JsonObject lo = left.getAsJsonObject();
      JsonObject ro = right.getAsJsonObject();
      if (lo.size() != ro.size()) {
        return false;
      }
      for (Map.Entry<String, JsonElement> entry : lo.entrySet()) {
        String key = entry.getKey();
        if (!ro.has(key)) {
          return false;
        }
        if (!deepEquals(entry.getValue(), ro.get(key))) {
          return false;
        }
      }
      return true;
    }

    // different JSON types
    return false;
  }
}
