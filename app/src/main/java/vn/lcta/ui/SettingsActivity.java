package vn.lcta.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.hardware.Sensor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import vn.lcta.BuildConfig;
import vn.lcta.Prefs;
import vn.lcta.data.DataHub;
import vn.lcta.data.WeatherSource;
import vn.lcta.net.Http;
import vn.lcta.net.Tls;
import vn.lcta.sys.AdminReceiver;
import vn.lcta.sys.Scheduler;
import vn.lcta.sys.WakeService;
import vn.lcta.update.Updater;

/** Màn Cài đặt dựng bằng code (chạy được trên Android 4.0.3 với giao diện Holo). */
public class SettingsActivity extends Activity implements Updater.Listener {
    private static final int REQ_ADMIN = 7;

    private Prefs prefs;
    private Updater updater;
    private final Handler handler = new Handler();
    private float density;

    private EditText place, watchlist, feeds, rotate, fontScale, dayStart, dayEnd, idle, nightBright, dayBright, installHour;
    private TextView placeState, adminState, updateState, versionText;
    private Button adminButton, installButton;
    private CheckBox alwaysOn, burnIn, proximity, wakeMorning, autoStart, autoUpdate;
    private RadioGroup layoutGroup, nightGroup;
    private final List<CheckBox> pageChecks = new ArrayList<CheckBox>();
    private boolean geocoding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = new Prefs(this);
        updater = Updater.get(this);
        density = getResources().getDisplayMetrics().density;
        setContentView(build());
        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updater.addListener(this);
        refreshStates();
    }

    @Override
    protected void onPause() {
        updater.removeListener(this);
        save(false);
        super.onPause();
    }

    @Override
    public void onUpdateChanged() {
        refreshStates();
    }

    // ------------------------------------------------------------------ dựng giao diện

    private int dp(float v) {
        return Math.round(v * density);
    }

    private View build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(8), dp(20), dp(40));

        section(root, "1. Thời tiết");
        place = input(root, "Thành phố, vd Hà Nội, Đà Nẵng, Cần Thơ", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        button(root, "Tìm và chọn thành phố", new Runnable() {
            @Override
            public void run() {
                geocode(true);
            }
        });
        placeState = note(root, "");

        section(root, "2. Chứng khoán");
        label(root, "Mã theo dõi, cách nhau dấu phẩy (chỉ số: VNINDEX, VN30, HNX, HNX30, UPCOM)");
        watchlist = input(root, Prefs.DEFAULT_WATCHLIST, InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        section(root, "3. Tin tức");
        label(root, "Nguồn RSS, mỗi dòng một link");
        feeds = input(root, "https://…", InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        feeds.setSingleLine(false);
        feeds.setMinLines(3);
        feeds.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);

        section(root, "4. Trang hiển thị");
        label(root, "Các trang tự xoay vòng bên phải (chạm để sang trang, vuốt ngang để chuyển)");
        for (int i = 0; i < Prefs.PAGE_KEYS.length; i++) {
            CheckBox c = check(root, Prefs.PAGE_NAMES[i]);
            c.setTag(Prefs.PAGE_KEYS[i]);
            pageChecks.add(c);
        }
        label(root, "Đổi trang sau (giây)");
        rotate = input(root, "20", InputType.TYPE_CLASS_NUMBER);
        label(root, "Bố cục màn hình ngang");
        layoutGroup = radios(root, 100, new String[]{"Tự động theo kích thước máy", "Luôn 2 cột", "3 cột (máy màn dài, ≥ 6 inch)"});
        label(root, "Cỡ chữ (%), mặc định 100");
        fontScale = input(root, "100", InputType.TYPE_CLASS_NUMBER);

        section(root, "5. Màn hình ngày / đêm");
        alwaysOn = check(root, "Luôn sáng 24/24 (bỏ qua chế độ đêm)");
        label(root, "Giờ ngày (màn luôn sáng): từ giờ … đến giờ …");
        LinearLayout hours = new LinearLayout(this);
        hours.setOrientation(LinearLayout.HORIZONTAL);
        dayStart = new EditText(this);
        dayStart.setInputType(InputType.TYPE_CLASS_NUMBER);
        dayStart.setHint("6");
        dayEnd = new EditText(this);
        dayEnd.setInputType(InputType.TYPE_CLASS_NUMBER);
        dayEnd.setHint("22");
        hours.addView(dayStart, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView to = new TextView(this);
        to.setText("  đến  ");
        hours.addView(to);
        hours.addView(dayEnd, new LinearLayout.LayoutParams(0, -2, 1f));
        root.addView(hours);
        label(root, "Ban đêm");
        nightGroup = radios(root, 200, new String[]{
                "Chỉ giảm độ sáng, màn vẫn sáng",
                "Tắt theo thời gian chờ của máy khi không chạm",
                "Tắt hẳn màn hình khi không chạm (cần quyền quản trị)"});
        label(root, "Tắt sau bao nhiêu phút không chạm / không vẫy tay");
        idle = input(root, "2", InputType.TYPE_CLASS_NUMBER);
        label(root, "Độ sáng ban đêm (%)");
        nightBright = input(root, "15", InputType.TYPE_CLASS_NUMBER);
        label(root, "Độ sáng ban ngày (%), 0 = theo cài đặt của máy");
        dayBright = input(root, "0", InputType.TYPE_CLASS_NUMBER);
        burnIn = check(root, "Chống lưu ảnh: dịch nội dung vài điểm ảnh mỗi 2 phút");
        adminButton = button(root, "", new Runnable() {
            @Override
            public void run() {
                toggleAdmin();
            }
        });
        adminState = note(root, "");
        note(root, "Chế độ \"Tắt theo thời gian chờ\" dùng thời gian chờ màn hình của máy (Cài đặt > Màn hình), nên đặt 15–30 giây. "
                + "Để bảng tự hiện lại khi bật màn, đặt khoá màn hình là \"Không\" hoặc \"Vuốt\" (không dùng mã PIN).");

        section(root, "6. Đánh thức và tự chạy");
        proximity = check(root, "Đưa tay qua cảm biến tiệm cận (cạnh loa thoại) để bật màn hình");
        note(root, sensorInfo());
        wakeMorning = check(root, "Tự bật màn hình lúc bắt đầu giờ ngày");
        autoStart = check(root, "Tự mở bảng lịch khi khởi động máy");
        if (Build.VERSION.SDK_INT >= 23) {
            button(root, "Cho app chạy nền (tắt tối ưu pin)", new Runnable() {
                @Override
                public void run() {
                    askIgnoreBattery();
                }
            });
            button(root, "Cho phép hiện trên ứng dụng khác (để tự bật lại)", new Runnable() {
                @Override
                public void run() {
                    askOverlay();
                }
            });
        }
        if (Build.VERSION.SDK_INT >= 33) {
            button(root, "Cho phép thông báo của dịch vụ đánh thức", new Runnable() {
                @Override
                public void run() {
                    askNotifications();
                }
            });
        }
        note(root, "Samsung/Xiaomi/Oppo: thêm Lịch Bảng vào danh sách được chạy nền / tự khởi động trong phần quản lý pin của máy.");

        section(root, "7. Cập nhật");
        versionText = note(root, "");
        autoUpdate = check(root, "Tự kiểm tra và tải bản mới từ GitHub");
        label(root, "Tự mở màn hình cài bản mới lúc mấy giờ (-1 = không tự mở)");
        installHour = input(root, "10", InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_SIGNED);
        button(root, "Kiểm tra cập nhật ngay", new Runnable() {
            @Override
            public void run() {
                save(false);
                updater.check();
            }
        });
        installButton = button(root, "Cài bản mới", new Runnable() {
            @Override
            public void run() {
                updater.install(SettingsActivity.this);
            }
        });
        updateState = note(root, "");
        if (Build.VERSION.SDK_INT < 26) {
            note(root, "Lần đầu cần bật: Cài đặt > Bảo mật > Nguồn không xác định. Mỗi lần cập nhật cần bấm \"Cài đặt\" một lần.");
        }

        Button saveBtn = button(root, "Lưu", new Runnable() {
            @Override
            public void run() {
                save(true);
                finish();
            }
        });
        ((LinearLayout.LayoutParams) saveBtn.getLayoutParams()).topMargin = dp(24);
        note(root, "Màn hình chính: giữ lâu vào đồng hồ hoặc bấm ··· để mở lại trang này.");

        ScrollView sv = new ScrollView(this);
        sv.addView(root);
        return sv;
    }

    private void section(LinearLayout root, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        t.setTextColor(0xFF5FD3B0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(24);
        root.addView(t, lp);
    }

    private TextView label(LinearLayout root, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(12);
        root.addView(t, lp);
        return t;
    }

    private TextView note(LinearLayout root, String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        t.setAlpha(0.75f);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(4);
        root.addView(t, lp);
        return t;
    }

    private EditText input(LinearLayout root, String hint, int type) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(type);
        if ((type & InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) e.setSingleLine(true);
        root.addView(e, new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    private CheckBox check(LinearLayout root, String s) {
        CheckBox c = new CheckBox(this);
        c.setText(s);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(6);
        root.addView(c, lp);
        return c;
    }

    private RadioGroup radios(LinearLayout root, int baseId, String[] labels) {
        RadioGroup g = new RadioGroup(this);
        g.setOrientation(RadioGroup.VERTICAL);
        for (int i = 0; i < labels.length; i++) {
            RadioButton b = new RadioButton(this);
            b.setId(baseId + i);
            b.setText(labels[i]);
            g.addView(b);
        }
        root.addView(g, new LinearLayout.LayoutParams(-1, -2));
        return g;
    }

    private Button button(LinearLayout root, String s, final Runnable action) {
        Button b = new Button(this);
        b.setText(s);
        b.setGravity(Gravity.CENTER);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(8);
        root.addView(b, lp);
        return b;
    }

    // ------------------------------------------------------------------ nạp / lưu

    private void load() {
        place.setText(prefs.placeName());
        placeState.setText(placeLabel());
        watchlist.setText(prefs.watchlistRaw());
        feeds.setText(prefs.feedsRaw());
        for (CheckBox c : pageChecks) c.setChecked(prefs.pageEnabled((String) c.getTag()));
        rotate.setText(String.valueOf(prefs.rotateSeconds()));
        String l = prefs.layout();
        layoutGroup.check(Prefs.LAYOUT_2.equals(l) ? 101 : Prefs.LAYOUT_3.equals(l) ? 102 : 100);
        fontScale.setText(String.valueOf(prefs.fontScale()));
        alwaysOn.setChecked(prefs.alwaysOn());
        dayStart.setText(String.valueOf(prefs.dayStart()));
        dayEnd.setText(String.valueOf(prefs.dayEnd()));
        String n = prefs.nightMode();
        nightGroup.check(Prefs.NIGHT_DIM.equals(n) ? 200 : Prefs.NIGHT_OFF.equals(n) ? 202 : 201);
        idle.setText(String.valueOf(prefs.idleMinutes()));
        nightBright.setText(String.valueOf(prefs.nightBrightness()));
        dayBright.setText(String.valueOf(prefs.dayBrightness()));
        burnIn.setChecked(prefs.burnInShift());
        proximity.setChecked(prefs.proximityWake());
        wakeMorning.setChecked(prefs.wakeMorning());
        autoStart.setChecked(prefs.autoStart());
        autoUpdate.setChecked(prefs.autoUpdate());
        installHour.setText(String.valueOf(prefs.updateInstallHour()));
    }

    private String placeLabel() {
        return String.format(java.util.Locale.US, "Đang dùng: %s (%.3f, %.3f)", prefs.placeName(), prefs.placeLat(), prefs.placeLon());
    }

    private static int intOr(EditText e, int def, int lo, int hi) {
        try {
            int v = Integer.parseInt(e.getText().toString().trim());
            return Math.max(lo, Math.min(hi, v));
        } catch (NumberFormatException ex) {
            return def;
        }
    }

    private void save(boolean announce) {
        prefs.setWatchlist(watchlist.getText().toString().trim().length() == 0
                ? "" : watchlist.getText().toString().trim());
        String f = feeds.getText().toString().trim();
        prefs.setFeeds(f);
        List<String> pagesOn = new ArrayList<String>();
        for (CheckBox c : pageChecks) if (c.isChecked()) pagesOn.add((String) c.getTag());
        if (pagesOn.isEmpty()) pagesOn.add("month");
        prefs.setPages(pagesOn);
        prefs.putInt("rotate_seconds", intOr(rotate, 20, 5, 600));
        int lid = layoutGroup.getCheckedRadioButtonId();
        prefs.putString("layout", lid == 101 ? Prefs.LAYOUT_2 : lid == 102 ? Prefs.LAYOUT_3 : Prefs.LAYOUT_AUTO);
        prefs.putInt("font_scale", intOr(fontScale, 100, 70, 160));
        prefs.putBool("always_on", alwaysOn.isChecked());
        prefs.putInt("day_start", intOr(dayStart, 6, 0, 23));
        prefs.putInt("day_end", intOr(dayEnd, 22, 0, 24));
        int nid = nightGroup.getCheckedRadioButtonId();
        prefs.putString("night_mode", nid == 200 ? Prefs.NIGHT_DIM : nid == 202 ? Prefs.NIGHT_OFF : Prefs.NIGHT_TIMEOUT);
        prefs.putInt("idle_minutes", intOr(idle, 2, 1, 240));
        prefs.putInt("night_brightness", intOr(nightBright, 15, 1, 100));
        prefs.putInt("day_brightness", intOr(dayBright, 0, 0, 100));
        prefs.putBool("burn_in_shift", burnIn.isChecked());
        prefs.putBool("proximity_wake", proximity.isChecked());
        prefs.putBool("wake_morning", wakeMorning.isChecked());
        prefs.putBool("auto_start", autoStart.isChecked());
        prefs.putBool("auto_update", autoUpdate.isChecked());
        prefs.putInt("update_install_hour", intOr(installHour, 10, -1, 23));

        if (!place.getText().toString().trim().equalsIgnoreCase(prefs.placeName())
                && place.getText().toString().trim().length() > 0) {
            geocode(false);
        }
        DataHub.get(this).settingsChanged();
        WakeService.sync(this);
        Scheduler.schedule(this);
        if (announce) Toast.makeText(this, "Đã lưu", Toast.LENGTH_SHORT).show();
    }

    private void geocode(final boolean announce) {
        final String name = place.getText().toString().trim();
        if (name.length() == 0 || geocoding) return;
        geocoding = true;
        placeState.setText("Đang tìm \"" + name + "\"…");
        final android.content.Context app = getApplicationContext();
        new Thread(new Runnable() {
            @Override
            public void run() {
                String msg;
                try {
                    Object[] r = WeatherSource.geocode(name);
                    new Prefs(app).setPlace((String) r[0], (Double) r[1], (Double) r[2]);
                    DataHub.get(app).settingsChanged();
                    msg = null;
                } catch (Exception e) {
                    msg = e.getMessage() != null && e.getMessage().startsWith("Không tìm thấy")
                            ? e.getMessage() : "Lỗi: " + Http.describe(e);
                }
                final String m = msg;
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        geocoding = false;
                        if (isFinishing()) {
                            if (m != null) Toast.makeText(app, m, Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (m == null) {
                            place.setText(prefs.placeName());
                            placeState.setText(placeLabel());
                            if (announce) Toast.makeText(SettingsActivity.this, "Đã chọn " + prefs.placeName(), Toast.LENGTH_SHORT).show();
                        } else {
                            placeState.setText(m);
                        }
                    }
                });
            }
        }).start();
    }

    // ------------------------------------------------------------------ trạng thái, quyền

    private void refreshStates() {
        boolean admin = AdminReceiver.isActive(this);
        adminButton.setText(admin ? "Gỡ quyền tắt màn hình (quản trị thiết bị)" : "Cấp quyền tắt màn hình (quản trị thiết bị)");
        adminState.setText(admin ? "Đã cấp quyền: có thể tắt hẳn màn hình ban đêm. Nhớ gỡ quyền trước khi gỡ app."
                : "Chưa cấp quyền: chế độ \"Tắt hẳn\" sẽ dùng thời gian chờ của máy.");
        versionText.setText("Phiên bản " + BuildConfig.VERSION_NAME + " (mã " + BuildConfig.VERSION_CODE + ") · Android "
                + Build.VERSION.RELEASE + " · TLS: " + Tls.engine());
        String st = updater.message();
        if (updater.ready()) {
            installButton.setVisibility(View.VISIBLE);
            installButton.setText("Cài bản " + updater.readyName());
            String notes = updater.readyNotes();
            if (st.length() == 0) st = "Bản " + updater.readyName() + " đã tải xong.";
            if (notes.length() > 0) st += "\n" + notes;
        } else {
            installButton.setVisibility(View.GONE);
        }
        if (!updater.supported()) st = "Bản debug (vn.lcta.debug) không tự cập nhật.";
        updateState.setText(st);
    }

    private void toggleAdmin() {
        if (AdminReceiver.isActive(this)) {
            AdminReceiver.remove(this);
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    refreshStates();
                }
            }, 500);
        } else {
            try {
                startActivityForResult(AdminReceiver.requestIntent(this), REQ_ADMIN);
            } catch (Throwable t) {
                Toast.makeText(this, "Máy không hỗ trợ quyền quản trị", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        refreshStates();
    }

    private String sensorInfo() {
        Sensor s = WakeService.proximitySensor(this);
        if (s == null) return "Máy không có cảm biến tiệm cận – chạm màn hình hoặc bấm nút nguồn để bật.";
        return "Cảm biến: " + s.getName() + " (tối đa " + Math.round(s.getMaximumRange()) + " cm). "
                + "Một số máy đời mới chỉ bật cảm biến khi đang gọi nên có thể không đánh thức được khi màn đã tắt.";
    }

    @TargetApi(23)
    private void askIgnoreBattery() {
        PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
        if (pm != null && pm.isIgnoringBatteryOptimizations(getPackageName())) {
            Toast.makeText(this, "App đã được phép chạy nền.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    Uri.parse("package:" + getPackageName())));
        } catch (Throwable t) {
            try {
                startActivity(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
            } catch (Throwable ignored) {
            }
        }
    }

    @TargetApi(23)
    private void askOverlay() {
        if (Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Đã được phép.", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        } catch (Throwable ignored) {
        }
    }

    @TargetApi(33)
    private void askNotifications() {
        requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9);
    }
}
