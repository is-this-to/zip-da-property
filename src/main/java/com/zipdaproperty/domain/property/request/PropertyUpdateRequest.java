package com.zipdaproperty.domain.property.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.zipdaproperty.global.id.TsidLongDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.util.List;
import java.util.Map;

public record PropertyUpdateRequest(

        @NotNull(message = "version은 필수입니다.")
        @PositiveOrZero(message = "version은 0 이상이어야 합니다.")
        Long version,

        Map<String, JsonNode> changes,

        @Size(
                min = 1,
                max = 30,
                message = "매물 이미지는 1개 이상 30개 이하로 등록해야 합니다."
        )
        @JsonDeserialize(contentUsing = TsidLongDeserializer.class)
        @JsonSerialize(contentUsing = ToStringSerializer.class)
        List<@NotNull Long> fileIds

) {

    public PropertyUpdateRequest(
            Long version,
            Map<String, JsonNode> changes
    ) {
        this(version, changes, null);
    }

    @AssertTrue(message = "수정할 필드 또는 파일 ID 목록이 필요합니다.")
    @JsonIgnore
    @Schema(hidden = true)
    public boolean isUpdateTargetProvided() {
        return (changes != null && !changes.isEmpty())
                || fileIds != null;
    }
}
