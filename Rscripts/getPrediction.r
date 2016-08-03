args <- commandArgs(trailingOnly = TRUE)
print(args)
list<-unlist(strsplit(args,","))
#read normalised tod(0.0 to 1.0) from argument
timeOfDay<-as.numeric(list[1])
library("neuralnet")
load(file = "~/TaxiDataPlotPackage/mymodel.rda")
test.results <- compute(trainModel,timeOfDay)
test.results$net.result
