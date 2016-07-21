# Taxi Data Processor in Spark

Experiments in processing taxi data using Spark

## Dependencies

To build, [sbt](http://www.scala-sbt.org/).

To run on Bluemix, [spark-submit.sh](https://spark-service-attr-yp.ng.bluemix.net/spark_service_attr/spark-submit.sh).

## Build

    sbt package


## Run on Bluemix Using Data from dashDB

Grab the `vcap.json` information from your Bluemix Spark instance, and place it into a file called `vcap.json`.

After that, you can use `spark-submit.sh` to submit the task to your Spark instance.

    spark-submit.sh --class TaxiDataProcessor --deploy-mode cluster --vcap vcap.json target/scala-2.10/taxidataprocessor_2.10-0.0.1.jar bluemix DASHDB_HOST DASHDB_USER DASHDB_PASS DASHDB_TABLE

Output of the job will be written into two files that start with `stdout_` and `stderr_`.

## Run Locally Using CSV Data

You can also run the processor locally on data in a CSV file.

The CSV file format is the following. No header.

```
2016-06-22 19:54:42,+08:00,103.62232,1.27534
2016-06-22 19:54:42,+08:00,103.62567,1.29112
2016-06-22 19:54:42,+08:00,103.6301,1.274934
```

You use the `spark-submit` from your local Spark install. You need to tell it to use the `spark-csv` package from Databricks, and give it the path to the CSV file you want to use.

    spark-submit --packages com.databricks:spark-csv_2.10:1.4.0 target/scala-2.10/taxidataprocessor_2.10-0.0.1.jar local test/taxi.csv
