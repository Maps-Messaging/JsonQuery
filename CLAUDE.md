# CLAUDE.md

Guide for AI assistants working on the JsonQuery codebase.

## Project Overview

JsonQuery is a small, embeddable JSON query engine for Java built on Google Gson. It compiles JSON AST queries into reusable `Function<JsonElement, JsonElement>` instances following a compile-once-execute-many pattern. Designed for high-throughput systems (MAPS Messaging), it is intentionally constrained: no mutation, no scripting, no runtime eval, no reflection.

- **Group/Artifact:** `io.mapsmessaging:JsonQuery:1.0.0`
- **Java version:** 17 (source and target)
- **License:** Apache 2.0 with Commons Clause
- **Spec compliance:** [jsonquerylang v5.0.0](https://github.com/jsonquerylang/jsonquery)

## Build Commands

```bash
# Compile and run tests (snapshot profile, skip GPG)
mvn -Dgpg.skip=true clean test -Psnapshot

# Full build with deploy (snapshot)
mvn -Dgpg.skip=true clean deploy -Psnapshot -U

# Release build (skips tests, requires GPG)
mvn -DskipTests=true clean deploy -Prelease

# Run SonarCloud analysis
mvn sonar:sonar -Dsonar.login=${SONAR_TOKEN}

# Run just tests
mvn -Dgpg.skip=true test -Psnapshot
```

**Important:** Always use `-Dgpg.skip=true` for local builds unless GPG keys are configured. Always specify a profile (`-Psnapshot` or `-Prelease`) since the `jms_selector_parser` dependency version is profile-specific.

## Project Structure

```
src/main/java/io/mapsmessaging/jsonquery/
  JsonQueryCompiler.java          # Entry point: compiles JSON AST -> Function
  JsonQueryParser.java            # Parses query strings into AST
  functions/
    JsonQueryFunction.java        # Core interface all functions implement
    FunctionRegistry.java         # ServiceLoader-based function discovery
    AbstractFunction.java         # Base class with validation helpers
    GetFunction.java              # Property/array access
    FilterFunction.java           # Filter by compiled predicate
    FilterSelectorFunction.java   # JMS selector-based filtering ("selector")
    SortFunction.java             # Stable sort
    PipeFunction.java             # Function composition
    PickFunction.java             # Field projection
    MapFunction.java              # Element transformation
    ArrayFunction.java            # Array construction
    IfFunction.java               # Conditional logic
    ObjectFunction.java           # Object construction
    numeric/                      # Arithmetic: add, subtract, multiply, divide,
                                  #   mod, pow, abs, round, min, max, sum, average, prod
    binary/                       # Comparisons: eq, ne, lt, lte, gt, gte
    logic/                        # Boolean: and, or
    matcher/                      # Regex: match, matchAll
    parser/                       # Tokenizer and parse error types

src/main/resources/META-INF/services/
  io.mapsmessaging.jsonquery.functions.JsonQueryFunction
                                  # SPI service file listing all 60+ function classes

src/test/java/io/mapsmessaging/jsonquery/
  JsonQueryCompilerTest.java      # Unit tests for compiler
  JsonQueryConformanceTest.java   # Spec conformance tests (@TestFactory, dynamic)
  JsonQueryParseConformanceTest.java  # Parser conformance tests

src/test/resources/jsonquery/
  compile.test.json               # Conformance test data (137 KB)
  parse.test.json                 # Parse conformance test data (38 KB)
```

## Architecture

### Compilation Pipeline

1. A JSON AST (e.g. `["pipe", ["filter", "age > 21"], ["get", "name"]]`) is passed to `JsonQueryCompiler.compile()`
2. The compiler resolves the function name from `FunctionRegistry`
3. Each `JsonQueryFunction.compile()` receives raw args and produces a `Function<JsonElement, JsonElement>`
4. Compiled functions are stateless, thread-safe, and reusable

### Function System (SPI)

All functions implement `JsonQueryFunction` with two methods:
- `String getName()` - the function name used in query ASTs
- `Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler)` - compile-time processing

Functions are registered via Java `ServiceLoader`. The SPI file at `META-INF/services/io.mapsmessaging.jsonquery.functions.JsonQueryFunction` must list every function class.

### Key Design Constraints

- **No mutation:** Input data is never modified
- **No reflection:** Zero use of reflection anywhere
- **Null handling:** `JsonNull.INSTANCE` is used as the null sentinel; functions handle nulls explicitly
- **Immutability:** `FunctionRegistry` wraps maps in `Collections.unmodifiableMap()`
- **Thread safety:** Compiled functions capture state in closures with no shared mutable state

## Adding a New Function

1. Create a class implementing `JsonQueryFunction` in the appropriate subpackage
2. Implement `getName()` returning the query function name
3. Implement `compile()` returning a `Function<JsonElement, JsonElement>`
4. Add the fully qualified class name to `src/main/resources/META-INF/services/io.mapsmessaging.jsonquery.functions.JsonQueryFunction`
5. Add tests (conformance test JSON or unit tests)

## Testing

- **Framework:** JUnit 5 (Jupiter 6.0.1)
- **Test patterns:** `**/*Test.java` and `**/*IT.java`
- **Conformance tests** use `@TestFactory` for dynamic test generation from JSON spec files
- **Coverage:** JaCoCo generates reports at `target/site/jacoco`, tracked via SonarCloud

Run tests:
```bash
mvn -Dgpg.skip=true test -Psnapshot
```

## Code Conventions

- **Formatting:** Standard Java conventions; consistent indentation
- **Naming:** PascalCase classes, camelCase methods, function classes named `<Name>Function`
- **License header:** All source files must include the Apache 2.0 + Commons Clause header (Copyright 2020-2024 Matthew Buckton / 2024-2025 MapsMessaging B.V.)
- **Error handling:** `IllegalArgumentException` for compile-time errors; no checked exceptions
- **Null pattern:** Use `JsonNull.INSTANCE` (never Java `null` in return values from functions)
- **Static helpers:** `JsonQueryFunction` interface provides static utilities (`isNull`, `isTruthy`, `numberValue`, `booleanValue`, `compileArg`, etc.)

## Commit Message Convention

This project follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(<scope>): <subject>
```

Allowed types: `feat`, `fix`, `refactor`, `perf`, `test`, `build`, `ci`, `docs`, `style`, `chore`, `revert`

- Use imperative mood ("add", not "added")
- Keep header under 100 characters
- Include `JIRA: MAPS-###` or `NO-ISSUE` in the footer

## CI/CD

- **Platform:** Buildkite
- **Snapshot pipeline** (`.buildkite/pipeline.yml`): builds with `-Psnapshot`, runs SonarCloud analysis, uses `raspberrypi_build_agent` queue
- **Release pipeline** (`.buildkite/pipeline_release.yml`): builds with `-Prelease`, skips tests, deploys to Sonatype Central, uses `java_build_queue`
- **Quality gates:** JaCoCo coverage, SonarCloud analysis, OWASP dependency-check (fails on CVSS 10), CycloneDX SBOM generation

## Dependencies

| Dependency | Version | Scope | Purpose |
|---|---|---|---|
| Gson | 2.13.2 | compile | JSON processing (`JsonElement` tree model) |
| jms_selector_parser | 2.1.0 (release) / 2.1.1-SNAPSHOT (snapshot) | compile | JMS selector parsing for `FilterSelectorFunction` |
| Lombok | 1.18.42 | provided | Annotation processing |
| JUnit Jupiter | 6.0.1 | test | Testing framework |

## Common Pitfalls

- Forgetting to add new function classes to the SPI service file will cause them to be invisible at runtime
- The `jms_selector_parser` dependency version differs between profiles; always specify `-Psnapshot` or `-Prelease`
- GPG signing is enabled by default; use `-Dgpg.skip=true` for local development
- The `selector` function name (not `filter`) is used for JMS selector-based filtering in queries
- Conformance test JSON files are large; changes to function behavior should be validated against them
