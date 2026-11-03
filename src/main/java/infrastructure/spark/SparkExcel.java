package infrastructure.spark;

import org.apache.spark.sql.DataFrameReader;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;

// https://github.com/nightscape/spark-excel#create-a-dataframe-from-an-excel-file
public class SparkExcel {

    public record Info(Dataset<Row> data) {
    }

    public static SparkExcel.Info read(SparkSession spark, String path, String sheet, String matrix) {
        Dataset<Row> df = spark.read()
                .format("com.crealytics.spark.excel")
                .option("dataAddress", "'%s'!%s".formatted(sheet, matrix))
                .option("header", "true")
                .option("treatEmptyValuesAsNulls", "true")
                .load(path);
        return new Info(df);
    }

}
