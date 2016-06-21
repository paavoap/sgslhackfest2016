
from urllib2 import Request, urlopen
import json

URL = "https://api.data.gov.sg/v1/transport/taxi-availability"

def main(d):
    if 'key' not in d:
        return { 'error': 'Missing API key' }

    key = d.get('key')

    ret = { 'status': 200 }

    req = Request(URL, headers = { 'api-key': key })
    res = urlopen(req)
    if res.getcode() != 200:
        ret['status'] = res.getcode()
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
