package io.mapsmessaging.jsonquery.functions;

import io.mapsmessaging.jsonquery.functions.binary.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.ServiceLoader;

public final class FunctionRegistry {

  private final Map<String, JsonQueryFunction> functions;

  public FunctionRegistry(Map<String, JsonQueryFunction> functions) {
    if (functions == null) {
      this.functions = Collections.emptyMap();
    } else {
      this.functions = Collections.unmodifiableMap(new HashMap<>(functions));
    }
  }

  public JsonQueryFunction get(String name) {
    return functions.get(name);
  }

  public Map<String, JsonQueryFunction> asMap() {
    return functions;
  }

  public static FunctionRegistry builtIns() {
    ServiceLoader<JsonQueryFunction> serviceLoaded = ServiceLoader.load(JsonQueryFunction.class);
    Map<String, JsonQueryFunction> builtIns = new HashMap<>();
    for(JsonQueryFunction analyser : serviceLoaded) {
      builtIns.put(analyser.getName(), analyser);
    }
    return new FunctionRegistry(builtIns);
  }

  public static FunctionRegistry merge(FunctionRegistry baseRegistry, FunctionRegistry customRegistry) {
    Map<String, JsonQueryFunction> merged = new HashMap<>();
    if (baseRegistry != null) {
      merged.putAll(baseRegistry.asMap());
    }
    if (customRegistry != null) {
      merged.putAll(customRegistry.asMap());
    }
    return new FunctionRegistry(merged);
  }
}
