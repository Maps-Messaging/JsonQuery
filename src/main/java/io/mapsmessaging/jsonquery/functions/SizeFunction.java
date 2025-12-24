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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.util.List;
import java.util.function.Function;

public final class SizeFunction implements JsonQueryFunction {

  @Override
  public String getName() {
    return "size";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs,
                                                    JsonQueryCompiler compiler) {

    if (!rawArgs.isEmpty()) {
      throw new IllegalArgumentException("size expects 0 arguments");
    }

    return data -> {
      if (data == null || data.isJsonNull()) {
        return new JsonPrimitive(0);
      }

      if (data.isJsonArray()) {
        JsonArray array = data.getAsJsonArray();
        return new JsonPrimitive(array.size());
      }

      if (data.isJsonObject()) {
        JsonObject object = data.getAsJsonObject();
        return new JsonPrimitive(object.size());
      }

      if (data.isJsonPrimitive()) {
        JsonPrimitive primitive = data.getAsJsonPrimitive();
        if (primitive.isString()) {
          return new JsonPrimitive(primitive.getAsString().length());
        }
      }

      return new JsonPrimitive(0);
    };
  }
}
