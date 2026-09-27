/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.jsonquery.functions.binary;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class NeFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "ne";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (rawArgs.size() != 2) {
      throw new IllegalArgumentException("ne expects 2 arguments");
    }

    Function<JsonElement, JsonElement> left = compiler.compile(rawArgs.get(0));
    Function<JsonElement, JsonElement> right = compiler.compile(rawArgs.get(1));
    return data -> new JsonPrimitive(!JsonDeepEquality.equals(left.apply(data), right.apply(data)));
  }
}
