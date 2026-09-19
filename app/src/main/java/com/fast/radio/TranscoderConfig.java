package com.fast.radio;
import android.content.Context; import android.content.SharedPreferences;
public class TranscoderConfig {
 public static String base(Context c){return c.getSharedPreferences("fast_radio",0).getString("server_url","").trim();}
 public static String key(Context c){return c.getSharedPreferences("fast_radio",0).getString("server_key","");}
 public static void save(Context c,String url,String key){c.getSharedPreferences("fast_radio",0).edit().putString("server_url",url.trim()).putString("server_key",key).apply();}
}
