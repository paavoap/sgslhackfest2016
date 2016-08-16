args <- commandArgs(trailingOnly = TRUE)

list<-unlist(strsplit(args,","))

todList<-unlist(strsplit(list[1]," "))

hhmmss<-todList[5]
hh<-as.numeric(unlist(strsplit(hhmmss,":"))[1])
mm<-as.numeric(unlist(strsplit(hhmmss,":"))[2])
timeOfDay<-(hh+mm/60.0)/24.0
#print(timeOfDay)
library("neuralnet")

lat <-as.numeric(list[2])
lon<-as.numeric( list[3])
loadModel<-function(lat,lon, timeOfDay){
  if(lat>104.01696 | lat<103.61323 | lon>1.46993 | lon<1.23348){
    print(-1)
  }
  else{
    lngGridSize = 0.05
    latGridSize = 0.05
    num<-floor((lat-103.61323)/latGridSize)*(5)+floor((lon-1.23348)/lngGridSize)
    name<-paste("~/TaxiDataPlotPackage/grid",num,".rda",sep='')
    tryCatch(load(file = name),error=function(cond) { print(-1)})
    test.results <- compute(trainModel,timeOfDay)
    print(test.results$net.result)
  }
}
loadModel(lat,lon,timeOfDay)
