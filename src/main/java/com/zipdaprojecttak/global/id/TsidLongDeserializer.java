package com.zipdaprojecttak.global.id;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class TsidLongDeserializer extends StdDeserializer<Long> {

    public TsidLongDeserializer() {
        super(Long.class);
    }

    @Override
    public Long deserialize(
            JsonParser jsonParser,
            DeserializationContext deserializationContext
    ) throws JacksonException {
        if (jsonParser.currentToken() != JsonToken.VALUE_STRING) {
            return (Long) deserializationContext.handleUnexpectedToken(
                    Long.class,
                    jsonParser
            );
        }

        String value = jsonParser
                .getString()
                .trim();

        if (value.isEmpty()) {
            return (Long) deserializationContext.handleWeirdStringValue(
                    Long.class,
                    value,
                    "TSID는 비어 있을 수 없습니다."
            );
        }

        try {
            Long tsid = Long.valueOf(value);

            if (tsid <= 0) {
                return (Long) deserializationContext.handleWeirdStringValue(
                        Long.class,
                        value,
                        "TSID는 0보다 큰 정수여야 합니다."
                );
            }

            return tsid;
        } catch (NumberFormatException exception) {
            return (Long) deserializationContext.handleWeirdStringValue(
                    Long.class,
                    value,
                    "TSID는 Long 범위의 숫자 문자열이어야 합니다."
            );
        }
    }
}