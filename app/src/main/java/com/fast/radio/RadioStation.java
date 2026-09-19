package com.fast.radio;

public class RadioStation {
    public String stationUuid=""; public String name=""; public String url=""; public String alternateUrl="";
    public String country=""; public String countryCode=""; public String codec=""; public int bitrate=0;
    public String homepage=""; public String favicon=""; public boolean favorite=false;
    public RadioStation(String name,String url){this.name=name;this.url=url;}
    public RadioStation(String name,String url,String favicon){this.name=name;this.url=url;this.favicon=favicon;}
    public RadioStation(String uuid,String name,String url,String alt,String country,String countryCode,String codec,int bitrate,String homepage,String favicon){
      this.stationUuid=uuid;this.name=name;this.url=url;this.alternateUrl=alt;this.country=country;this.countryCode=countryCode;this.codec=codec;this.bitrate=bitrate;this.homepage=homepage;this.favicon=favicon;
    }
    public String toString(){String s=name;if(!country.isEmpty())s+=" • "+country;if(bitrate>0)s+=" • "+bitrate+" kbps";return s;}
}
