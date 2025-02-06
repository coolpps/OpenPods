package com.dosse.airpods.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.dosse.airpods.R;
import com.dosse.airpods.receivers.StartupReceiver;
import com.dosse.airpods.utils.Logger;
import com.dosse.airpods.utils.MIUIWarning;
import com.dosse.airpods.utils.PermissionUtils;

import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private BroadcastReceiver freshUI = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if Bluetooth LE is available on this device. If not, show an error
        BluetoothAdapter btAdapter = ((BluetoothManager)Objects.requireNonNull(getSystemService(Context.BLUETOOTH_SERVICE))).getAdapter();
        if (btAdapter == null || (btAdapter.isEnabled() && btAdapter.getBluetoothLeScanner() == null) || (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))) {
            startActivity(new Intent(MainActivity.this, NoBTActivity.class));
            finish();

            return;
        }

        // 如果还有权限没有被允许,进入权限设置引导
        if (!PermissionUtils.checkAllPermissions(this)) {
            startActivity(new Intent(MainActivity.this, IntroActivity.class));
            finish();
        } else {
            // 开启前台Service获取电量
            StartupReceiver.startPodsService(getApplicationContext());

            //Warn MIUI users that their rom has known issues
            MIUIWarning.show(this);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        // 点击菜单中的设置项,当前只有它
        if (item.getItemId() == R.id.menu_ab_settings) {
            startActivity(new Intent(this, SettingsActivity.class)); // Settings icon clicked
            return true;
        }

        return false;
    }

    @Override
    protected void onStart() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.dosse.airpods.freshMainUI");

        freshUI = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                TextView leftPod = (TextView)findViewById(R.id.leftPodText_main);
                TextView rightPod = (TextView)findViewById(R.id.rightPodText_main);
                TextView podCase = (TextView)findViewById(R.id.podCaseText_main);

                String action = intent.getAction();
                if (action.equals("com.dosse.airpods.freshMainUI")) {
                    // 接收到广播传来的数据
                    String leftPodText = intent.getStringExtra("leftPod");
                    String rightPodText = intent.getStringExtra("rightPod");
                    String podCaseText = intent.getStringExtra("podCase");
                    Logger.debug("leftPodText="+leftPodText);
                    Logger.debug("rightPodText="+rightPodText);
                    Logger.debug("podCaseText="+podCaseText);

                    leftPod.setText(leftPodText);
                    rightPod.setText(rightPodText);
                    podCase.setText(podCaseText);
                }
            }
        };

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.registerReceiver(freshUI, filter);

        super.onStart();
    }

    @Override
    protected void onPause() {
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(getApplicationContext());
        localBroadcastManager.unregisterReceiver(freshUI);

        super.onPause();
    }
}
