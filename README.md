# Gson JSON Query Engine

A **small, embeddable JSON query engine** for Java, built on **Gson** and designed for **high‑throughput systems** such as MAPS Messaging.

This implementation is inspired by [jsonquerylang](https://github.com/jsonquerylang/jsonquery), but is intentionally constrained:
- No mutation
- No scripting
- No runtime eval
- Compile once, execute many

Filtering is powered by **MAPS’ JMS selector parser**, giving you a proven predicate language instead of inventing yet another one.

---

## Features

- Gson‑native (`JsonElement` everywhere)
- Compile JSON query ASTs into executable functions
- Stable sorting
- Selector‑based filtering (JMS selectors)
- Deterministic, side‑effect‑free execution
- Pluggable function registry

Supported functions:
- `get`
- `filter`
- `sort`
- `map`
- `pick`
- `pipe`

---

## Basic Usage

### 1. Create a compiler

```java
JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
```

### 2. Compile a query AST

```java
JsonElement query = JsonParser.parseString("""
["pipe",
  ["filter", "address.state = 'Alaska'"],
  ["sort", ["get","age"], "desc"],
  ["pick", "first", "age"]
]
""");

Function<JsonElement, JsonElement> program = compiler.compile(query);
```

### 3. Execute against JSON data

```java
JsonElement input = JsonParser.parseString("""
[
  {"first":"Chris","age":23,"address":{"state":"Alaska"}},
  {"first":"Joe","age":32,"address":{"state":"Alaska"}},
  {"first":"Emily","age":19,"address":{"state":"Texas"}}
]
""");

JsonElement result = program.apply(input);
```

---

## Query Model

Queries are expressed as **JSON ASTs**, not strings.

### `get`

```json
["get", "age"]
```

### `filter` (JMS selector)

```json
["filter", "address.state = 'Alaska'"]
```

### `sort`

```json
["sort", ["get","age"], "desc"]
```

### `map`

```json
["map", ["get","age"]]
```

### `pick`

```json
["pick", "name", "age"]
```

### `pipe`

```json
["pipe",
  ["filter", "state = 'Alaska'"],
  ["sort", ["get","age"], "desc"],
  ["pick", "name", "age"]
]
```

---

## Filtering with JMS Selectors

Filtering uses the MAPS **JMS selector parser**, so predicates like these are supported:

```text
state = 'Alaska'
age >= 21
address.state IN ('Alaska','Texas')
```

Selectors are **compiled once** and evaluated per element.

---

## Extending with Custom Functions

Add your own functions without touching the compiler:

```java
Map<String, JsonQueryFunction> custom = new HashMap<>();
custom.put("myFn", new MyFunction());

JsonQueryCompiler compiler =
    JsonQueryCompiler.create(FunctionRegistry.builtIns(),
                             new FunctionRegistry(custom));
```

Each function lives in its own class and implements:

```java
JsonQueryFunction
```

---

## Design Goals

- Predictable execution
- Explicit null handling
- Zero reflection
- No hidden magic
- Broker‑safe performance characteristics

This is a **query/filter engine**, not a programming language.

## Java API Examples

All examples use the **public JsonQuery API**:
- `JsonQueryParser`
- `JsonQueryCompiler`
- `Function<JsonElement, JsonElement>`

They show **exactly how users write Java**, not JSON ASTs by hand.

---

### Common Setup

```java
JsonQueryCompiler compiler = JsonQueryCompiler.createDefault();
```

---

### `get`

#### Example: read a field from an object

```java
JsonElement ast = JsonQueryParser.parse(".age");
Function<JsonElement, JsonElement> program = compiler.compile(ast);

JsonElement input = JsonParser.parseString(
    "{ "name": "Chris", "age": 23 }"
);

JsonElement result = program.apply(input);
```

**Result**
```json
23
```

---

### `filter`

Filters an array using a JMS selector.

```java
JsonElement ast = JsonQueryParser.parse(
    "filter("address.state = 'Alaska' and age >= 21")"
);

Function<JsonElement, JsonElement> program = compiler.compile(ast);

JsonElement input = JsonParser.parseString(
    """
    [
      {"name":"Chris","age":23,"address":{"state":"Alaska"}},
      {"name":"Joe","age":19,"address":{"state":"Alaska"}},
      {"name":"Emily","age":32,"address":{"state":"Texas"}}
    ]
    """
);

JsonElement result = program.apply(input);
```

---

### `sort`

```java
JsonElement ast = JsonQueryParser.parse(
    "sort(.age, "desc")"
);

Function<JsonElement, JsonElement> program = compiler.compile(ast);

JsonElement input = JsonParser.parseString(
    """
    [
      {"name":"Chris","age":23},
      {"name":"Joe","age":32},
      {"name":"Emily","age":19}
    ]
    """
);

JsonElement result = program.apply(input);
```

---

### `map`

```java
JsonElement ast = JsonQueryParser.parse(
    "map(.age)"
);

Function<JsonElement, JsonElement> program = compiler.compile(ast);

JsonElement input = JsonParser.parseString(
    """
    [
      {"name":"Chris","age":23},
      {"name":"Joe","age":32},
      {"name":"Emily","age":19}
    ]
    """
);

JsonElement result = program.apply(input);
```

---

### `pick`

```java
JsonElement ast = JsonQueryParser.parse(
    "pick("timestamp", "particles_gt_10")"
);

Function<JsonElement, JsonElement> program = compiler.compile(ast);

JsonElement input = JsonParser.parseString(
    """
    {
      "timestamp": "2026-01-31T12:00:00Z",
      "particles_gt_10": 742,
      "particles_gt_2_5": 1200
    }
    """
);

JsonElement result = program.apply(input);
```

---

### `pipe`

End-to-end example.

```java
JsonElement ast = JsonQueryParser.parse(
    """
    filter("address.state = 'Alaska'")
      | sort(.age, "desc")
      | pick("name", "age")
    """
);

Function<JsonElement, JsonElement> program = compiler.compile(ast);

JsonElement input = JsonParser.parseString(
    """
    [
      {"name":"Chris","age":23,"address":{"state":"Alaska"}},
      {"name":"Joe","age":32,"address":{"state":"Alaska"}},
      {"name":"Emily","age":19,"address":{"state":"Texas"}}
    ]
    """
);

JsonElement result = program.apply(input);
```

---

### Reuse the Compiled Query

```java
Function<JsonElement, JsonElement> program =
    compiler.compile(JsonQueryParser.parse("map(.age)"));

JsonElement out1 = program.apply(data1);
JsonElement out2 = program.apply(data2);
JsonElement out3 = program.apply(data3);
```

Compiled queries are immutable, thread-safe, and cheap to execute.


---

## License

Apache License 2.0 with Commons Clause

See `LICENSE` for details.

