# Taxi Data Processor in Spark

Experiments in processing taxi data using Spark

## Dependencies

To build, [sbt](http://www.scala-sbt.org/).

To run on Bluemix, [spark-submit.sh](https://spark-service-attr-yp.ng.bluemix.net/spark_service_attr/spark-submit.sh).

## Build

    sbt package


## Run on Bluemix

Grab the `vcap.json` information from your Bluemix Spark instance, and place it into a file called `vcap.json`.

After that, you can use `spark-submit.sh` to submit the task to your Spark instance.

    spark-submit.sh --class TaxiDataProcessor --deploy-mode cluster --vcap vcap.json target/scala-2.10/taxidataprocessor_2.10-0.0.1.jar DASHDB_HOST DASHDB_USER DASHDB_PASS DASHDB_TABLE

Output of the job will be written into two files that start with `stdout_` and `stderr_`.
