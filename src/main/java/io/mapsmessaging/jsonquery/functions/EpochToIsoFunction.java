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
import com.google.gson.JsonPrimitive;
import io.mapsmessaging.jsonquery.JsonQueryCompiler;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.List;
import java.util.function.Function;

public final class EpochToIsoFunction extends AbstractFunction {

  private static final BigDecimal MILLIS_THRESHOLD = BigDecimal.valueOf(1_000_000_000_000L);
  private static final BigDecimal ONE_THOUSAND = BigDecimal.valueOf(1_000L);
  private static final DateTimeFormatter ISO_INSTANT_MILLIS =
      new DateTimeFormatterBuilder().appendInstant(3).toFormatter();

  @Override
  public String getName() {
    return "epoch_to_iso";
  }

  @Override
  public Function<JsonElement, JsonElement> compile(List<JsonElement> rawArgs, JsonQueryCompiler compiler) {
    requireArgCountExact(rawArgs, 1, "1 argument: epoch_to_iso(value)");
    Function<JsonElement, JsonElement> valueExpr = compileArg(rawArgs.get(0), compiler);

    return data -> {
      JsonElement value = valueExpr.apply(data);
      if (JsonQueryFunction.isNull(value)) {
        return JsonQueryFunction.nullValue();
      }

      long epochMillis = parseEpochMillis(value);
      return new JsonPrimitive(ISO_INSTANT_MILLIS.format(Instant.ofEpochMilli(epochMillis)));
    };
  }

  private long parseEpochMillis(JsonElement value) {
    BigDecimal numericValue = parseNumericValue(value);
    BigDecimal normalized = numericValue.abs().compareTo(MILLIS_THRESHOLD) < 0
        ? numericValue.multiply(ONE_THOUSAND)
        : numericValue;

    try {
      return normalized.setScale(0, RoundingMode.DOWN).longValueExact();
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("epoch_to_iso value is out of range", exception);
    }
  }

  private BigDecimal parseNumericValue(JsonElement value) {
    if (!value.isJsonPrimitive()) {
      throw new IllegalArgumentException("epoch_to_iso expects a numeric or numeric string value");
    }

    if (value.getAsJsonPrimitive().isNumber()) {
      return parseNumericText(value.getAsString());
    }

    if (value.getAsJsonPrimitive().isString()) {
      String text = value.getAsString().trim();
      if (text.isEmpty()) {
        throw new IllegalArgumentException("epoch_to_iso expects a numeric or numeric string value");
      }
      return parseNumericText(text);
    }

    throw new IllegalArgumentException("epoch_to_iso expects a numeric or numeric string value");
  }

  private BigDecimal parseNumericText(String text) {
    try {
      return new BigDecimal(text);
    } catch (NumberFormatException exception) {
      throw new IllegalArgumentException("epoch_to_iso expects a numeric or numeric string value", exception);
    }
  }
}
