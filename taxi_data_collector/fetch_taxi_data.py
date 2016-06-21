
from urllib2 import Request, urlopen
import json

URL = "https://api.data.gov.sg/v1/transport/taxi-availability"

def main(d):
    if 'key' not in d:
        return { 'error': 'Missing API key' }

    key = d.get('key')

    d.pop('data', None)
    ret = d

    req = Request(URL, headers = { 'api-key': key })
    res = urlopen(req)
    ret['status'] = res.getcode()
    if res.getcode() != 200:
        return ret

    # sending the data parsed caused an error "undefined (undefined)"
    # when chaining to another action
    #ret['data'] = json.loads(res.read())
    ret['data'] = res.read()
    return ret

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("Missing argument API key")
        sys.exit(1)
    key = sys.argv[1]
    i = { "key": key }
    print(main(i))
