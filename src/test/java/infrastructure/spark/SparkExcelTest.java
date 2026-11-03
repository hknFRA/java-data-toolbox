package infrastructure.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;

class SparkExcelTest {

    SparkSession sparkSession = SparkCore.getUnitTestSparkSession();

    @Test
    void test_spark_should_read_from_excel() {
        // given
        String path = "src/test/resources/samples/s3.xlsx";
        String fullPath = new File(path).getAbsolutePath();
        String sheet = "Feuil1";
        String matrix = "A1:D3";

        // when
        Dataset<Row> df1 = SparkExcel.read(sparkSession, fullPath, sheet, matrix).data();
        Dataset<Row> df2 = df1.filter("ville = 'PARIS'");

        // then
        Assertions.assertThat(df1.count()).isEqualTo(2);
        Assertions.assertThat(df2.count()).isEqualTo(1);
    }

}