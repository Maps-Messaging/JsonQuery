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

package io.mapsmessaging.jsonquery;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import io.mapsmessaging.jsonquery.functions.FunctionRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

class JsonQueryOptimizationTest {

  @Test
  void builtInRegistryIsCached() {
    Assertions.assertSame(FunctionRegistry.builtIns(), FunctionRegistry.builtIns());
  }

  @Test
  void compileTextProducesReusableProgram() throws Exception {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
    Function<JsonElement, JsonElement> program = compiler.compile("[\"get\",\"position\",\"latitude\"]");

    JsonElement first = JsonParser.parseString("{\"position\":{\"latitude\":51.1}}");
    JsonElement second = JsonParser.parseString("{\"position\":{\"latitude\":52.2}}");

    Assertions.assertEquals(51.1d, program.apply(first).getAsDouble());
    Assertions.assertEquals(52.2d, program.apply(second).getAsDouble());
  }

  @Test
  void compiledGetSupportsObjectAndArraySegments() throws Exception {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
    Function<JsonElement, JsonElement> program = compiler.compile("[\"get\",\"vehicles\",1,\"name\"]");

    JsonElement input = JsonParser.parseString("{\"vehicles\":[{\"name\":\"alpha\"},{\"name\":\"bravo\"}]}");

    Assertions.assertEquals("bravo", program.apply(input).getAsString());
  }

  @Test
  void compiledGetStillReturnsNullForMissingPath() throws Exception {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
    Function<JsonElement, JsonElement> program = compiler.compile("[\"get\",\"position\",\"latitude\"]");

    JsonElement input = JsonParser.parseString("{\"position\":{}}");

    Assertions.assertTrue(program.apply(input).isJsonNull());
  }
}
