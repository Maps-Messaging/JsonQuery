package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.selector.ParseException;
import io.mapsmessaging.selector.SelectorParser;
import io.mapsmessaging.selector.operators.ParserExecutor;

import java.util.List;
import java.util.function.Function;

public final class FilterFunction implements JsonQueryFunction {

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.size() != 1) {
      throw new IllegalArgumentException("filter expects 1 argument: a JMS selector string");
    }

    String selector = JsonQueryGson.requireString(rawArgs.get(0), "filter selector must be a string");

    ParserExecutor executor;
    try {
      executor = SelectorParser.compile(selector);
    } catch (ParseException e) {
      throw new IllegalArgumentException("Invalid selector: " + selector, e);
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return JsonNull.INSTANCE;
      }

      if (data.isJsonObject()) {
        JsonObject object = data.getAsJsonObject();
        if (executor.evaluate(object)) {
          return object;
        }
        return JsonNull.INSTANCE;
      }

      if (!data.isJsonArray()) {
        return JsonNull.INSTANCE;
      }

      JsonArray inputArray = data.getAsJsonArray();
      JsonArray outputArray = new JsonArray();

      for (int i = 0; i < inputArray.size(); i++) {
        JsonElement element = inputArray.get(i);
        if (element != null && element.isJsonObject()) {
          JsonObject object = element.getAsJsonObject();
          if (executor.evaluate(object)) {
            outputArray.add(object);
          }
        }
      }

      return outputArray;
    };
  }
}
