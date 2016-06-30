import base64
import httplib
import mimetypes
import json

DASHDB_RESOURCE = '/dashdb-api/load/local/del/%s.%s?hasHeaderRow=false'

# From http://stackoverflow.com/a/681182
def encode_multipart_formdata(fields, files):
    LIMIT = '----------lImIt_of_THE_fIle_eW_$'
    CRLF = '\r\n'
    L = []
    for (key, value) in fields:
        L.append('--' + LIMIT)
        L.append('Content-Disposition: form-data; name="%s"' % key)
        L.append('')
        L.append(value)
    for (key, filename, value) in files:
        L.append('--' + LIMIT)
        L.append('Content-Disposition: form-data; name="%s"; filename="%s"' % (key, filename))
        L.append('Content-Type: %s' % get_content_type(filename))
        L.append('')
        L.append(value)
    L.append('--' + LIMIT + '--')
    L.append('')
    body = CRLF.join(L)
    content_type = 'multipart/form-data; boundary=%s' % LIMIT
    return content_type, body

def get_content_type(filename):
    return mimetypes.guess_type(filename)[0] or 'application/octet-stream'

def new_connection(d):
    dashdb_host = d.get('dashdb_host', '')
    dashdb_port = d.get('dashdb_port', '')

    return httplib.HTTPSConnection(dashdb_host, dashdb_port)

def make_body(d):
    fields = []
    files = [('loadFile', 'taxi.csv', d.get('data', ''))]
    return encode_multipart_formdata(fields, files)

def make_headers(d, content_type):
    headers = {'Content-Type': content_type}
    username = d.get('dashdb_user', '')
    password = d.get('dashdb_pass', '')
    auth_h = base64.encodestring('%s:%s' % (username, password)).replace('\n', '')
    headers["Authorization"] = "Basic %s" % auth_h
    return headers

def make_request(conn, d):
    content_type, body = make_body(d)
    headers = make_headers(d, content_type)
    schema = d.get('dashdb_schema', '')
    table = d.get('dashdb_table', '')
    resource = DASHDB_RESOURCE % (schema, table)
    conn.request('POST', resource, body, headers)

def get_response(conn):
    return conn.getresponse()

def main(d):
    conn = new_connection(d)
    make_request(conn, d)
    res = get_response(conn)

    data = d.pop('data', "")
    ret = d

    ret['status'] = res.status
    res_json = json.loads(res.read())
    ret['result'] = res_json['result']

    return ret

if __name__ == '__main__':
    import sys
    if len(sys.argv) < 2:
        print("Missing argument API key")
    if len(sys.argv) < 3:
        print("Missing argument DashDB host")
    if len(sys.argv) < 4:
        print("Missing argument DashDB port")
    if len(sys.argv) < 5:
        print("Missing argument DashDB username")
    if len(sys.argv) < 6:
        print("Missing argument DashDB password")
    if len(sys.argv) < 7:
        print("Missing argument DashDB schema")
    if len(sys.argv) < 8:
        print("Missing argument DashDB table")
        sys.exit(1)

    key = sys.argv[1]
    host = sys.argv[2]
    port = sys.argv[3]
    user = sys.argv[4]
    pwd = sys.argv[5]
    i = {
        'key': key,
        'dashdb_host': host,
        'dashdb_port': port,
        'dashdb_user': user,
        'dashdb_pass': pwd,
        'dashdb_schema': schema,
        'dashdb_table': table
    }
    import fetch_taxi_data as fetch
    d = fetch.main(i)
    import convert_taxi_data as convert
    d = convert.main(d)
    print(main(d))
