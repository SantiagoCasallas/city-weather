"""TEST ONLY. External provider fixture; never used by normal startup/deployment."""
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs
from datetime import date, timedelta
import json
class Provider(BaseHTTPRequestHandler):
 def log_message(self,*args): pass
 def do_GET(self):
  url=urlparse(self.path); params=parse_qs(url.query); code=200
  if url.path=='/v1/search':
   query=params.get('name',[''])[0].lower()
   if query=='error': code=503; body={'error':True}
   elif query=='sinresultados': body={}
   else: body={'results':[
    {'id':1,'name':'Córdoba','admin1':'Andalucía','country':'España','country_code':'ES','latitude':37.88,'longitude':-4.77,'timezone':'Europe/Madrid'},
    {'id':2,'name':'Córdoba','admin1':'Córdoba','country':'Argentina','country_code':'AR','latitude':-31.4,'longitude':-64.18,'timezone':'America/Argentina/Cordoba'}]}
  elif url.path=='/v1/forecast':
   body={'timezone':'America/Argentina/Cordoba','current':{'time':'2026-10-28T23:45','temperature_2m':18.5,'apparent_temperature':18,'relative_humidity_2m':None,'weather_code':3,'wind_speed_10m':5,'is_day':0},'current_units':{'temperature_2m':'°C','relative_humidity_2m':'%','wind_speed_10m':'km/h'},'daily_units':{'precipitation_probability_max':'%'},'daily':{'time':[(date(2026,10,28)+timedelta(days=i)).isoformat() for i in range(8)],'temperature_2m_min':[10]*8,'temperature_2m_max':[22]*8,'weather_code':[3]*8,'precipitation_probability_max':[30]*8}}
  else: body={'status':'fixture-only'}
  encoded=json.dumps(body).encode();self.send_response(code);self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(encoded)
ThreadingHTTPServer(('127.0.0.1',9099),Provider).serve_forever()
