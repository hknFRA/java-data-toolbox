package tools.jackson;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class JacksonUtilsTest {

    record UTRecord(
            String city,
            int population
    ) {
    }

    @Test
    void test_convert_json_string_to_object() {
        // given
        String json = """
                {"city": "PARIS", "population": 1}
                """;

        // when
        UTRecord utRecord = JacksonUtils.convertJsonToObject(json, UTRecord.class);

        // then
        Assertions.assertThat(utRecord).isEqualTo(new UTRecord("PARIS", 1));
    }

}