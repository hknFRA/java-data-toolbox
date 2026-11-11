package infrastructure.spark;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkCore {


    private static final Logger log = LoggerFactory.getLogger(SparkCore.class);

    public static SparkSession getUnitTestSparkSession() {
        log.info("start unit test spark session");
        return SparkSession.builder()
                .appName("unit-test-spark-session")
                .master("local[1]")
                .getOrCreate();
    }

    public static Object getColumnValue(Dataset<Row> df, String columnName) {
        return df.selectExpr(columnName)
                .limit(1)
                .collectAsList()
                .getFirst()
                .get(0);
    }


}
