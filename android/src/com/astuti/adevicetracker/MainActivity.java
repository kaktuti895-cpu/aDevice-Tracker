package com.astuti.adevicetracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.SharedPreferences;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.util.Base64;

public class MainActivity extends Activity {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService io = Executors.newCachedThreadPool();
    private final Map<String, JSONObject> devices = new LinkedHashMap<>();
    private EditText hostInput, portInput;
    private TextView connectionLabel, countLabel, emptyLabel;
    private LinearLayout deviceList;
    private Button connectButton;
    private volatile TrackerSocket tracker;
    private boolean closing = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(12, 18, 30));
        getWindow().setNavigationBarColor(Color.rgb(12, 18, 30));
        getWindow().getDecorView().setSystemUiVisibility(0);
        buildUi();
    }

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String value, int size, int color, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD); return t;
    }
    private void buildUi() {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(Color.rgb(12,18,30));
        root.setPadding(dp(18), dp(18), dp(18), dp(12));
        TextView title = text("aDevice Tracker", 25, Color.WHITE, true);
        root.addView(title);
        TextView subtitle = text("NATIVE ANDROID CLIENT  ·  DEBIAN", 10, Color.rgb(139,158,183), true);
        LinearLayout.LayoutParams subp = new LinearLayout.LayoutParams(-1,-2); subp.topMargin=dp(4); root.addView(subtitle,subp);

        LinearLayout connection = new LinearLayout(this); connection.setOrientation(LinearLayout.VERTICAL); connection.setPadding(dp(14),dp(13),dp(14),dp(13)); connection.setBackground(bg(Color.rgb(25,36,54),16));
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(-1,-2); cp.topMargin=dp(20); root.addView(connection,cp);
        TextView endpoint = text("SERVER ENDPOINT",10,Color.rgb(139,158,183),true); connection.addView(endpoint);
        LinearLayout fields = new LinearLayout(this); fields.setOrientation(LinearLayout.HORIZONTAL); LinearLayout.LayoutParams fp=new LinearLayout.LayoutParams(-1,-2); fp.topMargin=dp(8); connection.addView(fields,fp);
        SharedPreferences prefs = getSharedPreferences("adevice_tracker", MODE_PRIVATE);
        hostInput = new EditText(this); hostInput.setSingleLine(true); hostInput.setText(prefs.getString("host", "192.168.2.233")); hostInput.setTextColor(Color.WHITE); hostInput.setHintTextColor(Color.GRAY); hostInput.setTextSize(14); hostInput.setHint("Debian IP"); hostInput.setPadding(dp(10),0,dp(10),0); hostInput.setBackground(bg(Color.rgb(12,20,34),10));
        fields.addView(hostInput,new LinearLayout.LayoutParams(0,dp(46),1));
        portInput = new EditText(this); portInput.setSingleLine(true); portInput.setInputType(2); portInput.setText(prefs.getString("port", "8000")); portInput.setTextColor(Color.WHITE); portInput.setTextSize(14); portInput.setPadding(dp(10),0,dp(10),0); portInput.setBackground(bg(Color.rgb(12,20,34),10));
        LinearLayout.LayoutParams pp=new LinearLayout.LayoutParams(dp(82),dp(46)); pp.leftMargin=dp(8); fields.addView(portInput,pp);
        connectButton = new Button(this); connectButton.setText("Hubungkan"); connectButton.setAllCaps(false); connectButton.setTextColor(Color.WHITE); connectButton.setBackground(bg(Color.rgb(36,111,214),10));
        LinearLayout.LayoutParams bp=new LinearLayout.LayoutParams(-1,dp(46)); bp.topMargin=dp(10); connection.addView(connectButton,bp);
        connectButton.setOnClickListener(v -> connect());

        LinearLayout status = new LinearLayout(this); status.setGravity(Gravity.CENTER_VERTICAL); status.setPadding(dp(2),dp(18),dp(2),dp(10));
        connectionLabel=text("●  Belum terhubung",13,Color.rgb(255,190,95),true); status.addView(connectionLabel,new LinearLayout.LayoutParams(0,-2,1));
        countLabel=text("0 perangkat",12,Color.rgb(139,158,183),false); status.addView(countLabel); root.addView(status);

        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(-1,0,1); root.addView(scroll,sp);
        LinearLayout content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); scroll.addView(content);
        emptyLabel=text("Belum ada perangkat. Hubungkan ke server Debian untuk memuat daftar perangkat.",14,Color.rgb(139,158,183),false); emptyLabel.setGravity(Gravity.CENTER); emptyLabel.setPadding(dp(20),dp(40),dp(20),dp(40)); content.addView(emptyLabel);
        deviceList=new LinearLayout(this); deviceList.setOrientation(LinearLayout.VERTICAL); content.addView(deviceList);
        TextView footer=text("Web Remote UI tetap tersedia di browser: http://192.168.2.233:8000/",10,Color.rgb(103,122,148),false); footer.setPadding(0,dp(12),0,0); root.addView(footer);
        setContentView(root);
    }

    private void connect() {
        String host=hostInput.getText().toString().trim(); String portText=portInput.getText().toString().trim();
        if(host.isEmpty() || portText.isEmpty()) { Toast.makeText(this,"Isi alamat Debian dan port",Toast.LENGTH_SHORT).show(); return; }
        final int port; try { port=Integer.parseInt(portText); if(port<1 || port>65535) throw new Exception(); } catch(Exception e) { Toast.makeText(this,"Port tidak valid",Toast.LENGTH_SHORT).show(); return; }
        getSharedPreferences("adevice_tracker", MODE_PRIVATE).edit().putString("host", host).putString("port", portText).apply();
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(portInput.getWindowToken(),0);
        closeTracker(); devices.clear(); renderDevices(); setStatus("●  Menghubungkan ke "+host+":"+port,Color.rgb(255,190,95)); connectButton.setEnabled(false); connectButton.setText("Menghubungkan…");
        TrackerSocket s=new TrackerSocket(host,port); tracker=s;
        io.execute(() -> {
            try { s.connect(); if(tracker!=s){s.close();return;} main.post(() -> { connectButton.setEnabled(true); connectButton.setText("Hubungkan ulang"); }); s.readLoop(); }
            catch(Exception e) { if(tracker==s) main.post(() -> { setStatus("●  Gagal: "+e.getMessage(),Color.rgb(255,120,120)); connectButton.setEnabled(true); connectButton.setText("Coba lagi"); }); s.close(); }
        });
    }
    private void closeTracker() { closing=true; TrackerSocket s=tracker; tracker=null; if(s!=null) s.close(); closing=false; }
    private void setStatus(String s,int color) { connectionLabel.setText(s); connectionLabel.setTextColor(color); }
    private void onMessage(String raw) {
        try {
            JSONObject m=new JSONObject(raw); String type=m.optString("type"); JSONObject data=m.optJSONObject("data"); if(data==null)return;
            if("devicelist".equals(type)) {
                JSONArray a=data.optJSONArray("list"); devices.clear(); if(a!=null) for(int i=0;i<a.length();i++){ JSONObject d=a.optJSONObject(i); if(d!=null) devices.put(d.optString("udid","device-"+i),d); }
                renderDevices();
            } else if("device".equals(type)) {
                JSONObject d=data.optJSONObject("device"); if(d!=null){ devices.put(d.optString("udid","unknown"),d); renderDevices(); }
            }
        } catch(Exception ignored) { }
    }
    private void renderDevices() {
        deviceList.removeAllViews(); countLabel.setText(devices.size()+ (devices.size()==1?" perangkat":" perangkat"));
        emptyLabel.setVisibility(devices.isEmpty()?View.VISIBLE:View.GONE);
        for(JSONObject d:devices.values()) {
            String maker=d.optString("ro.product.manufacturer","Android"); String model=d.optString("ro.product.model","Perangkat");
            String state=d.optString("state","unknown"); String udid=d.optString("udid","-"); String android=d.optString("ro.build.version.release","-"); String sdk=d.optString("ro.build.version.sdk","-"); String pid=d.optString("pid","-1");
            boolean active="device".equals(state); int green=Color.rgb(72,205,145);
            LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14),dp(13),dp(14),dp(13)); card.setBackground(bg(Color.rgb(25,36,54),15)); LinearLayout.LayoutParams cardp=new LinearLayout.LayoutParams(-1,-2); cardp.bottomMargin=dp(10); deviceList.addView(card,cardp);
            LinearLayout head=new LinearLayout(this); head.setGravity(Gravity.CENTER_VERTICAL); card.addView(head);
            TextView name=text((maker+" "+model).trim(),17,Color.WHITE,true); head.addView(name,new LinearLayout.LayoutParams(0,-2,1));
            TextView badge=text(active?"ONLINE":"OFFLINE",10,active?green:Color.LTGRAY,true); badge.setPadding(dp(9),dp(5),dp(9),dp(5)); badge.setBackground(bg(Color.rgb(36,58,66),20)); head.addView(badge);
            TextView serial=text(udid,11,Color.rgb(139,158,183),false); serial.setPadding(0,dp(8),0,dp(3)); card.addView(serial);
            TextView detail=text("Android "+android+"  ·  SDK "+sdk+"  ·  Server PID "+pid,12,Color.rgb(190,204,222),false); card.addView(detail);
            JSONArray interfaces=d.optJSONArray("interfaces"); if(interfaces!=null && interfaces.length()>0){ StringBuilder b=new StringBuilder("Interface: "); for(int i=0;i<interfaces.length();i++){ if(i>0)b.append(", "); JSONObject n=interfaces.optJSONObject(i); b.append(n==null?interfaces.optString(i):n.optString("name",n.toString())); } TextView net=text(b.toString(),11,Color.rgb(139,158,183),false); net.setPadding(0,dp(6),0,0); card.addView(net); }
            if(active){ Button streamButton=new Button(this); streamButton.setText("Lihat layar & kontrol"); streamButton.setAllCaps(false); streamButton.setTextColor(Color.WHITE); streamButton.setBackground(bg(Color.rgb(36,111,214),10)); LinearLayout.LayoutParams streamParams=new LinearLayout.LayoutParams(-1,dp(44)); streamParams.topMargin=dp(12); card.addView(streamButton,streamParams); streamButton.setOnClickListener(v -> openStream(d)); }
        }
    }
    private void openStream(JSONObject device) {
        String udid=device.optString("udid","");
        if(udid.isEmpty()){ Toast.makeText(this,"Serial perangkat tidak tersedia",Toast.LENGTH_SHORT).show(); return; }
        String address=""; String backupAddress="";
        String preferredInterface=device.optString("wifi.interface","");
        JSONArray interfaces=device.optJSONArray("interfaces");
        if(interfaces!=null){
            for(int i=0;i<interfaces.length();i++){
                JSONObject item=interfaces.optJSONObject(i);
                if(item!=null){
                    String ip=item.optString("ipv4","").trim();
                    String name=item.optString("name","").trim();
                    if(ip.isEmpty() || "0.0.0.0".equals(ip) || "127.0.0.1".equals(ip)) continue;
                    if(!preferredInterface.isEmpty() && preferredInterface.equals(name)){ address=ip; break; }
                    if(name.startsWith("wlan") || name.startsWith("wifi") || name.startsWith("eth")){ if(backupAddress.isEmpty()) backupAddress=ip; }
                    else if(backupAddress.isEmpty() && !name.startsWith("ccmni")) backupAddress=ip;
                }
            }
        }
        if(address.isEmpty()) address=backupAddress;
        String host=hostInput.getText().toString().trim(); String port=portInput.getText().toString().trim();
        if(host.isEmpty() || port.isEmpty()){ Toast.makeText(this,"Alamat server Debian belum diisi",Toast.LENGTH_SHORT).show(); return; }
        Uri.Builder proxyBuilder=new Uri.Builder().scheme("ws").encodedAuthority(host+":"+port).path("/")
            .appendQueryParameter("action","proxy-adb")
            .appendQueryParameter("remote","tcp:8886")
            .appendQueryParameter("udid",udid);
        Uri.Builder directBuilder=new Uri.Builder().scheme("ws").encodedAuthority((address.isEmpty()?host:address)+":"+(address.isEmpty()?port:"8886")).path("/")
            .appendQueryParameter("udid",udid);
        Intent intent=new Intent(this,StreamActivity.class);
        intent.putExtra("udid",udid);
        intent.putExtra("ws_url",address.isEmpty()?proxyBuilder.build().toString():directBuilder.build().toString());
        intent.putExtra("fallback_ws_url",proxyBuilder.build().toString());
        startActivity(intent);
    }

    @Override protected void onDestroy(){ closeTracker(); io.shutdownNow(); super.onDestroy(); }

    private final class TrackerSocket {
        private final String host; private final int port; private Socket socket; private InputStream in; private BufferedOutputStream out;
        TrackerSocket(String h,int p){host=h;port=p;}
        void connect() throws Exception {
            socket=new Socket(); socket.connect(new InetSocketAddress(host,port),7000); socket.setSoTimeout(0); in=new BufferedInputStream(socket.getInputStream()); out=new BufferedOutputStream(socket.getOutputStream());
            byte[] nonce=new byte[16]; new SecureRandom().nextBytes(nonce); String key=Base64.encodeToString(nonce,Base64.NO_WRAP);
            String req="GET /?action=goog-device-list HTTP/1.1\r\nHost: "+host+":"+port+"\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: "+key+"\r\nSec-WebSocket-Version: 13\r\n\r\n";
            out.write(req.getBytes(StandardCharsets.US_ASCII)); out.flush(); ByteArrayOutputStream headers=new ByteArrayOutputStream(); int match=0; while(headers.size()<8192){ int c=in.read(); if(c<0)throw new Exception("server menutup koneksi"); headers.write(c); if((match==0&&c==13)||(match==1&&c==10)||(match==2&&c==13)||(match==3&&c==10)){match++;if(match==4)break;}else match=(c==13?1:0); }
            String response=headers.toString("US-ASCII"); if(!response.startsWith("HTTP/1.1 101")&&!response.startsWith("HTTP/1.0 101")) throw new Exception("WebSocket ditolak: "+response.split("\\r\\n")[0]);
            main.post(() -> setStatus("●  Terhubung · tracker aktif",Color.rgb(72,205,145)));
        }
        void readLoop() throws Exception {
            while(socket!=null&&!socket.isClosed()) {
                int b1=in.read(); if(b1<0)throw new Exception("koneksi terputus"); int b2=in.read(); if(b2<0)throw new Exception("frame tidak lengkap"); int opcode=b1&15; boolean masked=(b2&128)!=0; long len=b2&127;
                if(len==126) len=((long)readByte()<<8)|readByte(); else if(len==127){len=0;for(int i=0;i<8;i++)len=(len<<8)|readByte();}
                if(len>4*1024*1024)throw new Exception("frame terlalu besar"); byte[] mask=masked?readBytes(4):null; byte[] payload=readBytes((int)len); if(masked)for(int i=0;i<payload.length;i++)payload[i]^=mask[i%4];
                if(opcode==8)throw new Exception("server menutup koneksi"); if(opcode==9){writeFrame(10,payload);continue;} if(opcode==1){String msg=new String(payload,StandardCharsets.UTF_8);main.post(()->onMessage(msg));}
            }
        }
        int readByte() throws Exception {int x=in.read();if(x<0)throw new Exception("frame terpotong");return x;}
        byte[] readBytes(int n)throws Exception{byte[] b=new byte[n];int o=0;while(o<n){int r=in.read(b,o,n-o);if(r<0)throw new Exception("frame terpotong");o+=r;}return b;}
        synchronized void writeFrame(int opcode,byte[] data)throws Exception{if(out==null)return;SecureRandom r=new SecureRandom();byte[] mask=new byte[4];r.nextBytes(mask);out.write(0x80|opcode);if(data.length<126)out.write(0x80|data.length);else{out.write(0x80|126);out.write((data.length>>8)&255);out.write(data.length&255);}out.write(mask);for(int i=0;i<data.length;i++)out.write(data[i]^mask[i%4]);out.flush();}
        void close(){try{if(socket!=null)socket.close();}catch(Exception ignored){}}
    }
}