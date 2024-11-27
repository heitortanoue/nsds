package it.polimi.nsds.spark.lab.friends;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;

import static org.apache.spark.sql.functions.col;

public class FriendsComputation {
    private static final boolean useCache = true;

    public static void main(String[] args) {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./NSDS_spark_lab/";
        final String appName = useCache ? "FriendsCache" : "FriendsNoCache";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName(appName)
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> fields = new ArrayList<>();
        fields.add(DataTypes.createStructField("person", DataTypes.StringType, false));
        fields.add(DataTypes.createStructField("friend", DataTypes.StringType, false));
        final StructType schema = DataTypes.createStructType(fields);

        Dataset<Row> friends = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(schema)
                .csv(filePath + "files/friends/friends.csv");

        if (useCache) {
            friends = friends.cache();
        }

        Dataset<Row> transitiveClosure = friends;
        int distance = 0;
        final int MAX_DISTANCE = 1000;

        while (distance < MAX_DISTANCE) {
            Dataset<Row> newFriends = transitiveClosure
                    .as("tf")
                    .join(friends.as("f"), col("tf.friend").equalTo(col("f.person")))
                    .select(col("tf.person").alias("person"), col("f.friend").alias("friend"))
                    .except(transitiveClosure);

            if (newFriends.isEmpty()) {
                break;
            }

            transitiveClosure = transitiveClosure.union(newFriends).distinct();
            distance++;
        }

        transitiveClosure.orderBy("person", "friend");
        transitiveClosure.show();

        spark.close();
    }
}
