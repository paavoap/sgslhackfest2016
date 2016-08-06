args <- commandArgs(trailingOnly = TRUE)
#print(args)
list<-unlist(strsplit(args,","))

todList<-unlist(strsplit(list[1]," "))

hhmmss<-todList[5]
hh<-as.numeric(unlist(strsplit(hhmmss,":"))[1])
mm<-as.numeric(unlist(strsplit(hhmmss,":"))[2])
timeOfDay<-(hh+mm/60.0)/24.0
#print(timeOfDay)
library("neuralnet")
load(file = "~/TaxiDataPlotPackage/mymodel.rda")
test.results <- compute(trainModel,timeOfDay)
test.results$net.result
