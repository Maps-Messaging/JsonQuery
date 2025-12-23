package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class LimitFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "limit";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("limit expects 1 argument (count)");
    }

    JsonElement rawCount = rawArgs.get(0);
    if (rawCount == null || rawCount.isJsonNull() || !rawCount.isJsonPrimitive()) {
      throw new IllegalArgumentException("limit expects numeric count");
    }

    JsonPrimitive primitive = rawCount.getAsJsonPrimitive();
    if (!primitive.isNumber()) {
      throw new IllegalArgumentException("limit expects numeric count");
    }

    int count = primitive.getAsInt();
    if (count < 0) {
      count = 0;
    }

    int finalCount = count;

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }
      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray input = data.getAsJsonArray();
      JsonArray result = new JsonArray();

      int endExclusive = Math.min(finalCount, input.size());
      for (int index = 0; index < endExclusive; index++) {
        result.add(input.get(index));
      }

      return result;
    };
  }
}
