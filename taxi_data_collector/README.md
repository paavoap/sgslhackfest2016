# Taxi Data Collector

OpenWhisk actions to collect taxi availability data from [Data.gov.sg](https://data.gov.sg/) into a database.

## Testing

The script can be run either locally or in OpenWhisk. Both require passing in a Data.gov.sg API key.

Locally.

    python fetch_taxi_data.py YOUR_API_KEY

On OpenWhisk, the action needs to be created on OpenWhisk first, and then it can be invoked. (Refer to the OpenWhisk documentation for instructions on how to set up OpenWhisk and `wsk`.)

    wsk action create fetch-taxi-data fetch_taxi_data.py
    wsk action invoke --blocking --param key YOUR_API_KEY fetch-taxi-data
