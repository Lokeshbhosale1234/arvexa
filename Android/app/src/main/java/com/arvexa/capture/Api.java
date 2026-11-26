package com.arvexa.capture;
import org.json.JSONObject;
import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
final class Api {
 static final class Reply {final int status; final JSONObject body;Reply(int s,JSONObject b){status=s;body=b;}}
 static String validateBase(String value) {
  URI u=URI.create(value.trim());
  if(!"https".equalsIgnoreCase(u.getScheme())||u.getHost()==null||u.getUserInfo()!=null||u.getQuery()!=null||u.getFragment()!=null)throw new IllegalArgumentException("Use an HTTPS backend URL ending in /api");
  String base=value.trim().replaceAll("/+$","");
  if(!base.endsWith("/api"))throw new IllegalArgumentException("Backend URL must end in /api");return base;
 }
 static Reply post(String base,String path,JSONObject payload,String bearer,String deviceToken) throws Exception {
  HttpURLConnection c=(HttpURLConnection)new URL(base+path).openConnection();
  c.setInstanceFollowRedirects(false);c.setConnectTimeout(15000);c.setReadTimeout(45000);c.setRequestMethod("POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");
  if(bearer!=null)c.setRequestProperty("Authorization","Bearer "+bearer);
  if(deviceToken!=null)c.setRequestProperty("X-Arvexa-Device-Token",deviceToken);
  try {
   try(OutputStream out=c.getOutputStream()){out.write(payload.toString().getBytes(StandardCharsets.UTF_8));}
   int status=c.getResponseCode();InputStream stream=status>=400?c.getErrorStream():c.getInputStream();
   String text="";
   if(stream!=null)try(InputStream in=stream;ByteArrayOutputStream bytes=new ByteArrayOutputStream()){
    byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){bytes.write(b,0,n);if(bytes.size()>131072)throw new IOException("Response too large");}text=bytes.toString(StandardCharsets.UTF_8.name());
   }
   JSONObject body;try{body=new JSONObject(text);}catch(Exception ignored){body=new JSONObject();}
   return new Reply(status,body);
  }finally{c.disconnect();}
 }
 private Api(){}
}
