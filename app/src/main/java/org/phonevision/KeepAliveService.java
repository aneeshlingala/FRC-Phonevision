package org.phonevision;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;

/**
 * Foreground service (camera type) + partial wake lock. It keeps the process at foreground priority so
 * Android does not kill or freeze PhoneVision during a match, even if the screen turns off.
 */
public class KeepAliveService extends Service {
    private static final String CH = "phonevision_running";
    private PowerManager.WakeLock wl;

    @Override public int onStartCommand(Intent i, int flags, int id) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        nm.createNotificationChannel(new NotificationChannel(CH, "PhoneVision running", NotificationManager.IMPORTANCE_LOW));
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification n = new Notification.Builder(this, CH).setContentTitle("PhoneVision running")
                .setContentText("AprilTag vision active").setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true).setContentIntent(pi).build();
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
            else startForeground(1, n);
        } catch (RuntimeException e) { stopSelf(); return START_NOT_STICKY; }
        if (wl == null) {
            wl = ((PowerManager) getSystemService(POWER_SERVICE)).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "PhoneVision:vision");
            wl.acquire();
        }
        return START_STICKY;
    }

    @Override public void onDestroy() { if (wl != null && wl.isHeld()) wl.release(); super.onDestroy(); }
    @Override public IBinder onBind(Intent i) { return null; }
}
