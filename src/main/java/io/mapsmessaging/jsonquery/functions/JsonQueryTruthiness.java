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

package io.mapsmessaging.jsonquery.functions;

import com.google.gson.JsonElement;

public final class JsonQueryTruthiness {

  private JsonQueryTruthiness() {
  }

  public static boolean isTruthy(JsonElement value) {
    if (value == null || value.isJsonNull()) {
      return false;
    }
    if (value.isJsonPrimitive()) {
      if (value.getAsJsonPrimitive().isBoolean()) {
        return value.getAsBoolean();
      }
      if (value.getAsJsonPrimitive().isNumber()) {
        return value.getAsDouble() != 0.0d;
      }
      if (value.getAsJsonPrimitive().isString()) {
        return !value.getAsString().isEmpty();
      }
    }
    if (value.isJsonArray()) {
      return value.getAsJsonArray().size() > 0;
    }
    if (value.isJsonObject()) {
      return value.getAsJsonObject().size() > 0;
    }
    return true;
  }
}
