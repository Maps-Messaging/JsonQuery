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
package io.mapsmessaging.jsonquery.stringify;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JsonQueryStringifier {

  private final Options options;

  public JsonQueryStringifier() {
    this(null);
  }

  public JsonQueryStringifier(Options options) {
    this.options = options == null ? new Options() : options;
  }

  public String stringify(JsonElement ast) {
    return stringifyExpr(ast, Context.TOP, 0);
  }

  public String stringify(JsonElement ast, Options options) {
    return new JsonQueryStringifier(options).stringify(ast);
  }

  private enum Context {
    TOP,
    PIPE_STAGE,
    FUNC_ARG,
    ARRAY_ELEM,
    OBJECT_VALUE,
    OP_LEFT,
    OP_RIGHT
  }

  // Precedence: bigger = binds tighter
  private static final int PREC_PIPE = 1;
  private static final int PREC_OR = 2;
  private static final int PREC_AND = 3;
  private static final int PREC_EQ = 4;
  private static final int PREC_CMP = 5;
  private static final int PREC_ADD = 6;
  private static final int PREC_MUL = 7;
  private static final int PREC_POW = 8;
  private static final int PREC_ATOM = 100;

  private static final Map<String, String> OP_TO_TOKEN = Map.ofEntries(
      Map.entry("eq", "=="),
      Map.entry("ne", "!="),
      Map.entry("lt", "<"),
      Map.entry("lte", "<="),
      Map.entry("gt", ">"),
      Map.entry("gte", ">="),
      Map.entry("add", "+"),
      Map.entry("subtract", "-"),
      Map.entry("multiply", "*"),
      Map.entry("divide", "/"),
      Map.entry("mod", "%"),
      Map.entry("pow", "^"),
      Map.entry("and", "and"),
      Map.entry("or", "or"),
      Map.entry("in", "in"),
      Map.entry("not in", "not in")
  );

  private static final Map<String, Integer> PRECEDENCE = Map.ofEntries(
      Map.entry("pipe", PREC_PIPE),
      Map.entry("or", PREC_OR),
      Map.entry("and", PREC_AND),
      Map.entry("eq", PREC_EQ),
      Map.entry("ne", PREC_EQ),
      Map.entry("lt", PREC_CMP),
      Map.entry("lte", PREC_CMP),
      Map.entry("gt", PREC_CMP),
      Map.entry("gte", PREC_CMP),
      Map.entry("add", PREC_ADD),
      Map.entry("subtract", PREC_ADD),
      Map.entry("multiply", PREC_MUL),
      Map.entry("divide", PREC_MUL),
      Map.entry("mod", PREC_MUL),
      Map.entry("pow", PREC_POW)
  );

  private static final Set<String> NON_ASSOCIATIVE =
      Set.of("pow", "eq", "ne", "lt", "lte", "gt", "gte");
  private static final Set<String> LEFT_ASSOCIATIVE =
      Set.of("add", "subtract", "multiply", "divide", "mod", "and", "or", "in", "not in");

  private static int precedenceForCall(String head) {
    return PRECEDENCE.getOrDefault(head, PREC_ATOM);
  }

  private String stringifyExpr(JsonElement element, Context context, int indentLevel) {
    if (element == null || element.isJsonNull()) {
      return "null";
    }
    if (element.isJsonPrimitive()) {
      return stringifyPrimitive(element.getAsJsonPrimitive());
    }
    if (element.isJsonObject()) {
      return stringifyObjectLiteral(element.getAsJsonObject(), context, indentLevel);
    }
    if (!element.isJsonArray()) {
      return "";
    }
    return stringifyArrayExpression(element.getAsJsonArray(), context, indentLevel);
  }

  private String stringifyArrayExpression(
      JsonArray array,
      Context context,
      int indentLevel) {

    if (array.isEmpty()) {
      return "";
    }

    String head = asString(array.get(0));
    if (head == null) {
      return "";
    }

    return switch (head) {
      case "get" -> stringifyGet(array);
      case "array" -> stringifyArrayLiteral(
          array,
          indentLevel,
          context == Context.TOP || context == Context.OBJECT_VALUE);
      case "object" -> stringifyObjectCall(array, context, indentLevel);
      case "pipe" -> stringifyPipe(array, indentLevel);
      default -> OP_TO_TOKEN.containsKey(head)
          ? stringifyOperatorCall(head, array, indentLevel)
          : stringifyFunctionCall(head, array, indentLevel);
    };
  }

  private String stringifyObjectCall(JsonArray array, Context context, int indentLevel) {
    if (array.size() < 2 || !array.get(1).isJsonObject()) {
      return "{}";
    }
    return stringifyObjectLiteral(array.get(1).getAsJsonObject(), context, indentLevel);
  }

  private String stringifyPrimitive(JsonPrimitive prim) {
    if (prim.isString()) {
      return quoteString(prim.getAsString());
    }
    if (prim.isBoolean()) {
      return prim.getAsBoolean() ? "true" : "false";
    }
    if (prim.isNumber()) {
      return prim.getAsNumber().toString();
    }
    return "null";
  }

  private String stringifyGet(JsonArray arr) {
    if (arr.size() == 1) {
      return "get()";
    }

    StringBuilder sb = new StringBuilder();
    for (int i = 1; i < arr.size(); i++) {
      sb.append('.');
      JsonElement seg = arr.get(i);

      if (seg.isJsonPrimitive() && seg.getAsJsonPrimitive().isNumber()) {
        sb.append(seg.getAsNumber().toString());
        continue;
      }

      String name = asString(seg);
      if (name == null) {
        sb.append("null");
        continue;
      }

      if (isPlainIdentifier(name)) {
        sb.append(name);
      } else {
        sb.append(quoteString(name));
      }
    }
    return sb.toString();
  }

  private String stringifyFunctionCall(String name, JsonArray array, int indentLevel) {
    List<JsonElement> arguments = tail(array);
    if (arguments.isEmpty()) {
      return name + "()";
    }

    if ("map".equals(name)
        && arguments.size() == 1
        && isObjectLiteralCall(arguments.get(0))) {
      JsonObject object = arguments.get(0).getAsJsonArray().get(1).getAsJsonObject();
      return name + "(" + stringifyObjectLiteral(object, Context.FUNC_ARG, indentLevel) + ")";
    }

    String singleLine = buildFunctionSingleLine(name, arguments, indentLevel);
    if (isWithinLineLimit(singleLine)) {
      return singleLine;
    }
    return buildFunctionMultiline(name, arguments, indentLevel);
  }

  private String buildFunctionMultiline(
      String name,
      List<JsonElement> arguments,
      int indentLevel) {

    StringBuilder result = new StringBuilder(name).append("(\n");
    String argumentIndent = indent(indentLevel + 1);

    for (int index = 0; index < arguments.size(); index++) {
      result.append(argumentIndent)
          .append(stringifyExpr(arguments.get(index), Context.FUNC_ARG, indentLevel + 1))
          .append(index < arguments.size() - 1 ? ",\n" : "\n");
    }

    return result.append(indent(indentLevel)).append(")").toString();
  }

  private String buildFunctionSingleLine(String name, List<JsonElement> args, int indentLevel) {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append("(");
    for (int i = 0; i < args.size(); i++) {
      if (i > 0) sb.append(", ");
      sb.append(stringifyExpr(args.get(i), Context.FUNC_ARG, indentLevel));
    }
    sb.append(")");
    return sb.toString();
  }

  private String stringifyPipe(JsonArray array, int indentLevel) {
    List<JsonElement> stages = tail(array);
    if (stages.isEmpty()) {
      return "";
    }
    if (stages.size() == 1) {
      return stringifyExpr(stages.get(0), Context.PIPE_STAGE, indentLevel);
    }

    boolean canUseSingleLine =
        stages.size() <= 3 && stages.stream().noneMatch(this::isObjectLiteralCall);

    if (canUseSingleLine) {
      String singleLine = buildPipeSingleLine(stages, indentLevel);
      if (isWithinLineLimit(singleLine)) {
        return singleLine;
      }
    }
    return buildPipeMultiline(stages, indentLevel);
  }

  private String buildPipeSingleLine(List<JsonElement> stages, int indentLevel) {
    StringBuilder result = new StringBuilder();
    for (JsonElement stage : stages) {
      if (!result.isEmpty()) {
        result.append(" | ");
      }
      result.append(stringifyExpr(stage, Context.PIPE_STAGE, indentLevel));
    }
    return result.toString();
  }

  private String buildPipeMultiline(List<JsonElement> stages, int indentLevel) {
    StringBuilder result = new StringBuilder(
        stringifyExpr(stages.get(0), Context.PIPE_STAGE, indentLevel));
    String pipeIndent = indent(indentLevel + 1);

    for (int index = 1; index < stages.size(); index++) {
      result.append('\n')
          .append(pipeIndent)
          .append("| ")
          .append(stringifyExpr(stages.get(index), Context.PIPE_STAGE, indentLevel + 1));
    }
    return result.toString();
  }

  private String stringifyOperatorCall(String head, JsonArray array, int indentLevel) {
    List<JsonElement> arguments = tail(array);
    if (arguments.size() < 2) {
      return "";
    }

    String token = OP_TO_TOKEN.get(head);
    int parentPrecedence = precedenceForCall(head);
    StringBuilder result = new StringBuilder();

    for (int index = 0; index < arguments.size(); index++) {
      if (index > 0) {
        result.append(' ').append(token).append(' ');
      }

      JsonElement child = arguments.get(index);
      Context childContext = index == 0 ? Context.OP_LEFT : Context.OP_RIGHT;
      String rendered = stringifyExpr(child, childContext, indentLevel);
      result.append(maybeWrapForOperator(
          head,
          parentPrecedence,
          child,
          index,
          rendered));
    }
    return result.toString();
  }

  private String maybeWrapForOperator(String parentOp, int parentPrec, JsonElement childEl, int childIndex, String rendered) {
    if (childEl == null || !childEl.isJsonArray()) {
      return rendered;
    }

    JsonArray childArr = childEl.getAsJsonArray();
    if (childArr.isEmpty()) {
      return rendered;
    }

    String childHead = asString(childArr.get(0));
    if (childHead == null) {
      return rendered;
    }

    if (!OP_TO_TOKEN.containsKey(childHead) && !"pipe".equals(childHead)) {
      return rendered;
    }

    int childPrec = precedenceForCall(childHead);

    if (childPrec < parentPrec) {
      return "(" + rendered + ")";
    }
    if (childPrec > parentPrec) {
      return rendered;
    }

    if (needsSamePrecedenceParentheses(parentOp, childHead, childIndex)
        || "pipe".equals(childHead)) {
      return "(" + rendered + ")";
    }
    return rendered;
  }

  private static boolean needsSamePrecedenceParentheses(
      String parentOp,
      String childHead,
      int childIndex) {

    return (NON_ASSOCIATIVE.contains(parentOp) && parentOp.equals(childHead))
        || (childIndex > 0 && LEFT_ASSOCIATIVE.contains(parentOp));
  }

  private String stringifyArrayLiteral(
      JsonArray array,
      int indentLevel,
      boolean forceMultiline) {

    List<JsonElement> elements = tail(array);
    if (!forceMultiline) {
      String singleLine = buildArraySingleLine(elements, indentLevel);
      if (isWithinLineLimit(singleLine)) {
        return singleLine;
      }
    }
    return buildArrayMultiline(elements, indentLevel);
  }

  private String buildArraySingleLine(List<JsonElement> elements, int indentLevel) {
    StringBuilder result = new StringBuilder("[");
    for (int index = 0; index < elements.size(); index++) {
      if (index > 0) {
        result.append(", ");
      }
      result.append(stringifyExpr(elements.get(index), Context.ARRAY_ELEM, indentLevel));
    }
    return result.append(']').toString();
  }

  private String buildArrayMultiline(List<JsonElement> elements, int indentLevel) {
    StringBuilder result = new StringBuilder("[\n");
    String elementIndent = indent(indentLevel + 1);

    for (int index = 0; index < elements.size(); index++) {
      result.append(elementIndent)
          .append(stringifyExpr(elements.get(index), Context.ARRAY_ELEM, indentLevel + 1))
          .append(index < elements.size() - 1 ? ",\n" : "\n");
    }
    return result.append(indent(indentLevel)).append(']').toString();
  }

  private String stringifyObjectLiteral(
      JsonObject object,
      Context context,
      int indentLevel) {

    boolean forceMultiline =
        context == Context.PIPE_STAGE
            || object.entrySet().stream().anyMatch(entry -> isPipeCall(entry.getValue()));

    if (!forceMultiline) {
      String singleLine = buildObjectSingleLine(object, indentLevel);
      if (singleLine != null && isWithinLineLimit(singleLine)) {
        return singleLine;
      }
    }
    return buildObjectMultiline(object, indentLevel);
  }

  private String buildObjectMultiline(JsonObject object, int indentLevel) {
    StringBuilder result = new StringBuilder("{\n");
    String keyIndent = indent(indentLevel + 1);
    int index = 0;

    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      result.append(keyIndent)
          .append(formatObjectKey(entry.getKey()))
          .append(": ")
          .append(stringifyObjectValue(entry.getValue(), indentLevel + 1))
          .append(++index < object.size() ? ",\n" : "\n");
    }

    return result.append(indent(indentLevel)).append('}').toString();
  }

  private String stringifyObjectValue(JsonElement value, int indentLevel) {
    if (isArrayLiteralCall(value)) {
      return stringifyArrayLiteral(value.getAsJsonArray(), indentLevel, true);
    }
    return stringifyExpr(value, Context.OBJECT_VALUE, indentLevel);
  }

  private String buildObjectSingleLine(JsonObject object, int indentLevel) {
    StringBuilder result = new StringBuilder("{ ");
    int index = 0;

    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      if (isPipeCall(entry.getValue())) {
        return null;
      }

      String renderedValue = isArrayLiteralCall(entry.getValue())
          ? stringifyArrayLiteral(entry.getValue().getAsJsonArray(), indentLevel, false)
          : stringifyExpr(entry.getValue(), Context.OBJECT_VALUE, indentLevel);

      if (renderedValue.indexOf('\n') >= 0) {
        return null;
      }

      if (index++ > 0) {
        result.append(", ");
      }
      result.append(formatObjectKey(entry.getKey()))
          .append(": ")
          .append(renderedValue);
    }

    return result.append(" }").toString();
  }

  private boolean isPipeCall(JsonElement element) {
    return isCall(element, "pipe", 1);
  }

  private boolean isArrayLiteralCall(JsonElement element) {
    return isCall(element, "array", 1);
  }

  private boolean isObjectLiteralCall(JsonElement element) {
    if (!isCall(element, "object", 2)) {
      return false;
    }
    return element.getAsJsonArray().get(1).isJsonObject();
  }

  private static boolean isCall(JsonElement element, String name, int minimumSize) {
    if (element == null || !element.isJsonArray()) {
      return false;
    }

    JsonArray array = element.getAsJsonArray();
    return array.size() >= minimumSize && name.equals(asString(array.get(0)));
  }

  private String formatObjectKey(String key) {
    if (isPlainIdentifier(key)) {
      return key;
    }
    return quoteString(key);
  }

  private static String asString(JsonElement el) {
    if (el == null || el.isJsonNull() || !el.isJsonPrimitive()) {
      return null;
    }
    JsonPrimitive p = el.getAsJsonPrimitive();
    if (!p.isString()) {
      return null;
    }
    return p.getAsString();
  }

  private static boolean isPlainIdentifier(String s) {
    if (s == null || s.isEmpty()) {
      return false;
    }
    char first = s.charAt(0);
    if (!isIdentStart(first)) {
      return false;
    }
    for (int i = 1; i < s.length(); i++) {
      if (!isIdentPart(s.charAt(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean isIdentStart(char ch) {
    return (ch >= 'A' && ch <= 'Z')
        || (ch >= 'a' && ch <= 'z')
        || ch == '_' || ch == '$';
  }

  private static boolean isIdentPart(char ch) {
    return isIdentStart(ch) || (ch >= '0' && ch <= '9');
  }

  private String quoteString(String value) {
    if (value == null) {
      return "null";
    }

    StringBuilder result = new StringBuilder().append('"');
    for (int index = 0; index < value.length(); index++) {
      appendEscaped(result, value.charAt(index));
    }
    return result.append('"').toString();
  }

  private static void appendEscaped(StringBuilder result, char value) {
    switch (value) {
      case '\\' -> result.append("\\\\");
      case '"' -> result.append("\\\"");
      case '\n' -> result.append("\\n");
      case '\r' -> result.append("\\r");
      case '\t' -> result.append("\\t");
      default -> result.append(value);
    }
  }

  private static List<JsonElement> tail(JsonArray array) {
    List<JsonElement> values = new ArrayList<>(Math.max(0, array.size() - 1));
    for (int index = 1; index < array.size(); index++) {
      values.add(array.get(index));
    }
    return values;
  }

  private boolean isWithinLineLimit(String value) {
    return value.indexOf('\n') < 0 && value.length() <= options.getMaxLineLength();
  }

  private String indent(int level) {
    if (level <= 0) {
      return "";
    }
    return options.getIndentation().repeat(level);
  }
}
