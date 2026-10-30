package com.ordis.volumedock;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.IBinder;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;

public class FloatingService extends Service {
    private WindowManager window;
    private WindowManager.LayoutParams position;
    private Dock dock;
    private AudioManager audio;
    private SharedPreferences prefs;
    private static final String CHANNEL="volume_dock_active";
    private int dp(float n) { return Math.round(getResources().getDisplayMetrics().density*n); }
    @Override public void onCreate() {
        super.onCreate();
        window=(WindowManager)getSystemService(WINDOW_SERVICE);
        audio=(AudioManager)getSystemService(AUDIO_SERVICE);
        prefs=getSharedPreferences("dock",0);
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        NotificationChannel nc=new NotificationChannel(CHANNEL,"Volume Dock activo",
                NotificationManager.IMPORTANCE_LOW);
        nc.setDescription("Sustituye los botones físicos de volumen");
        nm.createNotificationChannel(nc);
    }
    @Override public int onStartCommand(Intent intent,int flags,int id) {
        if (!Settings.canDrawOverlays(this)) { stopSelf(); return START_NOT_STICKY; }
        try {
            Intent open=new Intent(this,MainActivity.class);
            PendingIntent pi=PendingIntent.getActivity(this,0,open,
                    PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Notification n=new Notification.Builder(this,CHANNEL)
                    .setSmallIcon(R.drawable.ic_volume)
                    .setContentTitle("Volume Dock activo")
                    .setContentText("Abrir volumen, silencio y candado")
                    .setContentIntent(pi).setOngoing(true)
                    .setCategory(Notification.CATEGORY_SERVICE).build();
            startForeground(1001,n);
            if (dock==null) createDock();
            prefs.edit().putBoolean("enabled",true).apply();
            return START_STICKY;
        } catch (RuntimeException ex) {
            Log.e("VolumeDock","No se pudo dibujar panel",ex);
            stopSelf(); return START_NOT_STICKY;
        }
    }
    private void createDock() {
        dock=new Dock(this);
        DisplayMetrics m=getResources().getDisplayMetrics();
        int w=dp(64),h=dp(192);
        int defaultX=m.widthPixels-w-dp(12),defaultY=(m.heightPixels-h)/2;
        position=new WindowManager.LayoutParams(w,h,WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        position.gravity=Gravity.TOP|Gravity.LEFT;
        position.x=prefs.getInt("x",defaultX);position.y=prefs.getInt("y",defaultY);
        move(position.x,position.y,false);
        window.addView(dock,position);
    }
    private void move(int x,int y,boolean save) {
        if (position==null) return;
        DisplayMetrics m=getResources().getDisplayMetrics();
        int pad=dp(4);
        position.x=Math.max(pad,Math.min(x,Math.max(pad,m.widthPixels-position.width-pad)));
        position.y=Math.max(pad,Math.min(y,Math.max(pad,m.heightPixels-position.height-pad)));
        if (dock!=null && dock.isAttachedToWindow()) window.updateViewLayout(dock,position);
        if (save) prefs.edit().putInt("x",position.x).putInt("y",position.y).apply();
    }
    /** Displays the native Android volume control without changing its level. */
    private void showSystemVolume() {
        if (audio==null) return;
        try {
            audio.adjustSuggestedStreamVolume(AudioManager.ADJUST_SAME,
                AudioManager.USE_DEFAULT_STREAM_TYPE,AudioManager.FLAG_SHOW_UI);
        } catch (RuntimeException e) {
            Log.w("VolumeDock","System volume UI unavailable",e);
        }
    }

    /** Brings audible streams to zero, leaving in-call and emergency audio untouched. */
    private void silencePhone() {
        if (audio==null) return;
        int[] streams = {
            AudioManager.STREAM_RING,
            AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_ALARM,
            AudioManager.STREAM_MUSIC
        };
        for (int stream : streams) {
            try {
                audio.setStreamVolume(stream,0,
                    stream==AudioManager.STREAM_MUSIC ? AudioManager.FLAG_SHOW_UI : 0);
            } catch (RuntimeException e) {
                // Android may require Do Not Disturb access for ringtone changes.
                Log.w("VolumeDock","Could not silence audio stream "+stream,e);
            }
        }
    }
    @Override public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);
        if (position!=null) move(position.x,position.y,true);
    }
    @Override public void onDestroy() {
        if (dock!=null) {
            try { window.removeView(dock); } catch (Exception ignored) {}
            dock=null;
        }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent i) { return null; }

    final class Dock extends View {
        private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
        private final float slop;
        private float rawX,rawY;
        private int startX,startY;
        private boolean moving=false,down=false,gestureMoved=false;
        Dock(Context context) {
            super(context);
            slop=ViewConfiguration.get(context).getScaledTouchSlop();
            setClickable(true);
            setContentDescription("Abrir control de volumen, bloquear desplazamiento y silenciar teléfono");
        }
        private boolean locked() { return prefs.getBoolean("locked",true); }
        @Override protected void onDraw(Canvas c) {
            super.onDraw(c);
            c.save(); c.scale(getWidth()/64f,getHeight()/192f);
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(238,12,23,40));
            c.drawRoundRect(new RectF(1,1,63,191),22,22,paint);
            paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.2f);
            paint.setColor(Color.rgb(71,111,133));
            c.drawRoundRect(new RectF(2,2,62,190),20,20,paint);
            paint.setStyle(Paint.Style.FILL);paint.setColor(Color.rgb(69,96,113));
            c.drawRect(14,64,50,65,paint);c.drawRect(14,127,50,128,paint);
            drawSpeaker(c,32,false);
            drawSpeaker(c,160,true);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setColor(locked()?0xFF73DCFF:0xFFFFC86A);
            paint.setStrokeWidth(3f);
            if(locked()) c.drawArc(new RectF(24,77,40,100),180,180,false,paint);
            else {
                c.drawArc(new RectF(30,77,46,100),170,190,false,paint);
                c.drawLine(30,89,30,94,paint);
            }
            paint.setStyle(Paint.Style.FILL);
            c.drawRoundRect(new RectF(21,92,43,113),3,3,paint);
            paint.setColor(0xFF101B2C);c.drawCircle(32,101,2.5f,paint);
            c.drawRect(31,102,33,107,paint);
            c.restore();
        }
        /** Volume glyph and crossed-out speaker glyph, drawn without extra resources. */
        private void drawSpeaker(Canvas canvas,float centerY,boolean muted) {
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            Path speaker=new Path();
            speaker.moveTo(15,centerY-7);
            speaker.lineTo(23,centerY-7);
            speaker.lineTo(34,centerY-15);
            speaker.lineTo(34,centerY+15);
            speaker.lineTo(23,centerY+7);
            speaker.lineTo(15,centerY+7);
            speaker.close();
            canvas.drawPath(speaker,paint);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeCap(Paint.Cap.ROUND);
            paint.setStrokeWidth(2.8f);
            if (muted) {
                paint.setColor(0xFFFFC86A);
                canvas.drawLine(15,centerY+16,49,centerY-16,paint);
            } else {
                paint.setColor(Color.WHITE);
                canvas.drawArc(new RectF(33,centerY-11,45,centerY+11),-64,128,false,paint);
                canvas.drawArc(new RectF(35,centerY-17,54,centerY+17),-64,128,false,paint);
            }
        }
        @Override public boolean onTouchEvent(MotionEvent ev) {
            switch(ev.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    down=true; moving=false; gestureMoved=false;
                    rawX=ev.getRawX();rawY=ev.getRawY();
                    startX=position.x;startY=position.y;
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if(!down)return true;
                    float dx=ev.getRawX()-rawX,dy=ev.getRawY()-rawY;
                    if(Math.hypot(dx,dy)>slop)gestureMoved=true;
                    if(!locked() && gestureMoved) {
                        moving=true;move(startX+Math.round(dx),startY+Math.round(dy),false);
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    if(!down)return true;
                    down=false;
                    if(moving) move(position.x,position.y,true);
                    else if(!gestureMoved) {
                        float section=ev.getY()/(getHeight()/3f);
                        if(section<1f)showSystemVolume();
                        else if(section<2f) {
                            prefs.edit().putBoolean("locked",!locked()).apply();
                            invalidate();
                        } else silencePhone();
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    moving=false;gestureMoved=false;performClick();
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    if(moving)move(position.x,position.y,true);
                    moving=false;down=false;gestureMoved=false;
                    return true;
            }
            return super.onTouchEvent(ev);
        }
        @Override public boolean performClick() { super.performClick();return true; }
    }
}
