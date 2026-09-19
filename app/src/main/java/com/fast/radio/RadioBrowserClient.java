package com.fast.radio;

import android.net.Uri;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class RadioBrowserClient {
    public interface StationCallback { void result(List<RadioStation> stations); void error(Exception e); }
    public interface CountryCallback { void result(List<CountryItem> countries); void error(Exception e); }
    public static class CountryItem { public String name,code; public int count; public CountryItem(String n,String c,int x){name=n;code=c;count=x;} public String toString(){return name+(count>0?" ("+count+")":"");} }
    private static final String[] HOSTS={
        "https://de1.api.radio-browser.info","https://nl1.api.radio-browser.info","https://at1.api.radio-browser.info","https://all.api.radio-browser.info"
    };
    private static final ExecutorService EXEC=Executors.newCachedThreadPool();
    private static String get(String url)throws Exception{
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(10000); c.setReadTimeout(15000); c.setInstanceFollowRedirects(true);
        c.setRequestProperty("User-Agent","FastRadio/3.1 Android"); c.setRequestProperty("Accept","application/json");
        int code=c.getResponseCode(); if(code<200||code>=300) throw new IOException("HTTP "+code);
        InputStream in=new BufferedInputStream(c.getInputStream()); BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8")); StringBuilder b=new StringBuilder(); String l;
        while((l=r.readLine())!=null)b.append(l); r.close(); c.disconnect(); return b.toString();
    }
    private static List<RadioStation> parseStations(String json)throws Exception{
        JSONArray a=new JSONArray(json); List<RadioStation> out=new ArrayList<>(); Set<String> seen=new HashSet<>();
        for(int i=0;i<a.length();i++){
            JSONObject o=a.getJSONObject(i);
            String uuid=o.optString("stationuuid"); String url=o.optString("url_resolved"); String raw=o.optString("url");
            if(url.isEmpty()) url=raw; if(url.isEmpty()) continue;
            if(!uuid.isEmpty() && seen.contains(uuid)) continue; if(!uuid.isEmpty()) seen.add(uuid);
            // Prefer stations that RadioBrowser currently reports as reachable.
            if(o.has("lastcheckok") && !o.optBoolean("lastcheckok",true)) continue;
            out.add(new RadioStation(uuid,o.optString("name","Unnamed"),url,raw,o.optString("country"),o.optString("countrycode"),o.optString("codec"),o.optInt("bitrate",0),o.optString("homepage"),o.optString("favicon")));
        }
        Collections.sort(out, new Comparator<RadioStation>(){
            public int compare(RadioStation a, RadioStation b){
                boolean aa=a.codec!=null && a.codec.toUpperCase(Locale.US).contains("AMR");
                boolean bb=b.codec!=null && b.codec.toUpperCase(Locale.US).contains("AMR");
                if(aa!=bb) return aa?-1:1;
                int ab=a.bitrate>0?a.bitrate:999999;
                int bbp=b.bitrate>0?b.bitrate:999999;
                return Integer.compare(ab,bbp);
            }
        });
        return out;
    }
    public static void countries(CountryCallback cb){ EXEC.execute(()->{Exception last=null; for(String h:HOSTS)try{JSONArray a=new JSONArray(get(h+"/json/countries?order=stationcount&reverse=true&hidebroken=true")); List<CountryItem> out=new ArrayList<>(); for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);out.add(new CountryItem(o.optString("name"),o.optString("iso_3166_1"),o.optInt("stationcount")));} cb.result(out);return;}catch(Exception e){last=e;} cb.error(last);}); }
    public static void byCountry(String code,StationCallback cb){ request("/json/stations/bycountrycodeexact/"+Uri.encode(code)+"?hidebroken=true&order=clickcount&reverse=true&limit=500",cb); }
    public static void topStations(StationCallback cb){ request("/json/stations/topclick?hidebroken=true&order=clickcount&reverse=true&limit=500",cb); }
    public static void lowBandwidthStations(StationCallback cb){ request("/json/stations/search?name=&hidebroken=true&order=bitrate&reverse=false&limit=500",cb); }
    public static void search(String text,String code,StationCallback cb){ String q=Uri.encode(text); String path="/json/stations/search?name="+q+(code==null||code.isEmpty()?"":"&countrycode="+Uri.encode(code))+"&hidebroken=true&order=clickcount&reverse=true&limit=500"; request(path,cb); }
    private static void request(String path,StationCallback cb){ EXEC.execute(()->{Exception last=null; for(String h:HOSTS){try{List<RadioStation>x=parseStations(get(h+path)); if(!x.isEmpty()){cb.result(x);return;} last=new IOException("No stations returned");}catch(Exception e){last=e;}} cb.error(last==null?new IOException("No RadioBrowser server"):last);}); }
}
