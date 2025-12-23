package io.mapsmessaging.jsonquery.functions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    Map<String, JsonQueryFunction> builtIns = new HashMap<>();
    builtIns.put("get", new GetFunction());
    builtIns.put("pipe", new PipeFunction());
    builtIns.put("selector", new FilterSelectorFunction());
    builtIns.put("filter", new FilterFunction());
    builtIns.put("gte", new GteFunction());
    builtIns.put("gt", new GtFunction());
    builtIns.put("sort", new SortFunction());
    builtIns.put("map", new MapFunction());
    builtIns.put("sum", new SumFunction());
    builtIns.put("pick", new PickFunction());
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
