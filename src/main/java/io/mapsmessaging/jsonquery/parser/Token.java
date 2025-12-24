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

package io.mapsmessaging.jsonquery.parser;

import java.util.Objects;

public record Token(Type type, String text, Object value, int position) {

  public Token(Type type, String text, Object value, int position) {
    this.type = Objects.requireNonNull(type, "type");
    this.text = text;
    this.value = value;
    this.position = position;
  }

  public enum Type {
    EOF,

    IDENT,
    STRING,
    NUMBER,

    DOT,
    COMMA,
    COLON,
    LPAREN,
    RPAREN,
    LBRACKET,
    RBRACKET,
    LBRACE,
    RBRACE,
    PIPE,

    PLUS,
    MINUS,
    STAR,
    SLASH,
    PERCENT,
    CARET,

    EQEQ,
    NEQ,
    LT,
    LTE,
    GT,
    GTE,

    AND,
    OR,
    IN,
    NOT,
    NULL,
    TRUE,
    FALSE
  }
}
