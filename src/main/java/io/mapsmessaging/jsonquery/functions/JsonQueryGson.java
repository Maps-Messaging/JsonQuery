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
import com.google.gson.JsonPrimitive;

public final class JsonQueryGson {

  private JsonQueryGson() {
  }

  public static boolean isString(JsonElement element) {
    if (element == null || element.isJsonNull()) {
      return false;
    }
    if (!element.isJsonPrimitive()) {
      return false;
    }
    JsonPrimitive primitive = element.getAsJsonPrimitive();
    return primitive.isString();
  }

  public static String requireString(JsonElement element, String message) {
    if (!isString(element)) {
      throw new IllegalArgumentException(message);
    }
    return element.getAsString();
  }

  public static JsonElement nullToJsonNull(JsonElement element) {
    if (element == null) {
      return JsonNull.INSTANCE;
    }
    return element;
  }
}
