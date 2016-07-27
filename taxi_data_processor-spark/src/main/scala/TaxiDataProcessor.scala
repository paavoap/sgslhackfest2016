
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

  def minuteofday(c: Column): Column = {
    (hour(c) * 60) + minute(c)
  }

  def dayofweek(c: Column): Column = {
    date_format(c, "u")
  }

  val coordinatesToGrid = udf[Int, Double, Double] { (lat, lng) =>
    val maxLAT = 104.0168
    val minLAT = 103.61368
    val maxLNG = 1.46989
    val minLNG = 1.23377
    val gridH = 0.02
    val gridW = 0.02
    val height = maxLAT - minLAT
    val width = maxLNG - minLNG
    val hGrids = (height / gridH).toInt
    val wGrids = (width / gridW).toInt
    val gridLAT = ((lat - minLAT) / gridH).toInt
    val gridLNG = ((lng - minLNG) / gridW).toInt
    (hGrids * gridLNG) + gridLAT
  }

  val createVector = udf[Vector, Int, Double, Double] { (tod, lat, lng) =>
    Vectors.dense(tod, lat, lng)
  }

  def toLabeledPointRDD(df: DataFrame): RDD[LabeledPoint] = {
    val r_rdd = df.rdd
    r_rdd.map[LabeledPoint] { row =>
      val fs = row.getAs[Vector](6)
      val l = row.getDouble(5)
      new LabeledPoint(l, fs)
    }
  }

  def summary(df: DataFrame) {
    df.printSchema
    df.show(3)
  }

  def addTimeAndGridColumns(df: DataFrame): DataFrame = {
    val withTime = df
      .withColumn("timeofday", minuteofday(df("TIMESTAMP")))
      .withColumn("dayofweek", dayofweek(df("TIMESTAMP")))

    val withGrid = withTime
      .withColumn("grid", coordinatesToGrid(withTime("LAT"), withTime("LNG")))

    withGrid
  }

  def mostPopularGrid(df: DataFrame): Int = {
    val gridCounts = df
      .groupBy("grid").count()
      .orderBy(desc("count"))

    gridCounts.first.getAs[Int]("grid")
  }

  def processData(loader: (SQLContext, Array[String]) => DataFrame, sqlContext: SQLContext, args: Array[String]) {
    val dashdata = loader(sqlContext, args)
    summary(dashdata)

    val processed = addTimeAndGridColumns(dashdata)
    summary(processed)

    val grid = mostPopularGrid(processed)
    println(s"Selected grid ${grid}.")

    val selected = processed.filter(processed("grid") === grid)
    summary(selected)

    val grouped = selected
      .groupBy(selected("timeofday"), selected("dayofweek")).count()
    summary(grouped)

/*
    val labeled = withTime.withColumn("label", lit(1.0))
    summary(labeled)

    val vectorized = labeled
      .withColumn("features", createVector(labeled("timeofday"), labeled("LAT"), df("LNG")))
    summary(vectorized)

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
*/
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
