package com.astuti.adevicetracker;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Native scrcpy H.264 stream surface. Uses the existing ws-scrcpy 1.19-ws8
 * stream endpoint and its binary control protocol. No WebView is involved.
 */
public class StreamActivity extends Activity implements android.view.SurfaceHolder.Callback {
    private final Handler main = new Handler(Looper.getMainLooper());
    private final AtomicBoolean running = new AtomicBoolean(false);
    private SurfaceView surfaceView;
    private TextView status;
    private StreamSocket socket;
    private Thread worker;
    private MediaCodec decoder;
    private Surface surface;
    private int videoWidth = 0;
    private int videoHeight = 0;
    private long presentationTimeUs = 0;
    private byte[] sps;
    private byte[] pps;
    private byte[] pending = new byte[0];
    private String udid = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(8, 12, 20));
        getWindow().setNavigationBarColor(Color.rgb(8, 12, 20));
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);
        root.addView(surfaceView, new FrameLayout.LayoutParams(-1, -1));
        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(13);
        status.setGravity(Gravity.CENTER);
        status.setPadding(18, 12, 18, 12);
        status.setBackgroundColor(0x99000000);
        FrameLayout.LayoutParams statusParams = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        root.addView(status, statusParams);
        setContentView(root);
        udid = getIntent().getStringExtra("udid");
        setStatus("Menghubungkan ke perangkat…");
    }

    private void setStatus(String value) {
        main.post(() -> { if (status != null) status.setText(value); });
    }

    @Override public void surfaceCreated(android.view.SurfaceHolder holder) {
        surface = holder.getSurface();
        String url = getIntent().getStringExtra("ws_url");
        if (url == null || url.isEmpty()) {
            setStatus("Alamat stream tidak tersedia.");
            return;
        }
        running.set(true);
        worker = new Thread(() -> runStream(url), "aDevice-native-stream");
        worker.start();
    }

    @Override public void surfaceChanged(android.view.SurfaceHolder holder, int format, int width, int height) { }
    @Override public void surfaceDestroyed(android.view.SurfaceHolder holder) {
        running.set(false);
        if (socket != null) socket.close();
        releaseDecoder();
        surface = null;
    }

    private void runStream(String url) {
        try {
            try {
                socket = new StreamSocket(url);
                socket.connect();
            } catch (Exception directError) {
                String fallback = getIntent().getStringExtra("fallback_ws_url");
                if (fallback == null || fallback.isEmpty() || fallback.equals(url) || !running.get()) throw directError;
                setStatus("Koneksi langsung gagal. Mencoba jalur proxy Debian…");
                if (socket != null) socket.close();
                socket = new StreamSocket(fallback);
                socket.connect();
            }
            setStatus("Stream tersambung. Menunggu metadata video…");
            while (running.get()) {
                byte[] frame = socket.readFrame();
                if (frame == null) break;
                if (isInitialInfo(frame)) {
                    parseInitialInfo(frame);
                    setStatus("Terhubung · " + (videoWidth > 0 ? videoWidth + "×" + videoHeight : "menyiapkan decoder"));
                } else if (isDeviceMessage(frame)) {
                    // Clipboard and file-push responses are not H.264 video packets.
                } else if (videoWidth > 0 && frame.length > 0) {
                    appendH264(frame);
                }
            }
            if (running.get()) setStatus("Stream terputus.");
        } catch (Exception e) {
            setStatus("Stream gagal: " + e.getMessage());
        } finally {
            running.set(false);
            releaseDecoder();
        }
    }

    private static boolean isInitialInfo(byte[] data) {
        byte[] magic = "scrcpy_initial".getBytes(StandardCharsets.UTF_8);
        return data.length >= magic.length && Arrays.equals(magic, Arrays.copyOf(data, magic.length));
    }

    private static boolean isDeviceMessage(byte[] data) {
        byte[] magic = "scrcpy_message".getBytes(StandardCharsets.UTF_8);
        return data.length >= magic.length && Arrays.equals(magic, Arrays.copyOf(data, magic.length));
    }

    private void parseInitialInfo(byte[] data) throws Exception {
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        b.position(14 + 64);
        int displays = b.getInt();
        for (int i = 0; i < displays; i++) {
            b.getInt(); // display id
            b.getInt(); b.getInt(); // display width and height
            b.getInt(); b.getInt(); b.getInt(); // rotation, layer stack, flags
            b.getInt(); // connection count
            int screenInfoLength = b.getInt();
            if (screenInfoLength > 0) {
                int base = b.position();
                if (screenInfoLength >= 25) {
                    b.position(base + 16);
                    videoWidth = b.getInt();
                    videoHeight = b.getInt();
                }
                b.position(base + screenInfoLength);
            }
            int videoSettingsLength = b.getInt();
            if (videoSettingsLength > 0) b.position(b.position() + videoSettingsLength);
        }
        int encoders = b.getInt();
        for (int i = 0; i < encoders; i++) {
            int length = b.getInt();
            b.position(b.position() + length);
        }
        b.getInt(); // client id
        if (videoWidth <= 0 || videoHeight <= 0) throw new Exception("ukuran layar tidak valid");
    }

    private synchronized void appendH264(byte[] incoming) throws Exception {
        byte[] joined = new byte[pending.length + incoming.length];
        System.arraycopy(pending, 0, joined, 0, pending.length);
        System.arraycopy(incoming, 0, joined, pending.length, incoming.length);
        int lastStart = -1;
        int i = 0;
        while (i + 3 < joined.length) {
            int startLength = startCodeLength(joined, i);
            if (startLength > 0) {
                if (lastStart >= 0 && i > lastStart) processNalu(Arrays.copyOfRange(joined, lastStart, i));
                lastStart = i;
                i += startLength;
            } else i++;
        }
        if (lastStart >= 0) pending = Arrays.copyOfRange(joined, lastStart, joined.length);
        else if (joined.length < 2 * 1024 * 1024) pending = joined;
        else pending = new byte[0];
    }

    private static int startCodeLength(byte[] b, int i) {
        if (i + 3 < b.length && b[i] == 0 && b[i+1] == 0 && b[i+2] == 0 && b[i+3] == 1) return 4;
        if (i + 2 < b.length && b[i] == 0 && b[i+1] == 0 && b[i+2] == 1) return 3;
        return 0;
    }

    private void processNalu(byte[] nalu) throws Exception {
        int prefix = startCodeLength(nalu, 0);
        if (prefix == 0 || nalu.length <= prefix) return;
        int type = nalu[prefix] & 0x1f;
        if (type == 7) { sps = nalu; configureDecoderIfReady(); return; }
        if (type == 8) { pps = nalu; configureDecoderIfReady(); return; }
        if (decoder == null) return;
        decodeNalu(nalu);
    }

    private void configureDecoderIfReady() throws Exception {
        if (decoder != null || sps == null || pps == null || surface == null) return;
        MediaFormat format = MediaFormat.createVideoFormat("video/avc", videoWidth, videoHeight);
        format.setByteBuffer("csd-0", ByteBuffer.wrap(sps));
        format.setByteBuffer("csd-1", ByteBuffer.wrap(pps));
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, Math.max(videoWidth * videoHeight, 1024 * 1024));
        decoder = MediaCodec.createDecoderByType("video/avc");
        decoder.configure(format, surface, null, 0);
        decoder.start();
        setStatus("Decoder aktif · " + videoWidth + "×" + videoHeight + " · sentuh layar untuk kontrol");
    }

    private void decodeNalu(byte[] nalu) {
        try {
            int input = decoder.dequeueInputBuffer(10000);
            if (input >= 0) {
                ByteBuffer buffer = decoder.getInputBuffer(input);
                if (buffer != null && buffer.capacity() >= nalu.length) {
                    buffer.clear();
                    buffer.put(nalu);
                    decoder.queueInputBuffer(input, 0, nalu.length, presentationTimeUs, 0);
                    presentationTimeUs += 33333;
                } else decoder.queueInputBuffer(input, 0, 0, presentationTimeUs, 0);
            }
            MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            int output;
            while ((output = decoder.dequeueOutputBuffer(info, 0)) >= 0) {
                decoder.releaseOutputBuffer(output, true);
            }
        } catch (Exception e) {
            setStatus("Decoder: " + e.getMessage());
        }
    }

    @Override public boolean dispatchTouchEvent(MotionEvent event) {
        if (socket != null && videoWidth > 0 && videoHeight > 0 && surfaceView != null) {
            int action = event.getActionMasked();
            if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE ||
                action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
                int mapped = action == MotionEvent.ACTION_DOWN ? 0 :
                    (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL ? 1 : 2);
                float vw = surfaceView.getWidth(), vh = surfaceView.getHeight();
                float scale = Math.min(vw / videoWidth, vh / videoHeight);
                float shownW = videoWidth * scale, shownH = videoHeight * scale;
                float left = (vw - shownW) / 2f, top = (vh - shownH) / 2f;
                float x = (event.getX() - left) / scale;
                float y = (event.getY() - top) / scale;
                boolean inside = x >= 0 && y >= 0 && x <= videoWidth && y <= videoHeight;
                if (inside || mapped == 1) {
                    int px = Math.max(0, Math.min(videoWidth, Math.round(x)));
                    int py = Math.max(0, Math.min(videoHeight, Math.round(y)));
                    socket.sendTouch(mapped, px, py, videoWidth, videoHeight,
                        mapped == 1 ? 0 : Math.max(1, Math.round(event.getPressure() * 65535f)));
                }
                return true;
            }
        }
        return super.dispatchTouchEvent(event);
    }

    private synchronized void releaseDecoder() {
        try { if (decoder != null) { decoder.stop(); decoder.release(); } } catch (Exception ignored) { }
        decoder = null;
    }

    @Override protected void onDestroy() {
        running.set(false);
        if (socket != null) socket.close();
        releaseDecoder();
        super.onDestroy();
    }

    private static final class StreamSocket {
        private final URI uri;
        private Socket socket;
        private InputStream in;
        private BufferedOutputStream out;
        private final SecureRandom random = new SecureRandom();

        StreamSocket(String url) throws Exception { uri = new URI(url); }

        void connect() throws Exception {
            String scheme = uri.getScheme();
            if (!"ws".equalsIgnoreCase(scheme)) throw new Exception("endpoint harus ws:// untuk backend HTTP saat ini");
            int port = uri.getPort() > 0 ? uri.getPort() : 80;
            socket = new Socket();
            socket.connect(new InetSocketAddress(uri.getHost(), port), 8000);
            socket.setSoTimeout(0);
            in = new BufferedInputStream(socket.getInputStream());
            out = new BufferedOutputStream(socket.getOutputStream());
            byte[] nonce = new byte[16]; random.nextBytes(nonce);
            String key = android.util.Base64.encodeToString(nonce, android.util.Base64.NO_WRAP);
            String path = (uri.getRawPath() == null || uri.getRawPath().isEmpty() ? "/" : uri.getRawPath());
            if (uri.getRawQuery() != null) path += "?" + uri.getRawQuery();
            String host = uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : "");
            String request = "GET " + path + " HTTP/1.1\r\nHost: " + host +
                "\r\nUpgrade: websocket\r\nConnection: Upgrade\r\nSec-WebSocket-Key: " + key +
                "\r\nSec-WebSocket-Version: 13\r\n\r\n";
            out.write(request.getBytes(StandardCharsets.US_ASCII)); out.flush();
            ByteArrayOutputStream headers = new ByteArrayOutputStream();
            int matched = 0;
            while (headers.size() < 8192) {
                int c = in.read(); if (c < 0) throw new Exception("server menutup koneksi");
                headers.write(c);
                if ((matched == 0 && c == 13) || (matched == 1 && c == 10) ||
                    (matched == 2 && c == 13) || (matched == 3 && c == 10)) {
                    matched++; if (matched == 4) break;
                } else matched = c == 13 ? 1 : 0;
            }
            String response = headers.toString("US-ASCII");
            if (!response.startsWith("HTTP/1.1 101") && !response.startsWith("HTTP/1.0 101"))
                throw new Exception("WebSocket ditolak: " + response.split("\\r\\n")[0]);
        }

        byte[] readFrame() throws Exception {
            int first = in.read(); if (first < 0) return null;
            int second = in.read(); if (second < 0) return null;
            int opcode = first & 15; boolean masked = (second & 128) != 0;
            long length = second & 127;
            if (length == 126) length = ((long)readByte() << 8) | readByte();
            else if (length == 127) { length = 0; for (int i=0;i<8;i++) length = (length << 8) | readByte(); }
            if (length > 8 * 1024 * 1024) throw new Exception("paket stream terlalu besar");
            byte[] mask = masked ? readBytes(4) : null;
            byte[] payload = readBytes((int)length);
            if (masked) for (int i=0;i<payload.length;i++) payload[i] ^= mask[i%4];
            if (opcode == 8) return null;
            if (opcode == 9) { sendFrame(10, payload); return new byte[0]; }
            return opcode == 2 ? payload : new byte[0];
        }

        synchronized void sendTouch(int action, int x, int y, int width, int height, int pressure) {
            try {
                ByteBuffer b = ByteBuffer.allocate(29).order(ByteOrder.BIG_ENDIAN);
                b.put((byte)2).put((byte)action).putLong(0L).putInt(x).putInt(y);
                b.putShort((short)width).putShort((short)height).putShort((short)pressure).putInt(1);
                sendFrame(2, b.array());
            } catch (Exception ignored) { }
        }

        private synchronized void sendFrame(int opcode, byte[] payload) throws Exception {
            if (out == null) return;
            byte[] mask = new byte[4]; random.nextBytes(mask);
            out.write(0x80 | opcode);
            if (payload.length < 126) out.write(0x80 | payload.length);
            else if (payload.length <= 65535) { out.write(0x80 | 126); out.write((payload.length >> 8) & 255); out.write(payload.length & 255); }
            else { out.write(0x80 | 127); for (int i=7;i>=0;i--) out.write((int)(((long)payload.length >> (8*i)) & 255)); }
            out.write(mask);
            for (int i=0;i<payload.length;i++) out.write(payload[i] ^ mask[i%4]);
            out.flush();
        }

        private int readByte() throws Exception { int v=in.read(); if(v<0) throw new Exception("frame terpotong"); return v; }
        private byte[] readBytes(int n) throws Exception { byte[] b=new byte[n]; int o=0; while(o<n){int r=in.read(b,o,n-o);if(r<0)throw new Exception("frame terpotong");o+=r;} return b; }
        void close() { try { if (socket != null) socket.close(); } catch (Exception ignored) { } }
    }
}