package com.ordis.volumedock;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView status;
    private Button overlayButton, notificationButton, batteryButton, policyButton;
    private boolean pendingStart = false;

    private int dp(int x) { return Math.round(getResources().getDisplayMetrics().density * x); }
    private GradientDrawable bg(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color); g.setCornerRadius(dp(14)); return g;
    }
    private TextView label(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setPadding(0, dp(7), 0, dp(7)); return t;
    }
    private Button button(String s, int color) {
        Button b = new Button(this); b.setText(s); b.setAllCaps(false);
        b.setTextColor(Color.WHITE); b.setBackground(bg(color));
        b.setMinHeight(dp(54)); return b;
    }
    private void add(LinearLayout l, View v, int margin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1,-2);
        p.bottomMargin = dp(margin); l.addView(v,p);
    }
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        ScrollView sc = new ScrollView(this); sc.setFillViewport(true);
        sc.setBackgroundColor(Color.rgb(11,20,34));
        LinearLayout col = new LinearLayout(this); col.setOrientation(1);
        col.setPadding(dp(22),dp(28),dp(22),dp(30)); sc.addView(col);
        TextView title=label("VOLUME DOCK",28,Color.WHITE);
        title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); add(col,title,4);
        add(col,label("OnePlus 7T Pro · Botones de volumen flotantes",15,0xFFAFC4D4),22);
        status=label("Comprobando permisos...",17,0xFF73DCFF); add(col,status,20);
        add(col,label("1 · PERMISOS NECESARIOS",14,0xFF73DCFF),8);
        overlayButton=button("Permitir mostrar sobre otras apps",0xFF25415A);
        overlayButton.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else Toast.makeText(this,"Superposición autorizada",Toast.LENGTH_SHORT).show();
        }); add(col,overlayButton,10);
        notificationButton=button("Permitir notificación",0xFF25415A);
        notificationButton.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},55);
            else Toast.makeText(this,"No requiere más permisos",Toast.LENGTH_SHORT).show();
        }); add(col,notificationButton,10);
        batteryButton=button("Excluir del ahorro de batería",0xFF25415A);
        batteryButton.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName()))); }
            catch (Exception e) {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            }
        }); add(col,batteryButton,10);
        policyButton=button("Autorizar silencio de timbre (No molestar)",0xFF25415A);
        policyButton.setOnClickListener(v -> {
            NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(nm!=null && nm.isNotificationPolicyAccessGranted())
                Toast.makeText(this,"Control de silencio autorizado",Toast.LENGTH_SHORT).show();
            else startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
        }); add(col,policyButton,20);
        add(col,label("2 · ACTIVAR PANEL FLOTANTE",14,0xFF73DCFF),9);
        Button activate=button("Activar Volume Dock",0xFF146F93);
        activate.setOnClickListener(v -> startDock()); add(col,activate,14);
        add(col,label("Altavoz superior: abre el volumen original de Android sin cambiar su nivel. Candado: bloquea o libera el arrastre. Altavoz tachado: pone a cero multimedia, timbre, notificaciones, sistema y alarmas cuando Android lo permite. Atención: también silencia alarmas programadas; no modifica el audio de llamadas en curso.",14,0xFFD4E1EB),17);
        Button system=button("Abrir ajustes de la aplicación",0xFF25415A);
        system.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName())))); add(col,system,14);
        add(col,label("Para detener la aplicación manualmente: Ajustes del teléfono → Aplicaciones → Volume Dock → Forzar detención. En OxygenOS habilita también la ejecución en segundo plano y la exclusión de batería. Android puede finalizar procesos ante ciertas restricciones; no existe persistencia absoluta.",13,0xFFAFC4D4),8);
        setContentView(sc);
    }
    private boolean excluded() {
        PowerManager pm=(PowerManager)getSystemService(POWER_SERVICE);
        return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
    }
    private void startDock() {
        if (!Settings.canDrawOverlays(this)) {
            pendingStart=true; overlayButton.performClick(); return;
        }
        try {
            startForegroundService(new Intent(this,FloatingService.class));
            getSharedPreferences("dock",0).edit().putBoolean("enabled",true).apply();
            Toast.makeText(this,"Volume Dock activado",Toast.LENGTH_SHORT).show();
        } catch (RuntimeException e) {
            Toast.makeText(this,"Android ha rechazado el inicio del servicio",Toast.LENGTH_LONG).show();
        }
        refresh();
    }
    private void refresh() {
        boolean enabled=getSharedPreferences("dock",0).getBoolean("enabled",false);
        boolean overlay=Settings.canDrawOverlays(this);
        status.setText(overlay ? (enabled ? "Control flotante activado" : "Permisos: listo para activar")
                : "Pendiente: permiso de superposición");
        overlayButton.setText(overlay ? "✓ Superposición autorizada" : "Permitir mostrar sobre otras apps");
        batteryButton.setText(excluded() ? "✓ Ahorro de batería excluido" : "Excluir del ahorro de batería");
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        boolean policy=nm!=null && nm.isNotificationPolicyAccessGranted();
        policyButton.setText(policy ? "✓ Silencio de timbre autorizado"
                : "Autorizar silencio de timbre (No molestar)");
        notificationButton.setVisibility(Build.VERSION.SDK_INT >= 33 ? View.VISIBLE : View.GONE);
        if (Build.VERSION.SDK_INT >= 33) {
            boolean granted=checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
            notificationButton.setText(granted ? "✓ Notificaciones autorizadas" : "Permitir notificación");
        }
    }
    @Override protected void onResume() {
        super.onResume(); refresh();
        if (pendingStart && Settings.canDrawOverlays(this)) {
            pendingStart=false; startDock();
        }
    }
    @Override public void onRequestPermissionsResult(int req,String[] perms,int[] results) {
        super.onRequestPermissionsResult(req,perms,results); refresh();
    }
}
