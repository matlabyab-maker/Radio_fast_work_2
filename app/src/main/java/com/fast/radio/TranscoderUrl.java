package com.fast.radio;
import android.content.Context; import android.net.Uri;
public class TranscoderUrl {
 public static String build(Context c,String source,String codec,int bitrate){String b=TranscoderConfig.base(c);if(b.isEmpty())return "";if(!b.endsWith("/"))b+="/";return b+"stream?url="+Uri.encode(source)+"&codec="+Uri.encode(codec)+"&bitrate="+bitrate;}
}
