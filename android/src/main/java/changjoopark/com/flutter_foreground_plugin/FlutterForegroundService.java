package changjoopark.com.flutter_foreground_plugin;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;


public class FlutterForegroundService extends Service {
    private static String TAG = "FlutterForegroundService";
    public static int ONGOING_NOTIFICATION_ID = 1;
    public static final String NOTIFICATION_CHANNEL_ID = "CHANNEL_ID";
    public static final String ACTION_STOP_SERVICE = "STOP";

    private boolean userStopForegroundService = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.d(TAG, "onStartCommand: Intent is null");
            return START_NOT_STICKY;
        }
        if (intent.getAction() == null) {
            Log.d(TAG, "onStartCommand: Intent action is null");
            return START_NOT_STICKY;
        }
        final String action = intent.getAction();
        Log.d(TAG, String.format("onStartCommand: %s", action));

        switch (action) {
            case FlutterForegroundPlugin.START_FOREGROUND_ACTION:
                PackageManager pm = getApplicationContext().getPackageManager();
                Intent notificationIntent = pm.getLaunchIntentForPackage(getApplicationContext().getPackageName());
                PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                        notificationIntent, PendingIntent.FLAG_IMMUTABLE);

                Bundle bundle = intent.getExtras();

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID,
                            "flutter_foreground_service_channel",
                            NotificationManager.IMPORTANCE_DEFAULT);
                    channel.setDescription("Channel for Sticky Notification");

                    ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                            .createNotificationChannel(channel);
                }

                Intent stopSelf = new Intent(this, FlutterForegroundService.class);
                stopSelf.setAction(ACTION_STOP_SERVICE);

                int falgs = PendingIntent.FLAG_CANCEL_CURRENT;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    falgs = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
                }
                PendingIntent pStopSelf = PendingIntent
                        .getService(this, 0, stopSelf, falgs);

                String userImage = bundle.getString("userlogo", "");

                RemoteViews customLayout = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification_layout);
                customLayout.setOnClickPendingIntent(R.id.llDecline, pStopSelf);
                customLayout.setTextViewText(R.id.title, bundle.getString("title"));
                customLayout.setTextViewText(R.id.content, bundle.getString("content", ""));
                if (userImage.isEmpty()) {

                }

                RemoteViews customSmallLayout = new RemoteViews(getApplicationContext().getPackageName(), R.layout.custom_notification_small_layout);
                customSmallLayout.setOnClickPendingIntent(R.id.llDecline, pStopSelf);
                customSmallLayout.setTextViewText(R.id.title, bundle.getString("title"));

                NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                        .setSmallIcon(getNotificationIcon(bundle.getString("icon")))
                        .setColor(bundle.getInt("color"))
                        .setCategory(NotificationCompat.CATEGORY_SERVICE)
                        .setContentIntent(pendingIntent)
                        .setUsesChronometer(bundle.getBoolean("chronometer"))
                        .setOngoing(true)
                        .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                        .setStyle(new NotificationCompat.DecoratedCustomViewStyle())
                        .setCustomContentView(customSmallLayout)
                        .setCustomBigContentView(customLayout);

                /*if (bundle.getBoolean("stop_action")) {
                    Intent stopSelf = new Intent(this, FlutterForegroundService.class);
                    stopSelf.setAction(ACTION_STOP_SERVICE);

                    PendingIntent pStopSelf = PendingIntent
                            .getService(this, 0, stopSelf, PendingIntent.FLAG_CANCEL_CURRENT);
                    builder.addAction(getNotificationIcon(bundle.getString("stop_icon")),
                            bundle.getString("stop_text"),
                            pStopSelf);
                }*/

                startForeground(ONGOING_NOTIFICATION_ID, builder.build());
                break;
            case FlutterForegroundPlugin.STOP_FOREGROUND_ACTION:
                stopFlutterForegroundService();
                break;
            case ACTION_STOP_SERVICE:
                stopFlutterForegroundService();
                FlutterForegroundPlugin.instance.notifyStopServiceFromNotification();
                break;
            default:
                break;
        }

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (!userStopForegroundService) {
            Log.d(TAG, "User close app, kill current process to avoid memory leak in other plugin.");
            android.os.Process.killProcess(android.os.Process.myPid());
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int getNotificationIcon(String iconName) {
        int resourceId = getApplicationContext().getResources().getIdentifier(iconName, "drawable", getApplicationContext().getPackageName());
        return resourceId;
    }

    private void stopFlutterForegroundService() {
        userStopForegroundService = true;
        stopForeground(true);
        stopSelf();
    }
}
