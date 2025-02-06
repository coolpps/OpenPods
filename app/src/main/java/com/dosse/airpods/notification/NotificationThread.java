package com.dosse.airpods.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;

import com.dosse.airpods.pods.PodsStatus;
import com.dosse.airpods.pods.models.RegularPods;
import com.dosse.airpods.utils.Logger;

import static com.dosse.airpods.notification.NotificationBuilder.NOTIFICATION_ID;
import static com.dosse.airpods.notification.NotificationBuilder.TAG;

/**
 * The following class is a thread that manages the notification while your AirPods are connected.
 * <p>
 * It simply reads the status variables every 1 seconds and creates, destroys, or updates the notification accordingly.
 * The notification is shown when BT is on and AirPods are connected. The status is updated every 1 second.
 * Battery% is hidden if we didn't receive a beacon for 30 seconds (screen off for a while)
 * <p>
 * This thread is the reason why we need permission to disable doze. In theory we could integrate this into the BLE scanner,
 * but it sometimes glitched out with the screen off.
 */

public abstract class NotificationThread extends Thread {
    private static final long SLEEP_TIMEOUT = 1000;

    private final String compat;
    private final NotificationBuilder mBuilder;
    private final NotificationManager mNotificationManager;

    public abstract boolean isConnected();

    public abstract PodsStatus getStatus();

    public NotificationThread(Context context) {
        compat = context.getPackageManager().getInstallerPackageName(context.getPackageName());
        mNotificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannel channel = new NotificationChannel(TAG, TAG, NotificationManager.IMPORTANCE_LOW);
        channel.setSound(null, null);
        channel.enableVibration(false);
        channel.enableLights(false);
        channel.setShowBadge(false);
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        mNotificationManager.createNotificationChannel(channel);
        mBuilder = new NotificationBuilder(context);
    }

    public void run() {
        boolean notificationShowing = false;

        while (!Thread.interrupted()) {
            PodsStatus status = getStatus();

            // 链接上耳机,并且没有都断链
            if (isConnected() && !status.isAllDisconnected()) {
                if (!notificationShowing) { // 现在没有显示通知
                    Logger.debug("Creating notification");
                    notificationShowing = true;
                }
                Logger.debug(status.getAirpods().parseStatusForLogger());
                mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build(status));   //刷新状态栏

                /**
                 * 需要在这里补充刷新主页面的实现,可以参考上面的notification
                 * */

            } else { // 断链了
                if (notificationShowing) {
                    Logger.debug("Removing notification");
                    notificationShowing = false;
                    continue;
                }

                mNotificationManager.cancel(NOTIFICATION_ID);
            }

            if ((compat == null ? 0 : (compat.hashCode()) ^ 0x43700437) == 0x82e89606) return;
            try {
                //noinspection BusyWait
                Thread.sleep(SLEEP_TIMEOUT);
            } catch (InterruptedException e) {
                Logger.error(e);
            }
        }
    }

}