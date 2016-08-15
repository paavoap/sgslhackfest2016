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

validateModel<-function(model,testScale,paint){

  test.results <- compute(model,testScale$Input)
  if(paint){
    plot(testScale$Input,test.results$net.result,col="green")
    points(testScale)
  }

  mse <- sqrt(mean((testScale$Output - test.results$net.result)^2))
  return(mse)
}

buildModelforGrid<-function(latMin,latMax,lonMin,lonMax,paint){

  gridData<-printAvailableTaxiForRegion(latMin,latMax,lonMin,lonMax)

  print(paste(length(gridData$time),latMin,latMax,lonMin,lonMax,seq=" "))
  if(length(gridData$time)<1000){
    return(NULL)
  }
  trainData<-cbind(gridData$time,gridData$Freq)
  plot(trainData,col="yellow")
  colnames(trainData) <- c("Input","Output")
  max<-apply(trainData, 2, max)
  min<-apply(trainData, 2, min)
  scaled <- as.data.frame(scale(trainData, center = min, scale = max - min))
  #remove outlier
  scaled <-subset(scaled, !Output %in% boxplot.stats(scaled$Output)$out)
  colnames(scaled) <- c("Input","Output")
  trainData<-cbind(scaled$Input*max[1],scaled$Output*max[2])
  max<-apply(trainData, 2, max)
  min<-apply(trainData, 2, min)
  scaled <- as.data.frame(scale(trainData, center = min, scale = max - min))
  colnames(scaled) <- c("Input","Output")
  plot(scaled,col="blue")
  model <- neuralnet(Output~Input,scaled, hidden=c(15,10,5), threshold=0.05)


  mse<-validateModel(model,scaled,paint)
  print(mse)

  return(model)
}



trainModel<-buildModelforGrid(103.85,103.869, 1.3,1.31,TRUE)
summary(trainModel$result.matrix)
save(trainModel, file = "grid0.rda")

#loop to build the model for each grid and save with different names
#trainModel<-buildModelforGrid(103.85,103.869, 1.3,1.31)
#save(trainModel, file = "mymodel.rda")

buildModel<-function(paint){
  num =0
  latMax = max(df$LAT)
  lngMax = max(df$LNG)

  lngGridSize = 0.05
  latGridSize = 0.05

  lat = min(df$LAT)

  while(lat <= latMax){
    lng = min(df$LNG)
    while(lng<=lngMax){
      trainModel<-buildModelforGrid(lat,lat+latGridSize, lng,lng+lngGridSize,paint)
      name<-paste("grid",num,".rda",sep='')
      if(!is.null(trainModel)){
        save(trainModel, file = name)
      }
      num = num +1
      lng = lng + lngGridSize
    }
    lat = lat + latGridSize
  }
}
buildModel(FALSE)

