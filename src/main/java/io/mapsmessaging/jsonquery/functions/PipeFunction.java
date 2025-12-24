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

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class PipeFunction implements JsonQueryFunction {
  @Override
  public String getName() {
    return "pipe";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    if (rawArgs.isEmpty()) {
      return data -> data == null ? JsonNull.INSTANCE : data;
    }

    List<Function<JsonElement, JsonElement>> stages = new ArrayList<>(rawArgs.size());
    for (JsonElement stageExpr : rawArgs) {
      stages.add(compiler.compile(stageExpr));
    }

    return data -> {
      JsonElement current = (data == null) ? JsonNull.INSTANCE : data;
      for (Function<JsonElement, JsonElement> stage : stages) {
        current = stage.apply(current);
      }
      return current;
    };
  }
}
