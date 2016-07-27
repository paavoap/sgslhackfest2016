
import org.apache.spark._
import org.apache.spark.mllib.linalg.{Vectors, Vector}
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.regression.{LinearRegressionWithSGD, LinearRegressionModel}
import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{DataFrame, Column}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions._

object TaxiDataProcessor {
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

  // Run on local CSV data
  def runLocal(args: Array[String]) {
    val conf = new SparkConf().setAppName("Taxi Data Processor")
    conf.setMaster("local")
    val sc = SparkContext.getOrCreate(conf)
    sc.setLogLevel("WARN")
    val sqlContext = SQLContext.getOrCreate(sc)
    processData(loadFromCSV, sqlContext, args)
    sc.stop()
  }

  // Run on data from dashDB
  def runBluemix(args: Array[String]) {
    val conf = new SparkConf().setAppName("Taxi Data Processor")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)
    processData(loadFromDashDB, sqlContext, args)
    sc.stop()
  }

  // ===========================================
  // # DATA LOADING

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

  // ===========================================
  // # DATA PROCESSING

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

  val createVector = udf[Vector, Int, Int] { (tod, dow) =>
    Vectors.dense(tod, dow)
  }

  def toLabeledPointRDD(df: DataFrame): RDD[LabeledPoint] = {
    val r_rdd = df.rdd
    r_rdd.map[LabeledPoint] { row =>
      val fs = row.getAs[Vector]("features")
      val l = row.getAs[Long]("label")
      new LabeledPoint(l, fs)
    }
  }

  def summary(df: DataFrame) {
    df.printSchema
    df.show(3)
  }

  def summary(rdd: RDD[LabeledPoint]) {
    rdd.take(3).foreach(println)
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

    val vectorized = grouped
      .withColumn("features", createVector(grouped("timeofday"), grouped("dayofweek")))
    summary(vectorized)

    val labeled = vectorized
      .withColumnRenamed("count", "label")
    summary(labeled)

    trainLRModel(labeled)
  }

  // ===========================================
  // # MACHINE LEARNING

  def trainLRModel(df: DataFrame) {
    val rdd = toLabeledPointRDD(df)
    //summary(rdd)

    val splits = rdd.randomSplit(Array(0.8, 0.2))
    val training = splits(0).cache()
    val testing = splits(1)

    //val iterationCounts = Array(50, 100, 200, 500)
    val iterationCounts = Array(50)
    //val stepSizes = Array(0.00001, 0.000001, 0.0000001, 0.00000001)
    //val stepSizes = Array(0.00000001, 0.000000001, 0.0000000001, 0.00000000001)
    val stepSizes = Array(0.00000000001)
    // Mini batch fraction default value from Spark source
    val miniBatchFraction = 1.0
    // Initial weights from a previous run
    val initialWeights = Vectors.dense(0.6722928916506891,0.005505889905483374)
    for (iterationCount <- iterationCounts; stepSize <- stepSizes) {
      println("iterationCount "+iterationCount)
      println("stepSize "+stepSize)
      //training.take(3).foreach(println)
      val model = LinearRegressionWithSGD.train(training,
        iterationCount, stepSize, miniBatchFraction, initialWeights)

      println("Model intercept "+model.intercept)
      println("Model weights "+model.weights)

      val pAndLs = testing.map { case LabeledPoint(l, fs) =>
        val p = model.predict(fs)
        (p, l)
      }
      pAndLs.take(3).foreach(println)

      val mse = pAndLs.map { case (p, v) => math.pow((v - p), 2)}.mean()
      println("Mean square error " + mse)
    }

  }

}
