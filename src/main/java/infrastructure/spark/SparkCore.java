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

    /// * warehouse (aka datastore)
    /// contains data files
    /// not use spark default 'spark-warehouse'
    /// avoid conflicts with default values and in-memory catalog who use default spark-warehouse
    ///
    /// * metastore
    /// contains data localization to tell where are data files to spark session
    /// default : metastore_db
    public static SparkSession getUnitTestSparkSessionWithHive() {
        log.info("start unit test spark session with hive");
        return SparkSession.builder()
                .appName("unit-test-spark-session-hive")
                .master("local[1]")
                .enableHiveSupport()
                .config("spark.sql.warehouse.dir", "./hive-warehouse")
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
