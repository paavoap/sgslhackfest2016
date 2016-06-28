# Taxi Data Collector

OpenWhisk actions to collect taxi availability data from [Data.gov.sg](https://data.gov.sg/) into a database.

The collector is split into multiple scripts, which should be run in sequence, and will each consume the output of the previous script.

## Testing

The script to fetch data from the API can be run either locally or in OpenWhisk. Both require passing in a Data.gov.sg API key.

Locally.

    python fetch_taxi_data.py YOUR_API_KEY

On OpenWhisk, the action needs to be created on OpenWhisk first, and then it can be invoked. (Refer to the OpenWhisk documentation for instructions on how to set up OpenWhisk and `wsk`.)

    wsk action create fetch-taxi-data fetch_taxi_data.py
    wsk action invoke --blocking --param key YOUR_API_KEY fetch-taxi-data

The script to convert the fetched data into CSV can also be run either locally or in OpenWhisk. If run locally, it will call `fetch_taxi_data.py` to fetch the data, and then convert it.

Locally.

    python convert_taxi_data.py YOUR_API_KEY

To run in OpenWhisk, you need to create an action for the converter script, and also create a sequence which will connect the fetcher to the converter.

    wsk action create convert-taxi-data convert_taxi_data.py
    wsk action create fetch-and-convert-taxi-data --sequence fetch-taxi-data,convert-taxi-data
    wsk action invoke --blocking --param key YOUR_API_KEY fetch-and-convert-taxi-data

The third script reads the CSV and pushes it into IBM dashDB&trade;. When run locally, it will call the two previous scripts to fetch the taxi data from the API, and to convert it to CSV before pushing it into IBM dashDB&trade;.

    python load_taxi_data.py YOUR_API_KEY DASHDB_HOST DASHDB_PORT DASHDB_USER DASHDB_PASS

To run in OpenWhisk, create an action, create a sequence, and pass in the params.

    wsk action create load-taxi-data load_taxi_data.py
    wsk action create fetch-and-convert-and-load-taxi-data --sequence fetch-taxi-data,convert-taxi-data,load-taxi-data
    wsk action invoke --blocking --param key YOUR_API_KEY --param dashdb_host DASHDB_HOST --param dashdb_port DASHDB_PORT --param dashdb_user DASHDB_USER --param dashdb_pass DASHDB_PASS fetch-and-convert-and-load-taxi-data
    
