
import org.apache.spark._
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.functions._

object TaxiDataProcessor {
  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("Taxi Data Processor")
    val sc = new SparkContext(conf)
    val sqlContext = new SQLContext(sc)

    val host = args(0)
    val user = args(1)
    val pass = args(2)
    val url = s"jdbc:db2://${host}:50000/BLUDB:user=${user};password=${pass};"

    val table = args(3)

    val dashdata = sqlContext.load("jdbc", Map(
        "url" -> url,
        "dbtable" -> table))

    //dashdata.registerTempTable("taxidata")

    //dashdata.printSchema

    dashdata.groupBy("TIMESTAMP").count().orderBy(desc("count")).show()

    sc.stop()
  }
}
