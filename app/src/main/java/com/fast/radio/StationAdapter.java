package com.fast.radio;

import android.app.Activity; import android.content.Context; import android.graphics.*; import android.graphics.drawable.ColorDrawable; import android.view.*; import android.widget.*; import java.io.*; import java.net.*; import java.util.*;

public class StationAdapter extends BaseAdapter {
 public interface Listener{void select(RadioStation s);void favorite(RadioStation s);} private final Context context; private final List<RadioStation> data; private final Listener listener;
 public StationAdapter(Context c,List<RadioStation>d,Listener l){context=c;data=d;listener=l;}
 public int getCount(){return data.size();} public Object getItem(int p){return data.get(p);} public long getItemId(int p){return p;}
 public View getView(int p,View convert,ViewGroup parent){
   RadioStation s=data.get(p); LinearLayout row=new LinearLayout(context); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(5,5,2,5);
   ImageView icon=new ImageView(context); icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE); icon.setImageDrawable(new ColorDrawable(Color.rgb(235,242,248))); row.addView(icon,new LinearLayout.LayoutParams(66,66));
   LinearLayout texts=new LinearLayout(context);texts.setOrientation(LinearLayout.VERTICAL);texts.setGravity(Gravity.CENTER_VERTICAL);texts.setPadding(7,0,2,0);
   TextView t=new TextView(context);t.setText((p+1)+". "+s.name);t.setTextColor(Color.rgb(20,20,20));t.setTextSize(14);t.setMaxLines(2); TextView sub=new TextView(context);sub.setText((s.country.isEmpty()?"":s.country+" • ")+(s.bitrate>0?s.bitrate+" kbps":"stream"));sub.setTextColor(Color.DKGRAY);sub.setTextSize(10);texts.addView(t);texts.addView(sub);row.addView(texts,new LinearLayout.LayoutParams(0,70,1));
   Button star=new Button(context);star.setText(s.favorite?"★":"☆");star.setTextSize(17);star.setTextColor(Color.rgb(70,145,220));row.addView(star,new LinearLayout.LayoutParams(50,58));
   row.setOnClickListener(v->listener.select(s));star.setOnClickListener(v->{listener.favorite(s);notifyDataSetChanged();});
   if(s.favicon!=null&&!s.favicon.isEmpty()&&!s.favicon.startsWith("data:")) new Thread(()->{Bitmap b=load(s.favicon);if(b!=null)((Activity)context).runOnUiThread(()->icon.setImageBitmap(b));}).start();
   return row;
 }
 private Bitmap load(String u){try{HttpURLConnection c=(HttpURLConnection)new URL(u).openConnection();c.setConnectTimeout(4000);c.setReadTimeout(5000);c.setInstanceFollowRedirects(true);c.setRequestProperty("User-Agent","FastRadio/8.0");InputStream in=c.getInputStream();Bitmap b=BitmapFactory.decodeStream(in);in.close();c.disconnect();return b;}catch(Exception e){return null;}}
}
