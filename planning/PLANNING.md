HackFest 2016
=============

## OpenWhisk Actions

The main data collection and analysis tasks can probably be built as OpenWhisk actions that get triggered on various events.

### Taxi Data Poller

Trigger every n (15?) minutes to fetch current taxi availability data from API (data.gov.sg). Possibly chain to perform reverse geocoding, and finally store into database.

Geocoding may end up not being useful. The available could have poor granularity for our use case. Postal codes are probably too granular, and the neighborhood information isn't granular enough.

### Model Builder

Trigger on change to database to build model (need to research and go into more depth here) based on new data.

If reverse geocoding wasn't done, and instead a grid is used, this may be a good place to map locations to grid cells.

## App

The app needs to access the database and the predicive models, and to present that information to the user in a useful way.

### Whole Singapore Availability History Visualization

Map with availability visualization (heat map maybe) and a way to move back and forward in time.

### Availability History and Prediction at Location

Get current location of user, and pass to API/handler to get current and future taxi availability.

## Technology and Tools

Some available cloud services with free trials that may be useful.

- Bluemix
- OpenWhisk Free Trial
- Cloudant NoSQL DB 20GB free
- Geospatial Analytics 1M checks free
- Predictive Analytics 2 models, 5,000 predictions per month, and 5 hours of compute time free
- Google Geocoding API Reverse Geocoding
- IBM Spark Free 3-month Trial

=============================================================================================
Comments:
-- Mapping location to grid should be done prior to Model Builder.
-- Singapore is about 719 km^2 in size. If we use grids that cover entire Singapore, even if we use big grid like 500m x 500m, that's almost 3000 grids. After removing those grid that have no roads within,  we could still have ~1000 grid. 
	If we just dump them into the model as parameters, that's not going to give us good models. Maybe we should use longitude and latitude as input prameter?
Tools that might be useful:
-- dashDB with R studio integrated for data storage and build model
-- Node Red. We can use it to pull data periodically and store it to dashDB. And possibly trigger OpenWhisk action 
