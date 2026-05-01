package spark;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class SparkPgTest {

    @Test
    void test() {
        // given
        int a = 12;
        int b = 23;

        // when
        int c = a + b;

        // then
        Assertions.assertThat(c).isEqualTo(35);
    }

}