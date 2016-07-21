
import org.apache.spark._
import org.apache.spark.mllib.classification.{LogisticRegressionWithLBFGS, LogisticRegressionModel}
import org.apache.spark.mllib.evaluation.MulticlassMetrics
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.functions._

object TaxiDataProcessor {
  def loadFromDashDB(sqlContext: SQLContext, args: Array[String]) = {
    val host = args(0)
    val user = args(1)
    val pass = args(2)
    val url = s"jdbc:db2://${host}:50000/BLUDB:user=${user};password=${pass};"

    val table = args(3)

    sqlContext.load("jdbc", Map(
        "url" -> url,
        "dbtable" -> table))
  }

  def loadFromCSV(sqlContext: SQLContext, args: Array[String]) = {
    val path = args(0)

    val df = sqlContext.read
      .format("com.databricks.spark.csv")
      .option("header", "false") // Use first line of all files as header
      .option("inferSchema", "true") // Automatically infer data types
      .load(path)

    df.withColumnRenamed("C0", "TIMESTAMP")
      .withColumnRenamed("C1", "TIMEZONE")
      .withColumnRenamed("C2", "LAT")
      .withColumnRenamed("C3", "LNG")
  }

  def splitTimestamp(c: Column): Column = {
    (hour(c) * 60) + minute(c)
  }

  def splitTimestampColumn(df: DataFrame): DataFrame = {
    df.withColumn("timeofday", splitTimestamp(df("TIMESTAMP")))
  }

  val createVector = udf[Vector, Int, Double, Double] { (tod, lng, lat) =>
    Vectors.dense(tod, lng, lat)
  }

  def createFeatures(df: DataFrame): DataFrame = {
    df.withColumn("features", createVector(df("timeofday"), df("LAT"), df("LNG")))
  }

  def toLabeledPointRDD(df: DataFrame): RDD[LabeledPoint] = {
    val r_rdd = df.rdd
    r_rdd.map[LabeledPoint] { row =>
      val fs = row.getAs[Vector](6)
      val l = row.getDouble(5)
      new LabeledPoint(l, fs)
    }
  }

  def processData(loader: (SQLContext, Array[String]) => DataFrame, sqlContext: SQLContext, args: Array[String]) {
    val dashdata = loader(sqlContext, args)

    //dashdata.registerTempTable("taxidata")

    dashdata.printSchema
    dashdata.show(3)

    //dashdata.groupBy("TIMESTAMP").count().orderBy(desc("count")).show()

    val withTime = splitTimestampColumn(dashdata)

    withTime.printSchema
    withTime.show(3)

    val labeled = withTime.withColumn("label", lit(1.0))

    labeled.printSchema
    labeled.show(3)

    val vectorized = createFeatures(labeled)

    vectorized.printSchema
    vectorized.show(3)

    val rdd = toLabeledPointRDD(vectorized)
    //val sc = sqlContext.sparkContext
    //val rdd = sc.emptyRDD[LabeledPoint]

    val splits = rdd.randomSplit(Array(0.8, 0.2))
    val training = splits(0).cache()
    val testing = splits(1)

    val model = new LogisticRegressionWithLBFGS()
      .setNumClasses(2)
      .run(training)

    val pAndLs = testing.map { case LabeledPoint(l, fs) =>
      val p = model.predict(fs)
      (p, l)
    }

    val ms = new MulticlassMetrics(pAndLs)
    val p = ms.precision
    println("Precision: " + p)
  }

  def runLocal(args: Array[String]) {
    val conf = new SparkConf().setAppName("Taxi Data Processor")
    conf.setMaster("local")
    val sc = new SparkContext(conf)
    sc.setLogLevel("WARN")
    val sqlContext = new SQLContext(sc)
    processData(loadFromCSV, sqlContext, args)
    sc.stop()
  }

  def runBluemix(args: Array[String]) {
    val conf = new SparkConf().setAppName("Taxi Data Processor")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    processData(loadFromDashDB, sqlContext, args)
    sc.stop()
  }

  def main(args: Array[String]) {
    if (args.length < 1) {
      println("missing command")
      System.exit(1)
    }

    val cmd = args.head

    cmd match {
      case "local" => runLocal(args.tail)
      case "bluemix" => runBluemix(args.tail)
      case _ => println(s"unknown command ${cmd}")
    }

  }

}
