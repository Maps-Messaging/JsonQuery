package io.mapsmessaging.jsonquery.functions;


import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

@FunctionalInterface
public interface JsonQueryFunction {

  Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler
  );
}
