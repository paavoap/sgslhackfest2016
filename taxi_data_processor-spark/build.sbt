name := "TaxiDataProcessor"

version := "0.0.1"

scalaVersion := "2.10.4"

libraryDependencies ++= Seq(
    "org.apache.spark" %% "spark-core" % "1.6.0",
    "org.apache.spark" %% "spark-sql" % "1.6.0",
    "org.apache.spark" %% "spark-mllib" % "1.6.0",
    "com.databricks" %% "spark-csv" % "1.4.0"
)

resolvers += "Akka Repository" at "http://repo.akka.io/releases/"
