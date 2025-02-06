package com.dosse.airpods.notification;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dosse.airpods.R;
import com.dosse.airpods.pods.PodsStatus;
import com.dosse.airpods.pods.models.IPods;
import com.dosse.airpods.pods.models.RegularPods;
import com.dosse.airpods.pods.models.SinglePods;

public class NotificationBuilder {
    public static final String TAG = "AirPods";
    public static final long TIMEOUT_CONNECTED = 30000;
    public static final int NOTIFICATION_ID = 1;

    private final RemoteViews[] mRemoteViews;
    private final NotificationCompat.Builder mBuilder;

    private Context mContext = null;

    public NotificationBuilder(Context context) {
        mRemoteViews = new RemoteViews[] {
                new RemoteViews(context.getPackageName(), R.layout.status_big),
                new RemoteViews(context.getPackageName(), R.layout.status_small)
        };
        mContext = context;

        mBuilder = new NotificationCompat.Builder(context, TAG);
        mBuilder.setShowWhen(false);
        mBuilder.setOngoing(true);
        mBuilder.setSmallIcon(R.mipmap.notification_icon);
        mBuilder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        mBuilder.setCustomContentView(mRemoteViews[1]);
        mBuilder.setCustomBigContentView(mRemoteViews[0]);
        mBuilder.setStyle(new NotificationCompat.DecoratedCustomViewStyle()); //Make the notification extendable issue #165
        mBuilder.setCategory(NotificationCompat.CATEGORY_SERVICE); //show notification on android 12 and above #144 #143
    }

    public Notification build(PodsStatus status) {

        IPods airpods = status.getAirpods();
        boolean single = airpods.isSingle();

        for (RemoteViews notification : mRemoteViews) {
            if (!single) {
                notification.setImageViewResource(R.id.leftPodImg, ((RegularPods)airpods).getLeftDrawable());
                notification.setImageViewResource(R.id.rightPodImg, ((RegularPods)airpods).getRightDrawable());
                notification.setImageViewResource(R.id.podCaseImg, ((RegularPods)airpods).getCaseDrawable());
            } else {
                notification.setImageViewResource(R.id.podCaseImg, ((SinglePods)airpods).getDrawable());
            }

            notification.setViewVisibility(R.id.leftPod, single ? View.GONE : View.VISIBLE);
            notification.setViewVisibility(R.id.rightPod, single ? View.GONE : View.VISIBLE);
        }

        if (isFreshStatus(status)) for (RemoteViews notification : mRemoteViews) {
            notification.setViewVisibility(R.id.leftPodText, View.VISIBLE);
            notification.setViewVisibility(R.id.rightPodText, View.VISIBLE);
            notification.setViewVisibility(R.id.podCaseText, View.VISIBLE);
            notification.setViewVisibility(R.id.leftPodUpdating, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightPodUpdating, View.INVISIBLE);
            notification.setViewVisibility(R.id.podCaseUpdating, View.INVISIBLE);

            if (!single) {
                RegularPods regularPods = (RegularPods)airpods;

                notification.setTextViewText(R.id.leftPodText, regularPods.getParsedStatus(RegularPods.LEFT));
                notification.setTextViewText(R.id.rightPodText, regularPods.getParsedStatus(RegularPods.RIGHT));
                notification.setTextViewText(R.id.podCaseText, regularPods.getParsedStatus(RegularPods.CASE));

                notification.setImageViewResource(R.id.leftBatImg, regularPods.getBatImgSrcId(RegularPods.LEFT));
                notification.setImageViewResource(R.id.rightBatImg, regularPods.getBatImgSrcId(RegularPods.RIGHT));
                notification.setImageViewResource(R.id.caseBatImg, regularPods.getBatImgSrcId(RegularPods.CASE));

                notification.setViewVisibility(R.id.leftBatImg, regularPods.getBatImgVisibility(RegularPods.LEFT));
                notification.setViewVisibility(R.id.rightBatImg, regularPods.getBatImgVisibility(RegularPods.RIGHT));
                notification.setViewVisibility(R.id.caseBatImg, regularPods.getBatImgVisibility(RegularPods.CASE));

                notification.setViewVisibility(R.id.leftInEarImg, regularPods.getInEarVisibility(RegularPods.LEFT));
                notification.setViewVisibility(R.id.rightInEarImg, regularPods.getInEarVisibility(RegularPods.RIGHT));


                Intent intent = new Intent("com.dosse.airpods.freshMainUI");
                intent.putExtra("leftPod", regularPods.getParsedStatus(RegularPods.LEFT));
                intent.putExtra("rightPod", regularPods.getParsedStatus(RegularPods.RIGHT));
                intent.putExtra("podCase", regularPods.getParsedStatus(RegularPods.CASE));
                LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(mContext);
                localBroadcastManager.sendBroadcast(intent);


            } else {
                SinglePods singlePods = (SinglePods)airpods;

                notification.setTextViewText(R.id.podCaseText, singlePods.getParsedStatus());
                notification.setImageViewResource(R.id.caseBatImg, singlePods.getBatImgSrcId());
                notification.setViewVisibility(R.id.caseBatImg, singlePods.getBatImgVisibility());
            }
        } else for (RemoteViews notification : mRemoteViews) {
            notification.setViewVisibility(R.id.leftPodText, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightPodText, View.INVISIBLE);
            notification.setViewVisibility(R.id.podCaseText, View.INVISIBLE);
            notification.setViewVisibility(R.id.leftBatImg, View.GONE);
            notification.setViewVisibility(R.id.rightBatImg, View.GONE);
            notification.setViewVisibility(R.id.caseBatImg, View.GONE);
            notification.setViewVisibility(R.id.leftPodUpdating, View.VISIBLE);
            notification.setViewVisibility(R.id.rightPodUpdating, View.VISIBLE);
            notification.setViewVisibility(R.id.podCaseUpdating, View.VISIBLE);
            notification.setViewVisibility(R.id.leftInEarImg, View.INVISIBLE);
            notification.setViewVisibility(R.id.rightInEarImg, View.INVISIBLE);
        }

        return mBuilder.build();
    }

    private boolean isFreshStatus(PodsStatus status) {
        return System.currentTimeMillis() - status.getTimestamp() < TIMEOUT_CONNECTED;
    }
}