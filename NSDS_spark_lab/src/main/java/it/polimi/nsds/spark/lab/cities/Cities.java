package it.polimi.nsds.spark.lab.cities;

import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

public class Cities {
    public static void main(String[] args) throws TimeoutException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName("SparkEval")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> citiesRegionsFields = new ArrayList<>();
        citiesRegionsFields.add(DataTypes.createStructField("city", DataTypes.StringType, false));
        citiesRegionsFields.add(DataTypes.createStructField("region", DataTypes.StringType, false));
        final StructType citiesRegionsSchema = DataTypes.createStructType(citiesRegionsFields);

        final List<StructField> citiesPopulationFields = new ArrayList<>();
        citiesPopulationFields.add(DataTypes.createStructField("id", DataTypes.IntegerType, false));
        citiesPopulationFields.add(DataTypes.createStructField("city", DataTypes.StringType, false));
        citiesPopulationFields.add(DataTypes.createStructField("population", DataTypes.IntegerType, false));
        final StructType citiesPopulationSchema = DataTypes.createStructType(citiesPopulationFields);

        final Dataset<Row> citiesPopulation = spark
                .read()
                .option("header", "true")
                .option("delimiter", ";")
                .schema(citiesPopulationSchema)
                .csv(filePath + "files/cities/cities_population.csv");

        final Dataset<Row> citiesRegions = spark
                .read()
                .option("header", "true")
                .option("delimiter", ";")
                .schema(citiesRegionsSchema)
                .csv(filePath + "files/cities/cities_regions.csv");

        final Dataset<Row> q = citiesPopulation
                .join(citiesRegions, "city")
                .select("region", "city", "population");
        q.cache();

        final Dataset<Row> q1 = q
                .groupBy(col("region"))
                .sum()
                .withColumnRenamed("sum(population)", "population")
                .orderBy(col("population").desc());
//        q1.show();

        Dataset<Row> q2 = q.groupBy("region")
                .agg(
                        count("*").alias("number_of_cities"),
                        max("population").alias("most_populated_city_population")
                );
//        q2.show();

        // JavaRDD where each element is an integer and represents the population of a city
        int years = 0;
        JavaRDD<Integer> population = citiesPopulation.toJavaRDD().map(r -> r.getInt(2));
        int sum = sumPopulation(population);
        while (sum < 100_000_000){
            population = calculatePopulation(population);
            sum = sumPopulation(population);
            years++;
        }
        System.out.println("It takes " + years + "years to achieve 100M population in Italy");

        // Bookings: the value represents the city of the booking
        final Dataset<Row> bookings = spark
                .readStream()
                .format("rate")
                .option("rowsPerSecond", 100)
                .load();

        final StreamingQuery q4 = null; // TODO query Q4

        try {
            q4.awaitTermination();
        } catch (final StreamingQueryException e) {
            e.printStackTrace();
        }

        spark.close();
    }

    static JavaRDD<Integer> calculatePopulation(JavaRDD<Integer> r) {
        return r.map(p -> {
            if (p > 1000) {
                return (int) Math.round(p * 1.01);
            }
            if (p < 1000) {
                return (int) Math.round(p * 0.99);
            }

            return p;
        });
    }

    static Integer sumPopulation(JavaRDD<Integer> r) {
        return r.reduce(Integer::sum);
    }
}