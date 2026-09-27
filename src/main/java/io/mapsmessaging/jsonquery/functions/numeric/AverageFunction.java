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

package io.mapsmessaging.jsonquery.functions.numeric;

import com.google.gson.JsonElement;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;
import io.mapsmessaging.jsonquery.functions.JsonQueryFunction;

import java.util.List;
import java.util.function.Function;

public final class AverageFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "average";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(
      List<JsonElement> rawArgs,
      JsonQueryCompiler compiler) {

    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("avg expects 0 arguments");
    }
    return AverageFunction::average;
  }

  private static JsonElement average(JsonElement data) {
    List<Double> values = NumericArray.values(data);
    if (values == null || values.isEmpty()) {
      return NumericArray.nullValue();
    }

    double sum = 0.0;
    for (double value : values) {
      sum += value;
    }
    return NumericArray.number(sum / values.size());
  }
}
