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

The third script reads the CSV and pushes it into IBM dashDB&trade;. Before you can do that, you need to create a table for your data in IBM dashDB&trade;. You can use the included [create_db.sql](create_db.sql) script. Please refer to the IBM dashDB&trade; for information on how to create tables.

When run locally, the script will call the two previous scripts to fetch the taxi data from the API, and to convert it to CSV before pushing it into IBM dashDB&trade;.

    python load_taxi_data.py YOUR_API_KEY DASHDB_HOST DASHDB_PORT DASHDB_USER DASHDB_PASS DASHDB_SCHEMA DASHDB_TABLE

To run in OpenWhisk, create an action, create a sequence, and pass in the params.

    wsk action create load-taxi-data load_taxi_data.py
    wsk action create fetch-and-convert-and-load-taxi-data --sequence fetch-taxi-data,convert-taxi-data,load-taxi-data
    wsk action invoke --blocking --param key YOUR_API_KEY --param dashdb_host DASHDB_HOST --param dashdb_port DASHDB_PORT --param dashdb_user DASHDB_USER --param dashdb_pass DASHDB_PASS --param dashdb_schema DASHDB_SCHEMA --param dashdb_table DASHDB_TABLE fetch-and-convert-and-load-taxi-data

## Set Up Scheduled Task on OpenWhisk

To set up a scheduled fetch-convert-load task on OpenWhisk use the `/whisk.system/alarms` package. The package provides a feed `/whisk.system/alarms/alarm` which will trigger on a fixed schedule.

This creates all the actions for the sequence and connects it to an alarm that is scheduled every 10 minutes. First create the sequence.

    wsk action create fetch-taxi-data fetch_taxi_data.py
    wsk action create convert-taxi-data convert_taxi_data.py
    wsk action create load-taxi-data load_taxi_data.py
    wsk action create fetch-and-convert-and-load-taxi-data --sequence fetch-taxi-data,convert-taxi-data,load-taxi-data

Then create the scheduled trigger. The `cron` parameter defines the schdule. Here I have it set to every ten minutes. The content of the `trigger_payload` parameter gets sent to the triggered action.

    wsk trigger create taxi-data-fetch-interval --feed /whisk.system/alarms/alarm -p cron '*/10 * * * *' -p trigger_payload '{"key": "YOUR_API_KEY", "dashdb_host": "DASHDB_HOST", "dashdb_port": DASHDB_PORT, "dashdb_user": "DASHDB_USER", "dashdb_pass": "DASHDB_PASS", "dashdb_schema": "DASHDB_SCHEMA", "dashdb_table": "DASHDB_TABLE"}'

Finally, connect the trigger to your action sequence.

    wsk rule create --enable taxi-data-collector taxi-data-fetch-interval fetch-and-convert-and-load-taxi-data

**Important note.** The `/whisk.system/alarms/alarm` feed will stop after a max number of triggers have fired, and the trigger `taxi-data-fetch-interval` and the rule `taxi-data-collector` need to be deleted and recreated periodically.
