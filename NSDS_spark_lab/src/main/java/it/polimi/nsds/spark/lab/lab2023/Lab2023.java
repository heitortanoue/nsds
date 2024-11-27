package it.polimi.nsds.spark;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;
import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.*;

/*
 * Group number: XX
 *
 * Group members
 *  - Student 1
 *  - Student 2
 *  - Student 3
 */

public class Lab2023 {
    private static final int numCourses = 3000;

    public static void main(String[] args) throws TimeoutException {
        final String master = args.length > 0 ? args[0] : "local[4]";
        final String filePath = args.length > 1 ? args[1] : "./";

        final SparkSession spark = SparkSession
                .builder()
                .master(master)
                .appName("SparkEval")
                .getOrCreate();
        spark.sparkContext().setLogLevel("ERROR");

        final List<StructField> profsFields = new ArrayList<>();
        profsFields.add(DataTypes.createStructField("prof_name", DataTypes.StringType, false));
        profsFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType profsSchema = DataTypes.createStructType(profsFields);

        final List<StructField> coursesFields = new ArrayList<>();
        coursesFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        coursesFields.add(DataTypes.createStructField("course_hours", DataTypes.IntegerType, false));
        coursesFields.add(DataTypes.createStructField("course_students", DataTypes.IntegerType, false));
        final StructType coursesSchema = DataTypes.createStructType(coursesFields);

        final List<StructField> videosFields = new ArrayList<>();
        videosFields.add(DataTypes.createStructField("video_id", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("video_duration", DataTypes.IntegerType, false));
        videosFields.add(DataTypes.createStructField("course_name", DataTypes.StringType, false));
        final StructType videosSchema = DataTypes.createStructType(videosFields);

        // Professors: prof_name, course_name
        final Dataset<Row> profs = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(profsSchema)
                .csv(filePath + "files/lab2023/profs.csv");

        // Courses: course_name, course_hours, course_students
        final Dataset<Row> courses = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(coursesSchema)
                .csv(filePath + "files/lab2023/courses.csv");

        // Videos: video_id, video_duration, course_name
        final Dataset<Row> videos = spark
                .read()
                .option("header", "false")
                .option("delimiter", ",")
                .schema(videosSchema)
                .csv(filePath + "files/lab2023/videos.csv");

        // Visualizations: value, timestamp
        // value represents the video id
        final Dataset<Row> visualizations = spark
                .readStream()
                .format("rate")
                .option("rowsPerSecond", 10)
                .load()
                .withColumn("value", col("value").mod(numCourses));

        /**
         * TODO: Enter your code below
         */

        /*
         * Query Q1. Compute the total number of lecture hours per prof
         */
        final Dataset<Row> q1 = profs
                .join(courses, "course_name")
                .groupBy("prof_name")
                .agg(sum("course_hours").alias("total_hours"));

//        q1.show();

        /*
         * Query Q2. For each course, compute the total number of visualizations of videos of that course,
         * computed over a minute, updated every 10 seconds
         */
        final Dataset<Row> q2Stream = visualizations
                .join(videos, visualizations.col("value").equalTo(videos.col("video_id")))
                .withColumn("timestamp", col("timestamp").cast("timestamp"))
                .groupBy(
                        window(col("timestamp"), "1 minute", "10 seconds"),
                        col("course_name"))
                .agg(sum("video_duration").alias("total_duration"))
                .select("window", "course_name", "total_duration");

//        final StreamingQuery q2 = q2Stream.writeStream()
//                .outputMode("update")
//                .format("console")
//                .option("truncate", "false")
//                .start();

        /*
         * Query Q3. For each video, compute the total number of visualizations of that video
         * with respect to the number of students in the course in which the video is used.
         */
        final Dataset<Row> q3Stream = visualizations
                .groupBy("value")
                .count()
                .withColumnRenamed("value", "video_id")
                .withColumnRenamed("count", "total_visualizations")
                .join(videos.select("video_id", "course_name"), "video_id")
                .join(courses.select("course_name", "course_students"), "course_name")
                .withColumn("visualizations_per_student",
                        col("total_visualizations").divide(col("course_students")))
                .select("video_id", "visualizations_per_student");

        final StreamingQuery q3 = q3Stream.writeStream()
                .outputMode("update")
                .format("console")
                .option("truncate", "false")
                .start();

        try {
//            q2.awaitTermination();
            q3.awaitTermination();
        } catch (final StreamingQueryException e) {
            e.printStackTrace();
        }

        spark.close();
    }
}
