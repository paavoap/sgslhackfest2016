import json
from StringIO import StringIO
import csv

# 2016-06-21T19:14:50+08:00
def split_timestamp(ts):
    return (ts[:19].replace('T', ' '), ts[19:])

def convert_to_file(data, file):
    csv_w = csv.writer(file)
    fs = data.get('features', [])
    for f in fs:
        (ts, tz) = split_timestamp(f['properties']['timestamp'])
        ps = f['geometry']['coordinates']
        for p in ps:
            csv_w.writerow([ts, tz, p[0], p[1]])

def convert(data):
    csv_s = StringIO()
    convert_to_file(data, csv_s)
    return csv_s.getvalue()

def main(d):
    if 'data' not in d:
        return { 'error': 'No data in input' }

    data = json.loads(d.pop('data', {}))
    ret = d

    ret['data'] = convert(data)
    return ret

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("Missing argument API key")
        sys.exit(1)
    key = sys.argv[1]
    i = { "key": key }
    import fetch_taxi_data as fetch
    d = fetch.main(i)
    print(main(d))
