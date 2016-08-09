library(ibmdbR)
library(lubridate)
library(plyr)
#install.packages('neuralnet')
library("neuralnet")


con <- idaConnect("BLUDB","","")
idaInit(con)
query<-paste('select * from DASH7927.TAXI_LOCATIONS')
df <- idaQuery(query,as.is=F)


#get day of week from timestamp
dayOfWeek <- function(ts){
  x <- ymd_hms(ts)
  wday(x)
}
#dayOfWeek("2016-06-29 23:39:46")

stripTime<- function(ts){
  x <- ymd_hms(ts)
  hour(x) + minute(x)/60
}
#stripTime("2016-06-29 23:39:46")


printAvailableTaxiForRegion<-function(latMin,latMax,lonMin,lonMax){
  sampler<-subset(df, LAT < latMax & LAT > latMin & LNG>lonMin & LNG<lonMax)

  #use table to get number of row instead of nrow()
  tsTaxiCount <- data.frame(table(sampler$TIMESTAMP))

  tsTaxiCount <- ddply(tsTaxiCount, "Freq", summarise, time = eval(stripTime(Var1), envir = environment(stripTime)))
  #print(head(tsTaxiCount,50))

  return(tsTaxiCount)
}

buildModelforGrid<-function(latMin,latMax,lonMin,lonMax){

  gridData<-printAvailableTaxiForRegion(latMin,latMax,lonMin,lonMax)

  trainData<-cbind(gridData$time,gridData$Freq)
  colnames(trainData) <- c("Input","Output")
  max<-apply(trainData, 2, max)
  min<-apply(trainData, 2, min)
  scaled <- as.data.frame(scale(trainData, center = min, scale = max - min))
  #remove outlier
  scaled <-subset(scaled, !Output %in% boxplot.stats(scaled$Output)$out)
  colnames(scaled) <- c("Input","Output")
  #head(scaled)
  model <- neuralnet(Output~Input,scaled, hidden=c(5,5), threshold=0.001)

  return(model)
}


trainModel<-buildModelforGrid(103.85,103.869, 1.3,1.31)


#loop to build the model for each grid and save with different names
#trainModel<-buildModelforGrid(103.85,103.869, 1.3,1.31)
#save(trainModel, file = "mymodel.rda")


