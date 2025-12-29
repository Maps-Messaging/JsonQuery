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

package io.mapsmessaging.jsonquery;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.function.Function;

class JsonQueryCompilerTest {

  @Test
  void pipeFilterSortPick() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        [
          {"first":"Chris","last":"A","age":23,"address":{"state":"Alaska","city":"Anchorage"}},
          {"first":"Joe","last":"B","age":32,"address":{"state":"Alaska","city":"Juneau"}},
          {"first":"Emily","last":"C","age":19,"address":{"state":"Texas","city":"Austin"}},
          {"first":"Pat","last":"D","age":40,"address":{"state":"Alaska","city":"Fairbanks"}}
        ]
        """);

    JsonElement query = JsonParser.parseString("""
        ["pipe",
          ["selector", "address.state = 'Alaska'"],
          ["sort", ["get","age"], "desc"],
          ["pick", "first", "age", "address"]
        ]
        """);

    Function<JsonElement, JsonElement> program = compiler.compile(query);
    JsonElement result = program.apply(data);

    JsonElement expected = JsonParser.parseString("""
        [
          {"first":"Pat","age":40,"address":{"state":"Alaska","city":"Fairbanks"}},
          {"first":"Joe","age":32,"address":{"state":"Alaska","city":"Juneau"}},
          {"first":"Chris","age":23,"address":{"state":"Alaska","city":"Anchorage"}}
        ]
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void filterSkipsNonObjects() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        [
          {"state":"Alaska","age":10},
          123,
          "x",
          {"state":"Texas","age":11},
          null,
          {"state":"Alaska","age":12}
        ]
        """);

    JsonElement query = JsonParser.parseString("""
        ["selector", "state = 'Alaska'"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    JsonElement expected = JsonParser.parseString("""
        [
          {"state":"Alaska","age":10},
          {"state":"Alaska","age":12}
        ]
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void sortByGetDescIsStable() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        [
          {"name":"A","age":10},
          {"name":"B","age":10},
          {"name":"C","age":9}
        ]
        """);

    JsonElement query = JsonParser.parseString("""
        ["sort", ["get","age"], "desc"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    JsonElement expected = JsonParser.parseString("""
        [
          {"name":"A","age":10},
          {"name":"B","age":10},
          {"name":"C","age":9}
        ]
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void mapGetAge() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        [
          {"name":"Chris","age":23},
          {"name":"Joe","age":32},
          {"name":"Emily","age":19}
        ]
        """);

    JsonElement query = JsonParser.parseString("""
        ["map", ["get","age"]]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    JsonElement expected = JsonParser.parseString("""
        [23,32,19]
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void pickOnSingleObject() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {"name":"Chris","age":23,"scores":[7.2,5,8.0]}
        """);

    JsonElement query = JsonParser.parseString("""
        ["pick", "name", "age"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    JsonElement expected = JsonParser.parseString("""
        {"name":"Chris","age":23}
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void getMissingReturnsNull() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {"name":"Chris"}
        """);

    JsonElement query = JsonParser.parseString("""
        ["get", "age"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    Assertions.assertTrue(result.isJsonNull());
  }



  @Test
  void filterNonArrayReturnsInput() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {"state":"Alaska"}
        """);

    JsonElement query = JsonParser.parseString("""
        ["selector", "state = 'Alaska'"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    Assertions.assertEquals(data, result);
  }

  @Test
  void pickArrayDropsMissingFields() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        [
          {"name":"A","age":1},
          {"name":"B"},
          {"age":3}
        ]
        """);

    JsonElement query = JsonParser.parseString("""
        ["pick", "name", "age"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    JsonElement expected = JsonParser.parseString("""
        [
          {"name":"A","age":1},
          {"name":"B"},
          {"age":3}
        ]
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void filterOnSingleObjectDropsOrPasses() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {
          "pm_1_0": 0,
          "timestamp": "2025-12-23T05:37:11.757186593Z",
          "pm_2_5": 0,
          "pm_10": 0,
          "pm_1_0_atm": 0,
          "pm_2_5_atm": 0,
          "pm_10_atm": 0,
          "particles_gt_3": 108,
          "particles_gt_5": 36,
          "particles_gt_10": 4,
          "particles_gt_25": 2,
          "particles_gt_50": 2,
          "particles_gt_100": 0
        }
        """);

    JsonElement query = JsonParser.parseString("""
        ["pipe",
          ["selector", "particles_gt_10 > 100"],
          ["pick", "timestamp", "particles_gt_10", "pm_2_5"]
        ]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    Assertions.assertTrue(result.isJsonNull());
  }

  @Test
  void filterOnSingleObjectPassesAndPicks() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {
          "pm_1_0": 0,
          "timestamp": "2025-12-23T05:37:11.757186593Z",
          "pm_2_5": 0,
          "pm_10": 0,
          "pm_1_0_atm": 0,
          "pm_2_5_atm": 0,
          "pm_10_atm": 0,
          "particles_gt_3": 108,
          "particles_gt_5": 36,
          "particles_gt_10": 4,
          "particles_gt_25": 2,
          "particles_gt_50": 2,
          "particles_gt_100": 0
        }
        """);

    JsonElement query = JsonParser.parseString("""
        ["pipe",
          ["selector", "particles_gt_10 < 10"],
          ["pick", "timestamp", "particles_gt_10", "pm_2_5"]
        ]
        """);

    Function<JsonElement, JsonElement> compiled = compiler.compile(query);
    JsonElement result = compiled.apply(data);

    JsonElement expected = JsonParser.parseString("""
        {
          "timestamp": "2025-12-23T05:37:11.757186593Z",
          "particles_gt_10": 4,
          "pm_2_5": 0
        }
        """);

    Assertions.assertEquals(expected, result);
  }

  @Test
  void parseHumanReadablePipelineQuery() throws Exception {
    String query = """
        .friends
          | filter(.city == "New York")
          | sort(.age)
          | pick(.name, .age)
        """;

    JsonElement ast = JsonQueryParser.parse(query);

    JsonElement expected = JsonParser.parseString("""
        [
          "pipe",
          ["get","friends"],
          ["filter", ["eq", ["get","city"], "New York"]],
          ["sort", ["get","age"]],
          ["pick", ["get","name"], ["get","age"]]
        ]
        """);

    Assertions.assertEquals(expected, ast);

    Assertions.assertDoesNotThrow(() ->
        JsonQueryCompiler.createDefault().compile(ast)
    );
  }

}
