package io.mapsmessaging.jsonquery;

import com.google.gson.JsonArray;
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
          ["filter", "address.state = 'Alaska'"],
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
        ["filter", "state = 'Alaska'"]
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
  void pipeEmptyIsRejected() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement query = JsonParser.parseString("""
        ["pipe"]
        """);

    Assertions.assertThrows(IllegalArgumentException.class, () -> compiler.compile(query));
  }

  @Test
  void sortNonArrayReturnsInput() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {"age": 10}
        """);

    JsonElement query = JsonParser.parseString("""
        ["sort", ["get","age"], "desc"]
        """);

    JsonElement result = compiler.compile(query).apply(data);

    Assertions.assertEquals(data, result);
  }

  @Test
  void filterNonArrayReturnsInput() {
    JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();

    JsonElement data = JsonParser.parseString("""
        {"state":"Alaska"}
        """);

    JsonElement query = JsonParser.parseString("""
        ["filter", "state = 'Alaska'"]
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
}
