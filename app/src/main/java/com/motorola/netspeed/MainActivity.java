package com.motorola.netspeed;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

import com.motorola.netspeed.service.ShizukuServiceManager;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {

    private static final int SHIZUKU_REQUEST_CODE = 1001;

    private TextView status;
    private TextView stateText;
    private Switch speedSwitch;
    private Button permissionButton;

    private ShizukuServiceManager serviceManager;
    private Shizuku.OnRequestPermissionResultListener permissionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        buildUi();

        serviceManager = new ShizukuServiceManager(this);
        serviceManager.setCallback(new ShizukuServiceManager.Callback() {
            @Override
            public void onConnected(IUserService service) {
                runOnUiThread(() -> {
                    status.setText("خدمة Shizuku UserService متصلة.");
                    speedSwitch.setEnabled(true);
                });
                queryCurrentState(service);
            }

            @Override
            public void onDisconnected() {
                runOnUiThread(() -> {
                    speedSwitch.setEnabled(false);
                    status.setText("انقطع اتصال خدمة Shizuku.");
                });
            }
        });

        permissionListener = (requestCode, grantResult) -> {
            if (requestCode == SHIZUKU_REQUEST_CODE) updateShizukuState();
        };
        Shizuku.addRequestPermissionResultListener(permissionListener);

        updateShizukuState();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(24));
        root.setBackgroundColor(Color.rgb(11, 15, 25));
        scroll.addView(root);

        TextView title = text("Motorola NetSpeed", 24, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        root.addView(title, match());

        TextView subtitle = text(
                "التحكم في إظهار وإخفاء سرعة الشبكة في شريط الحالة",
                14, Color.rgb(148, 163, 184));
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, margin(0, 8, 0, 24));

        TextView shizukuTitle = text("حالة Shizuku", 18, Color.WHITE);
        root.addView(shizukuTitle, margin(0, 0, 0, 8));

        permissionButton = new Button(this);
        permissionButton.setText("طلب إذن Shizuku");
        permissionButton.setOnClickListener(v -> requestShizukuPermission());
        root.addView(permissionButton, margin(0, 0, 0, 8));

        status = text("جارٍ التحقق...", 14, Color.rgb(203, 213, 225));
        root.addView(status, margin(0, 0, 0, 24));

        speedSwitch = new Switch(this);
        speedSwitch.setText("عرض سرعة الشبكة");
        speedSwitch.setTextColor(Color.WHITE);
        speedSwitch.setTextSize(18);
        speedSwitch.setPadding(0, dp(12), 0, dp(12));
        speedSwitch.setEnabled(false);
        speedSwitch.setOnCheckedChangeListener((button, checked) -> {
            IUserService service = serviceManager == null ? null : serviceManager.getService();
            if (service == null) {
                button.setChecked(!checked);
                status.setText("خدمة Shizuku غير جاهزة.");
                return;
            }

            button.setEnabled(false);
            new Thread(() -> {
                boolean ok;
                try {
                    ok = service.setInternetSpeed(checked);
                } catch (Exception e) {
                    ok = false;
                }

                final boolean result = ok;
                runOnUiThread(() -> {
                    button.setEnabled(true);
                    if (result) {
                        stateText.setText(checked ? "الحالة: مفعّل (1)" : "الحالة: معطّل (0)");
                        status.setText(checked
                                ? "تم تفعيل سرعة الشبكة."
                                : "تم إيقاف سرعة الشبكة.");
                    } else {
                        button.setChecked(!checked);
                        status.setText("فشل تنفيذ أمر Motorola. تحقق من Shizuku.");
                    }
                });
            }).start();
        });
        root.addView(speedSwitch, margin(0, 0, 0, 8));

        stateText = text("الحالة: غير معروفة", 14, Color.rgb(74, 222, 128));
        root.addView(stateText, margin(0, 0, 0, 20));

        TextView command = text(
                "/system/bin/cmd motsettings put global internet_speed_switch 1|0",
                12, Color.rgb(56, 189, 248));
        root.addView(command, margin(0, 0, 0, 8));

        TextView note = text(
                "يتطلب Shizuku. التطبيق لا يحتاج إلى root مباشرة؛ UserService يعمل بهوية Shizuku.",
                13, Color.rgb(148, 163, 184));
        root.addView(note, margin(0, 0, 0, 16));

        setContentView(scroll);
    }

    private void updateShizukuState() {
        boolean available = Shizuku.pingBinder();
        boolean permitted = available && hasShizukuPermission();

        if (!available) {
            status.setText("Shizuku غير مشغّل.");
            permissionButton.setEnabled(false);
            speedSwitch.setEnabled(false);
            return;
        }

        permissionButton.setEnabled(!permitted);
        if (!permitted) {
            status.setText("Shizuku متصل، لكن التطبيق يحتاج الإذن.");
            speedSwitch.setEnabled(false);
            return;
        }

        status.setText("Shizuku متصل ومصرّح للتطبيق.");
        permissionButton.setEnabled(false);
        if (!serviceManager.bind()) {
            status.setText("تعذر تشغيل Shizuku UserService.");
        }
    }

    private boolean hasShizukuPermission() {
        return Shizuku.pingBinder()
                && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    private void requestShizukuPermission() {
        if (!Shizuku.pingBinder()) {
            status.setText("شغّل Shizuku أولاً.");
            return;
        }
        try {
            Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
        } catch (RuntimeException e) {
            status.setText("تعذر طلب إذن Shizuku.");
        }
    }

    private void queryCurrentState(IUserService service) {
        new Thread(() -> {
            try {
                boolean enabled = service.getInternetSpeed();
                runOnUiThread(() -> {
                    speedSwitch.setChecked(enabled);
                    stateText.setText(enabled
                            ? "الحالة: مفعّل (1)"
                            : "الحالة: معطّل (0)");
                });
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("تعذر قراءة حالة Motorola."));
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        if (permissionListener != null) {
            Shizuku.removeRequestPermissionResultListener(permissionListener);
        }
        if (serviceManager != null) {
            serviceManager.unbind();
        }
        super.onDestroy();
    }

    private TextView text(String value, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(sp);
        v.setTextColor(color);
        return v;
    }

    private LinearLayout.LayoutParams match() {
        return new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private LinearLayout.LayoutParams margin(int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = match();
        p.setMargins(dp(l), dp(t), dp(r), dp(b));
        return p;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
