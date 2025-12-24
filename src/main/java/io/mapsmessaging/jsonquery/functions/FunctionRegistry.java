/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.mapsmessaging.jsonquery.functions;

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

  public static FunctionRegistry builtIns() {
    ServiceLoader<JsonQueryFunction> serviceLoaded = ServiceLoader.load(JsonQueryFunction.class);
    Map<String, JsonQueryFunction> builtIns = new HashMap<>();
    for (JsonQueryFunction analyser : serviceLoaded) {
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

  public JsonQueryFunction get(String name) {
    return functions.get(name);
  }

  public Map<String, JsonQueryFunction> asMap() {
    return functions;
  }
}
