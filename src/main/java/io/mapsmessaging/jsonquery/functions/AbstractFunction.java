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
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractFunction implements JsonQueryFunction {

  @Override
  public abstract String getName();

  protected final void requireArgCount(List<JsonElement> rawArgs, int min, int max, String usage) {
    int size = rawArgs.size();
    if (size < min || size > max) {
      throw new IllegalArgumentException(getName() + " expects " + usage);
    }
  }

  protected final void requireArgCountExact(List<JsonElement> rawArgs, int expected, String usage) {
    if (rawArgs.size() != expected) {
      throw new IllegalArgumentException(getName() + " expects " + usage);
    }
  }

  protected final Function<JsonElement, JsonElement> compileArg(JsonElement rawArg, JsonQueryCompiler compiler) {
    return JsonQueryFunction.compileArg(rawArg, compiler);
  }
}
