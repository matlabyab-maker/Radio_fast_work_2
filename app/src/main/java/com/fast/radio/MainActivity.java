package com.fast.radio;

import android.content.*; import android.database.Cursor; import android.net.*; import android.net.TrafficStats; import android.os.*; import android.provider.MediaStore; import android.view.*; import android.widget.*; import android.media.audiofx.Equalizer; import androidx.appcompat.app.AppCompatActivity; import androidx.appcompat.app.AlertDialog; import androidx.media3.common.*; import androidx.media3.session.*; import androidx.media3.session.MediaController; import com.google.common.util.concurrent.ListenableFuture; import org.json.*; import java.io.*; import java.net.*; import java.util.*;

public class MainActivity extends AppCompatActivity {
 ListView customList,iranList,persianList; TextView status,nowPlaying,qualityValue,usagePerMinute; List<RadioStation> custom=new ArrayList<>(),iran=new ArrayList<>(),persian=new ArrayList<>(),world=new ArrayList<>(),favorites=new ArrayList<>(); StationAdapter customAdapter,iranAdapter,persianAdapter,worldAdapter; MediaController controller; ListenableFuture<MediaController> controllerFuture; VerticalRulerView qualityRuler; FrameLayout rootFrame; TextView sunLight,tvUpper,tvLower,newsLower,recordLight; Button tvToggle,newsToggle,recordButton; boolean tvTickerOn=true,newsTickerOn=true; int tvIndex=0,newsIndex=0; Handler sunHandler=new Handler(Looper.getMainLooper()); Random random=new Random(); RadioStation selected; LegacyMediaPlayerEngine legacyEngine; Equalizer equalizer; Handler usageHandler=new Handler(Looper.getMainLooper()); long usageBase=0,minuteStart=0; int minute=1; boolean recording=false; String recordId=""; RadioStation draggingStation; List<RadioStation> draggingSource;
 final Runnable usage=new Runnable(){public void run(){updateUsage();usageHandler.postDelayed(this,1000);}};
 @Override protected void onCreate(Bundle b){super.onCreate(b);setContentView(R.layout.activity_main);legacyEngine=new LegacyMediaPlayerEngine(this);bind();loadCustom();loadPersian();setupIran();connectController();}
 void bind(){
  rootFrame=findViewById(R.id.rootFrame); sunLight=findViewById(R.id.sunLight); recordLight=findViewById(R.id.recordLight); recordButton=findViewById(R.id.record); final View tvPanel=findViewById(R.id.tvPanel); findViewById(R.id.tvButton).setOnClickListener(v->showTvWindow()); tvUpper=findViewById(R.id.tvUpper); tvLower=findViewById(R.id.tvLower); newsLower=findViewById(R.id.newsLower); tvToggle=findViewById(R.id.tvTickerToggle); newsToggle=findViewById(R.id.newsTickerToggle);
  customList=findViewById(R.id.customList);iranList=findViewById(R.id.iranList);persianList=findViewById(R.id.persianList);status=findViewById(R.id.status);nowPlaying=findViewById(R.id.nowPlaying);qualityValue=findViewById(R.id.qualityValue);usagePerMinute=findViewById(R.id.usagePerMinute);qualityRuler=findViewById(R.id.qualityRuler);
  if(recordLight!=null)recordLight.setVisibility(View.GONE); if(recordButton!=null)recordButton.setText("REC"); qualityRuler.setValue(24);qualityRuler.setListener(v->{qualityValue.setText(v+" kbps");status.setText("Target "+v+" kbps");});
  findViewById(R.id.onlineSearch).setOnClickListener(v->onlineSearch()); findViewById(R.id.countrySearch).setOnClickListener(v->showCountryWindow()); findViewById(R.id.tvTickerToggle).setOnClickListener(v->toggleTvTicker()); findViewById(R.id.newsTickerToggle).setOnClickListener(v->toggleNewsTicker()); findViewById(R.id.tvPrev).setOnClickListener(v->{tvIndex=(tvIndex+6)%7;updateTvTicker();}); findViewById(R.id.tvNext).setOnClickListener(v->{tvIndex=(tvIndex+1)%7;updateTvTicker();}); findViewById(R.id.newsPrev).setOnClickListener(v->{newsIndex=(newsIndex+24)%25;updateNewsTicker();}); findViewById(R.id.newsNext).setOnClickListener(v->{newsIndex=(newsIndex+1)%25;updateNewsTicker();}); findViewById(R.id.play).setOnClickListener(v->playSelected());findViewById(R.id.stop).setOnClickListener(v->stop());findViewById(R.id.record).setOnClickListener(v->toggleRecord());findViewById(R.id.fav).setOnClickListener(v->{if(selected!=null)toggleFavorite(selected);});findViewById(R.id.equalizerButton).setOnClickListener(v->showEqualizer());findViewById(R.id.settings).setOnClickListener(v->settings());findViewById(R.id.addRadio).setOnClickListener(v->addRadio());findViewById(R.id.importRadio).setOnClickListener(v->importList());findViewById(R.id.worldButton).setOnClickListener(v->showWorld());
  updateTvTicker(); updateNewsTicker(); scheduleLiveText(); scheduleSunLight(); setupDragLists(); setupScroll(R.id.customUp,customList,true);setupScroll(R.id.customDown,customList,false);setupScroll(R.id.iranUp,iranList,true);setupScroll(R.id.iranDown,iranList,false);setupScroll(R.id.persianUp,persianList,true);setupScroll(R.id.persianDown,persianList,false);
 }



 void setupDragLists(){
  setupDragList(customList,custom,"CUSTOM RADIO");
  setupDragList(iranList,iran,"IRAN RADIO");
  setupDragList(persianList,persian,"SPECIAL RADIO");
 }
 void setupDragList(final ListView view,final List<RadioStation> data,final String label){
  view.setOnItemLongClickListener((parent,v,pos,id)->{
   if(pos<0||pos>=data.size()) return true;
   draggingStation=data.get(pos); draggingSource=data;
   ClipData clip=ClipData.newPlainText("FastRadioStation",String.valueOf(pos));
   View.DragShadowBuilder shadow=new View.DragShadowBuilder(v);
   if(Build.VERSION.SDK_INT>=24) v.startDragAndDrop(clip,shadow,null,View.DRAG_FLAG_GLOBAL);
   else v.startDrag(clip,shadow,null,0);
   status.setText("Drag: "+draggingStation.name+" → another list");
   return true;
  });
  view.setOnDragListener((v,event)->{
   switch(event.getAction()){
    case DragEvent.ACTION_DRAG_STARTED: return draggingStation!=null;
    case DragEvent.ACTION_DRAG_ENTERED: status.setText("Drop into "+label); return true;
    case DragEvent.ACTION_DRAG_EXITED: return true;
    case DragEvent.ACTION_DROP:
     if(draggingStation==null||draggingSource==data) return true;
     if(!data.contains(draggingStation)){
      data.add(draggingStation);
      removeFromAllOtherLists(data,draggingStation);
      notifyAllStationAdapters();
      status.setText(draggingStation.name+" → "+label);
     }
     draggingStation=null; draggingSource=null;
     return true;
    case DragEvent.ACTION_DRAG_ENDED: draggingStation=null; draggingSource=null; return true;
   }
   return true;
  });
 }
 void removeFromAllOtherLists(List<RadioStation> target,RadioStation s){
  if(target!=custom) custom.remove(s);
  if(target!=iran) iran.remove(s);
  if(target!=persian) persian.remove(s);
 }
 void notifyAllStationAdapters(){
  if(customAdapter!=null) customAdapter.notifyDataSetChanged();
  if(iranAdapter!=null) iranAdapter.notifyDataSetChanged();
  if(persianAdapter!=null) persianAdapter.notifyDataSetChanged();
 }
 void showCountryWindow(){
  final LinearLayout root=new LinearLayout(this);
  root.setOrientation(LinearLayout.VERTICAL);
  root.setPadding(12,8,12,8);
  final LinearLayout regions=new LinearLayout(this);
  regions.setOrientation(LinearLayout.HORIZONTAL);
  final ListView countriesList=new ListView(this);
  final TextView hint=new TextView(this);
  hint.setText("در حال دریافت فهرست کشورها...");
  hint.setTextSize(12);
  hint.setPadding(8,8,8,8);
  root.addView(regions,new LinearLayout.LayoutParams(-1,52));
  root.addView(hint,new LinearLayout.LayoutParams(-1,36));
  root.addView(countriesList,new LinearLayout.LayoutParams(-1,0,1));

  final AlertDialog dlg=new AlertDialog.Builder(this).setTitle("REGION / COUNTRY").setView(root).setNegativeButton("CLOSE",null).create();

  RadioBrowserClient.countries(new RadioBrowserClient.CountryCallback(){
   public void result(List<RadioBrowserClient.CountryItem> all){
    runOnUiThread(()->{
     hint.setText("یک منطقه و سپس کشور را انتخاب کنید");
     final String[] regionNames={"America","Europe","Africa","Asia","National"};
     for(String rn:regionNames){
      Button b=new Button(MainActivity.this); b.setText(rn); b.setTextSize(9); b.setPadding(2,0,2,0);
      regions.addView(b,new LinearLayout.LayoutParams(0,50,1));
      b.setOnClickListener(v->{
       List<RadioBrowserClient.CountryItem> filtered=new ArrayList<>();
       for(RadioBrowserClient.CountryItem c:all){
        String rg=RegionCatalog.region(c.code);
        if(rn.equals("National") || rg.equals(rn)) filtered.add(c);
       }
       Collections.sort(filtered,(a,b2)->a.name.compareToIgnoreCase(b2.name));
       ArrayAdapter<RadioBrowserClient.CountryItem> ca=new ArrayAdapter<RadioBrowserClient.CountryItem>(MainActivity.this,android.R.layout.simple_list_item_1,filtered);
       countriesList.setAdapter(ca);
       hint.setText(rn+" • "+filtered.size()+" countries");
      });
     }
     countriesList.setOnItemClickListener((parent,view,pos,id)->{
      RadioBrowserClient.CountryItem c=(RadioBrowserClient.CountryItem)parent.getItemAtPosition(pos);
      showCountryStations(c,dlg);
     });
     // Default: show all countries alphabetically
     Button first=(Button)regions.getChildAt(0);
     if(first!=null) first.performClick();
    });
   }
   public void error(Exception e){runOnUiThread(()->hint.setText("Country list unavailable")); }
  });
  dlg.show();
 }
 void showCountryStations(RadioBrowserClient.CountryItem c, AlertDialog parent){
  final ListView list=new ListView(this);
  final List<RadioStation> data=new ArrayList<>();
  final StationAdapter ad=new StationAdapter(this,data,new StationAdapter.Listener(){
   public void select(RadioStation st){selected=st;nowPlaying.setText("▶ "+st.name);status.setText("Selected • "+c.name);parent.dismiss();}
   public void favorite(RadioStation st){toggleFavorite(st);}
  });
  list.setAdapter(ad);
  AlertDialog stationDlg=new AlertDialog.Builder(this).setTitle(c.name+" • "+c.code).setView(list).setNegativeButton("BACK",null).create();
  stationDlg.show();
  status.setText("Loading "+c.name+"...");
  RadioBrowserClient.byCountry(c.code,new RadioBrowserClient.StationCallback(){
   public void result(List<RadioStation>x){runOnUiThread(()->{data.clear();data.addAll(x);ad.notifyDataSetChanged();status.setText(c.name+" • "+x.size()+" stations");});}
   public void error(Exception e){runOnUiThread(()->status.setText(c.name+" • unavailable"));}
  });
 }
 void onlineSearch(){ final EditText q=new EditText(this); q.setHint("نام رادیو یا کشور"); q.setSingleLine(true); LinearLayout box=new LinearLayout(this); box.setPadding(24,8,24,4); box.setOrientation(LinearLayout.VERTICAL); box.addView(q,new LinearLayout.LayoutParams(-1,-2)); final ListView list=new ListView(this); final List<RadioStation> results=new ArrayList<>(); final StationAdapter[] ad=new StationAdapter[1]; AlertDialog dlg=new AlertDialog.Builder(this).setTitle("ONLINE RADIO SEARCH").setView(box).setNegativeButton("CLOSE",null).create(); LinearLayout actions=new LinearLayout(this); actions.setOrientation(LinearLayout.HORIZONTAL); Button go=new Button(this); go.setText("SEARCH ONLINE"); Button save=new Button(this); save.setText("SAVE RESULTS"); Button saved=new Button(this); saved.setText("SAVED LIST"); actions.addView(go,new LinearLayout.LayoutParams(0,50,1)); actions.addView(save,new LinearLayout.LayoutParams(0,50,1)); actions.addView(saved,new LinearLayout.LayoutParams(0,50,1)); box.addView(actions); box.addView(list,new LinearLayout.LayoutParams(-1,520)); go.setOnClickListener(v->{String text=q.getText().toString().trim(); if(text.isEmpty())return; status.setText("Searching RadioBrowser..."); RadioBrowserClient.search(text,null,new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{results.clear();results.addAll(x); ad[0]=new StationAdapter(MainActivity.this,results,new StationAdapter.Listener(){public void select(RadioStation s){selected=s;nowPlaying.setText("▶ "+s.name);status.setText("Online result selected");dlg.dismiss();}public void favorite(RadioStation s){toggleFavorite(s);}});list.setAdapter(ad[0]);status.setText("Online search • "+x.size()+" results");});}public void error(Exception e){runOnUiThread(()->status.setText("Online search unavailable"));}});}); save.setOnClickListener(v->{if(results.isEmpty()){status.setText("No search results to save");return;}saveSearchResults(results);}); saved.setOnClickListener(v->showSavedSearchResults(dlg)); dlg.show(); }
 void saveSearchResults(List<RadioStation> data){try{JSONArray a=new JSONArray();for(RadioStation s:data){JSONObject o=new JSONObject();o.put("uuid",s.stationUuid);o.put("name",s.name);o.put("url",s.url);o.put("alternateUrl",s.alternateUrl);o.put("country",s.country);o.put("countryCode",s.countryCode);o.put("codec",s.codec);o.put("bitrate",s.bitrate);o.put("homepage",s.homepage);o.put("favicon",s.favicon);a.put(o);}File f=new File(getFilesDir(),"saved_search_results.json");try(FileOutputStream out=new FileOutputStream(f)){out.write(a.toString().getBytes("UTF-8"));}status.setText("Saved "+data.size()+" results on device");}catch(Exception e){status.setText("Save results failed");}}
 void showSavedSearchResults(AlertDialog parent){final List<RadioStation> data=loadSavedSearchResults();if(data.isEmpty()){status.setText("No saved search results");return;}final ListView list=new ListView(this);StationAdapter ad=new StationAdapter(this,data,new StationAdapter.Listener(){public void select(RadioStation s){selected=s;nowPlaying.setText("▶ "+s.name);status.setText("Saved result selected");parent.dismiss();}public void favorite(RadioStation s){toggleFavorite(s);}});list.setAdapter(ad);new AlertDialog.Builder(this).setTitle("SAVED SEARCH RESULTS • "+data.size()).setView(list).setPositiveButton("CLOSE",null).setNeutralButton("DELETE",(d,w)->{deleteSavedSearchResults();status.setText("Saved search list deleted");}).show();}
 List<RadioStation> loadSavedSearchResults(){List<RadioStation> data=new ArrayList<>();File f=new File(getFilesDir(),"saved_search_results.json");if(!f.exists())return data;try{String text=readStream(new FileInputStream(f));JSONArray a=new JSONArray(text);for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);data.add(new RadioStation(o.optString("uuid"),o.optString("name"),o.optString("url"),o.optString("alternateUrl"),o.optString("country"),o.optString("countryCode"),o.optString("codec"),o.optInt("bitrate"),o.optString("homepage"),o.optString("favicon")));}}catch(Exception ignored){}return data;}
 void deleteSavedSearchResults(){File f=new File(getFilesDir(),"saved_search_results.json");if(f.exists())f.delete();}
 void toggleTvTicker(){tvTickerOn=!tvTickerOn;tvToggle.setText(tvTickerOn?"ON":"OFF");tvUpper.setVisibility(tvTickerOn?View.VISIBLE:View.GONE);tvLower.setVisibility(tvTickerOn?View.VISIBLE:View.GONE);}
 void toggleNewsTicker(){newsTickerOn=!newsTickerOn;newsToggle.setText(newsTickerOn?"ON":"OFF");newsLower.setVisibility(newsTickerOn?View.VISIBLE:View.GONE);}
 void updateTvTicker(){String[] n={"BBC Persian","VOA Persian"};if(!tvTickerOn)return;tvUpper.setText("TV: "+n[tvIndex]+" • Online");tvLower.setText(tvIndex==0?"BBC Persian • news subtitle / Persian translation when available":"VOA Persian • news subtitle / Persian translation when available");}
 void updateNewsTicker(){String[] n={"آفتاب نیوز","ایلنا","پویش","الف","بهارنیوز","تابناک","باشگاه خبرنگاران جوان","جام نیوز","انتخاب","پارسینه","فرارو","خبرآنلاین","فردانیوز","مشرق نیوز","جماران","صراط نیوز","رجانیوز","عصرایران","نواندیش","تیک","دانا","تسنیم","ورزش3","طرفداری","بانک ورزش"};if(!newsTickerOn)return;newsLower.setText(n[newsIndex]+" • خبر آنلاین");}
 void scheduleLiveText(){final Handler h=new Handler(Looper.getMainLooper());h.postDelayed(new Runnable(){public void run(){updateTvTicker();updateNewsTicker();h.postDelayed(this,60000);}},60000);}
 void scheduleSunLight(){sunHandler.removeCallbacksAndMessages(null);sunHandler.postDelayed(new Runnable(){public void run(){showSunLight();sunHandler.postDelayed(this,60000);}},60000);}
 void showSunLight(){if(rootFrame.getWidth()<80||rootFrame.getHeight()<80)return;float x=random.nextInt(Math.max(1,rootFrame.getWidth()-70));float y=random.nextInt(Math.max(1,rootFrame.getHeight()-130));sunLight.setX(x);sunLight.setY(y);sunLight.setVisibility(View.VISIBLE);sunLight.setAlpha(0f);sunLight.animate().alpha(1f).setDuration(800).withEndAction(()->sunLight.postDelayed(()->sunLight.animate().alpha(0f).setDuration(800).withEndAction(()->sunLight.setVisibility(View.INVISIBLE)).start(),8400)).start();}

 void setupScroll(int id,ListView l,boolean up){findViewById(id).setOnClickListener(v->{int p=l.getFirstVisiblePosition();l.setSelection(Math.max(0,p+(up?-7:7)));});}
 void loadCustom(){try{JSONArray a=new JSONArray(readAsset("custom_imported_stations.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);custom.add(new RadioStation(o.optString("name"),o.optString("url"),o.optString("icon")));}}catch(Exception e){status.setText("Custom list load error");}setupAdapters();}
 void loadPersian(){try{JSONArray a=new JSONArray(readAsset("persian_special.json"));for(int i=0;i<a.length();i++){JSONObject o=a.getJSONObject(i);RadioStation s=new RadioStation(o.optString("name"),o.optString("url"),o.optString("icon"));s.homepage=o.optString("homepage");persian.add(s);}}catch(Exception e){}setupAdapters();}
 void setupIran(){RadioBrowserClient.byCountry("IR",new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{iran.clear();iran.addAll(x);if(iranAdapter!=null)iranAdapter.notifyDataSetChanged();status.setText("Iran Radio • "+x.size()+" stations (separate list)");});}public void error(Exception e){runOnUiThread(()->status.setText("Iran Radio list unavailable"));}});}
 void setupAdapters(){StationAdapter.Listener l=new StationAdapter.Listener(){public void select(RadioStation s){selected=s;nowPlaying.setText("▶ "+s.name);status.setText("Selected • "+qualityRuler.getValue()+" kbps");}public void favorite(RadioStation s){toggleFavorite(s);}};customAdapter=new StationAdapter(this,custom,l);iranAdapter=new StationAdapter(this,iran,l);persianAdapter=new StationAdapter(this,persian,l);worldAdapter=new StationAdapter(this,world,l);customList.setAdapter(customAdapter);iranList.setAdapter(iranAdapter);persianList.setAdapter(persianAdapter);}
 String readAsset(String n)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(getAssets().open(n),"UTF-8"));StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s);r.close();return b.toString();}
 void connectController(){SessionToken t=new SessionToken(this,new ComponentName(this,RadioPlaybackService.class));controllerFuture=new MediaController.Builder(this,t).buildAsync();controllerFuture.addListener(()->{try{controller=controllerFuture.get();controller.addListener(new Player.Listener(){public void onPlayerError(PlaybackException e){fallback();}});}catch(Exception e){status.setText("Player error");}},getMainExecutor());}
 String mediaUrl(String u){String x=u.toLowerCase(Locale.US);if(x.contains(".m3u8"))return u;if(x.contains(".mpd"))return u;return u;}
 void playSelected(){if(selected==null){status.setText("Select a station");return;}if(controller==null){status.setText("Player not ready");return;}legacyEngine.stop();int kb=qualityRuler.getValue();String transcoded=TranscoderUrl.build(this,selected.url,"amr-nb",Math.min(12,kb));String play=transcoded.isEmpty()?mediaUrl(selected.url):transcoded;MediaItem item=new MediaItem.Builder().setUri(play).build();controller.setMediaItem(item);controller.prepare();controller.play();controller.setVolume(1f);startUsage();nowPlaying.setText("▶ "+selected.name);status.setText(transcoded.isEmpty()?"Playing original stream":"Playing through Fast Radio server");}
 void fallback(){if(selected==null)return;try{legacyEngine.play(selected.url);startUsage();status.setText("Compatibility player");}catch(Exception e){status.setText("Stream unavailable");}}
 void stop(){if(controller!=null)controller.stop();legacyEngine.stop();stopUsage();if(recording)stopRecord();status.setText("Stopped");}
 void toggleRecord(){if(selected==null){status.setText("Select a station first");return;}if(TranscoderConfig.base(this).isEmpty()){status.setText("Record needs Fast Radio server in Settings");return;}if(!recording)startRecord();else stopRecord();}
 void startRecord(){new Thread(()->{try{URL u=new URL(TranscoderConfig.base(this)+"record/start?url="+Uri.encode(selected.url)+"&codec=amr-nb&bitrate=12");HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod("POST");c.setRequestProperty("X-API-Key",TranscoderConfig.key(this));c.setConnectTimeout(10000);c.setReadTimeout(15000);int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);String s=readStream(c.getInputStream());c.disconnect();JSONObject o=new JSONObject(s);recordId=o.optString("id");runOnUiThread(()->{recording=true;if(recordLight!=null){recordLight.setVisibility(View.VISIBLE);recordLight.bringToFront();}if(recordButton!=null)recordButton.setText("● REC");status.setText("● Recording AMR");});}catch(Exception e){runOnUiThread(()->{if(recordLight!=null)recordLight.setVisibility(View.GONE);if(recordButton!=null)recordButton.setText("REC");status.setText("Record start failed");});}}).start();}
 void stopRecord(){if(!recording||recordId.isEmpty())return;String id=recordId;recording=false;if(recordLight!=null)recordLight.setVisibility(View.GONE);if(recordButton!=null)recordButton.setText("REC");recordId="";new Thread(()->{try{URL u=new URL(TranscoderConfig.base(this)+"record/stop?id="+Uri.encode(id));HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestProperty("X-API-Key",TranscoderConfig.key(this));c.setConnectTimeout(10000);c.setReadTimeout(30000);int code=c.getResponseCode();if(code<200||code>=300)throw new IOException("HTTP "+code);byte[] data=readBytes(c.getInputStream());c.disconnect();saveAmr(data);runOnUiThread(()->status.setText("AMR recording saved"));}catch(Exception e){runOnUiThread(()->status.setText("Record stop failed"));}}).start();}
 void saveAmr(byte[] data)throws Exception{ContentValues v=new ContentValues();v.put(MediaStore.Audio.Media.DISPLAY_NAME,"FastRadio_"+System.currentTimeMillis()+".amr");v.put(MediaStore.Audio.Media.MIME_TYPE,"audio/amr");v.put(MediaStore.Audio.Media.RELATIVE_PATH,"Music/Fast Radio");Uri uri=getContentResolver().insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,v);if(uri==null)throw new IOException("MediaStore");OutputStream o=getContentResolver().openOutputStream(uri);o.write(data);o.close();}
 void applyVolume(float v){if(controller!=null)controller.setVolume(Math.max(0f,Math.min(2f,v)));}
 void toggleFavorite(RadioStation s){s.favorite=!s.favorite;if(s.favorite&&!favorites.contains(s))favorites.add(s);if(!s.favorite)favorites.remove(s);customAdapter.notifyDataSetChanged();iranAdapter.notifyDataSetChanged();persianAdapter.notifyDataSetChanged();saveFavorites();}
 void showEqualizer(){try{if(controller==null)return;int sid=controller.getAudioSessionId();if(sid==0){status.setText("EQ unavailable");return;}if(equalizer!=null)equalizer.release();equalizer=new Equalizer(0,sid);equalizer.setEnabled(true);short bands=equalizer.getNumberOfBands();LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);for(short b=0;b<bands;b++){SeekBar bar=new SeekBar(this);bar.setMax(equalizer.getBandLevelRange()[1]-equalizer.getBandLevelRange()[0]);bar.setProgress(equalizer.getBandLevel(b)-equalizer.getBandLevelRange()[0]);final short bb=b;bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){if(f)equalizer.setBandLevel(bb,(short)(p+equalizer.getBandLevelRange()[0]));}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(bar,new LinearLayout.LayoutParams(-1,55));}new AlertDialog.Builder(this).setTitle("Equalizer").setView(box).setPositiveButton("OK",null).show();}catch(Exception e){status.setText("EQ unavailable");}}
 void settings(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(24,12,24,8);EditText server=new EditText(this);server.setHint("https://your-server:8080");server.setText(TranscoderConfig.base(this));EditText key=new EditText(this);key.setHint("API key");key.setText(TranscoderConfig.key(this));box.addView(new TextView(this){{setText("Fast Radio transcoder server");}});box.addView(server);box.addView(key);TextView vol=new TextView(this);vol.setText("Volume: hardware-normal to 2x");box.addView(vol);SeekBar vb=new SeekBar(this);vb.setMax(200);vb.setProgress(100);vb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onProgressChanged(SeekBar s,int p,boolean f){if(f)applyVolume(p/100f);}public void onStartTrackingTouch(SeekBar s){}public void onStopTrackingTouch(SeekBar s){}});box.addView(vb);new AlertDialog.Builder(this).setTitle("Settings").setView(box).setPositiveButton("SAVE",(d,w)->{TranscoderConfig.save(this,server.getText().toString(),key.getText().toString());status.setText("Settings saved");}).setNeutralButton("TEST SERVER",(d,w)->testServer(server.getText().toString(),key.getText().toString())).show();}
 void testServer(String b,String k){final String base=b.endsWith("/")?b:b+"/";new Thread(()->{try{HttpURLConnection c=(HttpURLConnection)new URL(base+"health").openConnection();c.setRequestProperty("X-API-Key",k);c.setConnectTimeout(6000);int code=c.getResponseCode();c.disconnect();runOnUiThread(()->status.setText(code==200?"Server ONLINE":"Server HTTP "+code));}catch(Exception e){runOnUiThread(()->status.setText("Server unavailable"));}}).start();}
 void addRadio(){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);EditText n=new EditText(this);n.setHint("Radio name");EditText u=new EditText(this);u.setHint("http:// or https:// stream URL");box.addView(n);box.addView(u);new AlertDialog.Builder(this).setTitle("Add radio to Custom list").setView(box).setPositiveButton("ADD",(d,w)->{String name=n.getText().toString().trim(),url=u.getText().toString().trim();if(!name.isEmpty()&&!url.isEmpty()){custom.add(new RadioStation(name,url));customAdapter.notifyDataSetChanged();}}).setNegativeButton("CANCEL",null).show();}
 void importList(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("text/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,77);}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r!=77||c!=RESULT_OK||d==null)return;try{InputStream in=getContentResolver().openInputStream(d.getData());String text=readStream(in);in.close();parseImport(text);status.setText("Imported list into Custom Radio");}catch(Exception e){status.setText("Import failed");}}
 void parseImport(String text){for(String line:text.split("\\R")){String s=line.trim();if(s.isEmpty()||s.startsWith("#"))continue;String[] p=s.split(",",3);if(p.length>=2&&p[1].trim().startsWith("http")){custom.add(new RadioStation(p[0].trim(),p[1].trim(),p.length>2?p[2].trim():""));}}customAdapter.notifyDataSetChanged();}
 void showWorld(){final ListView list=new ListView(this);StationAdapter a=new StationAdapter(this,world,new StationAdapter.Listener(){public void select(RadioStation s){selected=s;nowPlaying.setText("▶ "+s.name);status.setText("World selected");}public void favorite(RadioStation s){toggleFavorite(s);}});list.setAdapter(a);new Thread(()->RadioBrowserClient.topStations(new RadioBrowserClient.StationCallback(){public void result(List<RadioStation>x){runOnUiThread(()->{world.clear();world.addAll(x);a.notifyDataSetChanged();});}public void error(Exception e){runOnUiThread(()->status.setText("World Radio unavailable"));}})).start();new AlertDialog.Builder(this).setTitle("WORLD RADIO • RadioBrowser").setView(list).setPositiveButton("CLOSE",null).show();}
 void startUsage(){usageHandler.removeCallbacks(usage);usageBase=TrafficStats.getUidRxBytes(getApplicationInfo().uid);if(usageBase==TrafficStats.UNSUPPORTED)usageBase=0;minuteStart=System.currentTimeMillis();minute=1;usageHandler.post(usage);}
 void updateUsage(){if(minuteStart==0)return;long now=System.currentTimeMillis();while(now-minuteStart>=60000){minuteStart+=60000;minute++;usageBase=TrafficStats.getUidRxBytes(getApplicationInfo().uid);}long cur=TrafficStats.getUidRxBytes(getApplicationInfo().uid);if(cur==TrafficStats.UNSUPPORTED)cur=usageBase;double mb=Math.max(0,cur-usageBase)/(1024.0*1024.0);usagePerMinute.setText(String.format(Locale.US,"دقیقه %d: %.3f MB",minute,mb));}
 void stopUsage(){usageHandler.removeCallbacks(usage);minuteStart=0;}
 void loadFavorites(){ }
 void saveFavorites(){ }
 String readStream(InputStream in)throws Exception{BufferedReader r=new BufferedReader(new InputStreamReader(in,"UTF-8"));StringBuilder b=new StringBuilder();String s;while((s=r.readLine())!=null)b.append(s);return b.toString();}
 byte[] readBytes(InputStream in)throws Exception{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=in.read(buf))!=-1)b.write(buf,0,n);return b.toByteArray();}
 @Override protected void onDestroy(){sunHandler.removeCallbacksAndMessages(null);stopUsage();if(equalizer!=null)try{equalizer.release();}catch(Exception ignored){}legacyEngine.release();if(controllerFuture!=null)MediaController.releaseFuture(controllerFuture);super.onDestroy();}

    private void playStationFromView(View v) {
        try {
            Object tag = v.getTag();
            if (tag instanceof Station) {
                selected = (Station) tag;
                playSelected();
            }
        } catch (Exception ignored) {}
    }


 void showTvWindow(){
  final LinearLayout box=new LinearLayout(this);
  box.setOrientation(LinearLayout.VERTICAL);
  box.setPadding(12,6,12,6);

  final TextView channel=new TextView(this);
  channel.setTextSize(18);
  channel.setTextStyle(android.graphics.Typeface.BOLD);
  channel.setGravity(Gravity.CENTER);
  box.addView(channel,new LinearLayout.LayoutParams(-1,50));

  final TextView subtitle=new TextView(this);
  subtitle.setTextSize(14);
  subtitle.setGravity(Gravity.CENTER_VERTICAL);
  subtitle.setPadding(8,4,8,4);
  box.addView(subtitle,new LinearLayout.LayoutParams(-1,90));

  final LinearLayout controls=new LinearLayout(this);
  controls.setGravity(Gravity.CENTER);
  Button prev=new Button(this); prev.setText("‹");
  Button on=new Button(this); on.setText(tvTickerOn?"ON":"OFF");
  Button next=new Button(this); next.setText("›");
  controls.addView(prev,new LinearLayout.LayoutParams(70,55));
  controls.addView(on,new LinearLayout.LayoutParams(90,55));
  controls.addView(next,new LinearLayout.LayoutParams(70,55));
  box.addView(controls);

  final String[] channels={"BBC Persian","VOA Persian","Radio Farda","DW Persian","France 24","RFI Persian","Euronews Persian"};
  final int[] idx={Math.max(0,Math.min(tvIndex,channels.length-1))};

  Runnable refresh=new Runnable(){public void run(){
   channel.setText("TV NEWS • "+channels[idx[0]]);
   subtitle.setText(tvTickerOn
     ? "زیرنویس/خبر زنده: "+channels[idx[0]]+"\nدر صورت در دسترس بودن منبع آنلاین نمایش داده می‌شود."
     : "TV OFF • درخواست جدیدی ارسال نمی‌شود");
   on.setText(tvTickerOn?"ON":"OFF");
  }};
  prev.setOnClickListener(v->{idx[0]=(idx[0]+channels.length-1)%channels.length;tvIndex=idx[0];updateTvTicker();refresh.run();});
  next.setOnClickListener(v->{idx[0]=(idx[0]+1)%channels.length;tvIndex=idx[0];updateTvTicker();refresh.run();});
  on.setOnClickListener(v->{toggleTvTicker();refresh.run();});

  AlertDialog dlg=new AlertDialog.Builder(this)
    .setTitle("TV • NEWS SUBTITLE")
    .setView(box)
    .setPositiveButton("CLOSE",null)
    .create();
  dlg.setOnShowListener(v->refresh.run());
  dlg.show();
 }

    private void setupPlayerAndSlideControls() {
        View slide=findViewById(R.id.slideControls);
        View extras=findViewById(R.id.extraControls);
        if(slide!=null && extras!=null){
            slide.setOnClickListener(v -> {
                extras.setVisibility(extras.getVisibility()==View.VISIBLE?View.GONE:View.VISIBLE);
            });
        }
        TextView now=findViewById(R.id.playerNow);
        Button play=findViewById(R.id.playerPlay);
        Button stop=findViewById(R.id.playerStop);
        if(play!=null) play.setOnClickListener(v -> {
            if(selected!=null) {
                try { playSelected(); } catch(Exception e) { status.setText("Playback error"); }
                if(now!=null) now.setText("PLAYER • "+selected.name);
            } else if(now!=null) now.setText("PLAYER • Select a station");
        });
        if(stop!=null) stop.setOnClickListener(v -> {
            try { player.stop(); } catch(Exception ignored) {}
            if(now!=null) now.setText("PLAYER • Stopped");
        });
    }

    private void setupSafeVolumeControl() {
        SeekBar bar = findViewById(R.id.volumeSafe);
        TextView value = findViewById(R.id.volumeValue);
        if (bar == null) return;

        // Never send a player volume value above 1.0 to ExoPlayer.
        // Values above the phone's hardware maximum are not used; this prevents
        // invalid gain/clipping from stopping playback.
        bar.setMax(100);
        bar.setProgress(100);
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b, int p, boolean fromUser) {
                int safe = Math.max(0, Math.min(100, p));
                float playerVolume = safe / 100f;
                try {
                    if (player != null) player.setVolume(playerVolume);
                } catch (Exception ignored) {}
                if (value != null) value.setText("VOLUME " + safe + "%");
            }
            public void onStartTrackingTouch(SeekBar b) {}
            public void onStopTrackingTouch(SeekBar b) {}
        });
    }

}
