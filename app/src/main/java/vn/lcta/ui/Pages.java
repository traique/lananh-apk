package vn.lcta.ui;

import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Calendar;
import java.util.List;

import vn.lcta.Fmt;
import vn.lcta.Theme;
import vn.lcta.data.DataHub;
import vn.lcta.data.StockSource;
import vn.lcta.data.WeatherCodes;
import vn.lcta.lunar.Lunar;
import vn.lcta.lunar.LunarDate;

/** Dựng nội dung từng trang bên phải. Mỗi trang tự cắt số dòng cho vừa khung. */
final class Pages {
    static final float BODY = 15f;
    static final float SMALL = 12f;

    private final Ui ui;
    private final DataHub hub;
    /** Bề rộng khung trang tính theo dp đã nhân hệ số (để chọn bản gọn). */
    private final float widthU;

    Pages(Ui ui, DataHub hub, float widthPx) {
        this.ui = ui;
        this.hub = hub;
        this.widthU = widthPx / (ui.density * ui.u);
    }

    /** Khung hẹp (màn 3,5"–5", hoặc cột phải của bố cục 3 cột). */
    private boolean narrow() {
        return widthU < 330;
    }

    /** Rất hẹp: bỏ thêm cột phụ. */
    private boolean tiny() {
        return widthU < 240;
    }

    // ------------------------------------------------------------ chung

    private View message(String s, int color) {
        TextView t = ui.multi(s, BODY, color, false, 4);
        t.setGravity(Gravity.CENTER);
        LinearLayout box = ui.col();
        box.setGravity(Gravity.CENTER);
        box.addView(t, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        return box;
    }

    /** Khi chưa có dữ liệu: đang tải / lỗi / trống. */
    private View empty(String key) {
        DataHub.Entry e = hub.entry(key);
        if (e != null && e.error != null && !e.loading) return message("Lỗi: " + e.error + "\nĐang thử lại…", Theme.WARN);
        return message("Đang tải dữ liệu…", Theme.DIM);
    }

    /** Dòng chú thích cuối trang: nguồn, giờ cập nhật, cảnh báo dữ liệu cũ. */
    private TextView note(String key, String text) {
        DataHub.Entry e = hub.entry(key);
        StringBuilder sb = new StringBuilder(text);
        int color = Theme.FAINT;
        if (e != null && e.error != null) {
            sb.append(" · lỗi: ").append(e.error);
            color = Theme.WARN;
        } else if (hub.isStale(key)) {
            sb.append(" · dữ liệu cũ (").append(Fmt.ago(e.at, System.currentTimeMillis())).append(")");
            color = Theme.WARN;
        }
        return ui.text(sb.toString(), SMALL, color, false);
    }

    private LinearLayout header(String... cols) {
        LinearLayout r = ui.row();
        for (int i = 0; i < cols.length; i += 2) {
            ui.cell(r, cols[i], Float.parseFloat(cols[i + 1]), i > 0, SMALL, Theme.DIM, false);
        }
        return r;
    }

    private FitColumn column() {
        return new FitColumn(ui.ctx);
    }

    // ------------------------------------------------------------ âm lịch chi tiết

    View lunar() {
        FitColumn col = column();
        Calendar now = Calendar.getInstance();
        int d = now.get(Calendar.DAY_OF_MONTH);
        int m = now.get(Calendar.MONTH) + 1;
        int y = now.get(Calendar.YEAR);
        LunarDate ld = Lunar.solarToLunar(d, m, y);
        int jd = ld.jd;

        LinearLayout top = ui.row();
        TextView big = ui.text(Integer.toString(ld.day), 44, Theme.ACCENT, true);
        top.addView(big);
        LinearLayout right = ui.col();
        right.setPadding(ui.px(10), 0, 0, 0);
        right.addView(ui.text("Tháng " + Lunar.MONTH_NAME[ld.month] + (ld.leap ? " (nhuận)" : "")
                + (Lunar.lunarMonthLength(ld) == 30 ? " · đủ" : " · thiếu"), BODY + 1, Theme.TEXT, true));
        right.addView(ui.text("Năm " + Lunar.yearCanChi(ld.year), BODY, Theme.DIM, false));
        top.addView(right, Ui.weight(1));
        col.add(top, 0);

        col.add(ui.text("Ngày " + Lunar.dayCanChi(jd) + " · tháng " + Lunar.monthCanChi(ld.month, ld.year),
                BODY, Theme.TEXT, false), 1, ui.px(8));

        int star = Lunar.dayStar(jd, ld.month);
        boolean good = Lunar.isHoangDao(jd, ld.month);
        col.add(ui.text((good ? "Ngày hoàng đạo" : "Ngày hắc đạo") + " · sao " + Lunar.STARS[star],
                BODY, good ? Theme.GOOD_DAY : Theme.HOLIDAY, false), 2, ui.px(6));

        String term = "Tiết " + Lunar.solarTerm(jd) + (Lunar.solarTermStarts(jd) ? " (bắt đầu hôm nay)" : "");
        col.add(ui.text(term, BODY, Theme.TEXT, false), 4, ui.px(6));

        List<String> gio = Lunar.gioHoangDao(jd);
        StringBuilder g = new StringBuilder("Giờ tốt: ");
        for (int i = 0; i < gio.size(); i++) {
            if (i > 0) g.append(", ");
            g.append(gio.get(i));
        }
        col.add(ui.multi(g.toString(), SMALL + 1, Theme.DIM, false, 2), 3, ui.px(6));

        List<Lunar.Upcoming> up = Lunar.upcoming(d, m, y, 4, false);
        int tet = Lunar.daysToTet(d, m, y);
        if (tet > 0) {
            col.add(ui.text("Còn " + tet + " ngày đến Tết " + Lunar.yearCanChi(ld.year + 1), BODY, Theme.GOLD, true), 2, ui.px(8));
        }
        int p = 5;
        for (Lunar.Upcoming u : up) {
            if (u.holiday.name.startsWith("Tết Nguyên")) continue;
            String when = u.days == 1 ? "Ngày mai" : "Còn " + u.days + " ngày";
            col.add(ui.text(when + " · " + u.holiday.name + " (" + Fmt.two(u.solar[0]) + "/" + Fmt.two(u.solar[1]) + ")",
                    SMALL + 1, Theme.DIM, false), p++, ui.px(4));
        }
        return col;
    }

    // ------------------------------------------------------------ thời tiết

    View weather() {
        JSONObject w = hub.data("weather");
        if (w == null) return empty("weather");
        FitColumn col = column();
        JSONArray hours = w.optJSONArray("hours");
        if (hours != null && hours.length() > 1) {
            LinearLayout strip = ui.row();
            int count = Math.min(Math.max(4, Math.min(8, (int) (widthU / 44))), hours.length() - 1);
            for (int i = 1; i <= count; i++) {
                JSONObject h = hours.optJSONObject(i);
                LinearLayout c = ui.col();
                c.setGravity(Gravity.CENTER_HORIZONTAL);
                TextView hh = ui.text(h.optString("h") + "h", SMALL, Theme.DIM, false);
                hh.setGravity(Gravity.CENTER);
                c.addView(hh);
                WeatherIcon ic = new WeatherIcon(ui.ctx, ui.px(26));
                ic.set(h.optInt("code"), h.optInt("day", 1) == 1, Theme.PANEL);
                c.addView(ic);
                TextView t = ui.text(Fmt.temp(h.optDouble("t")), SMALL + 1, Theme.TEXT, true);
                t.setGravity(Gravity.CENTER);
                c.addView(t);
                int rain = h.optInt("rain");
                TextView r = ui.text(rain >= 20 ? rain + "%" : " ", SMALL - 1, 0xFF7FC4FF, false);
                r.setGravity(Gravity.CENTER);
                c.addView(r);
                strip.addView(c, Ui.weight(1));
            }
            col.add(strip, 1);
        }
        JSONArray days = w.optJSONArray("days");
        if (days != null) {
            for (int i = 0; i < days.length(); i++) {
                JSONObject d = days.optJSONObject(i);
                LinearLayout r = ui.row();
                r.setPadding(0, ui.px(5), 0, 0);
                String date = d.optString("date");
                boolean nar = narrow();
                String label = i == 0 ? (nar ? "Nay" : "Hôm nay") : dayLabel(date, !nar);
                ui.cell(r, label, nar ? 0.8f : 1.3f, false, BODY, i == 0 ? Theme.ACCENT : Theme.TEXT, i == 0);
                WeatherIcon ic = new WeatherIcon(ui.ctx, ui.px(22));
                ic.set(d.optInt("code"), true, Theme.PANEL);
                r.addView(ic);
                if (!nar) {
                    TextView desc = ui.text(" " + WeatherCodes.text(d.optInt("code")), SMALL + 1, Theme.DIM, false);
                    r.addView(desc, Ui.weight(2.0f));
                }
                ui.cell(r, Fmt.range(d.optDouble("min"), d.optDouble("max")), 1.3f, true, BODY, Theme.TEXT, true);
                int rain = d.optInt("rain");
                ui.cell(r, rain > 0 ? (nar ? rain + "%" : "mưa " + rain + "%") : "", nar ? 0.8f : 1.0f, true, SMALL, 0xFF7FC4FF, false);
                col.add(r, 2 + i);
            }
            if (days.length() > 0) {
                JSONObject today = days.optJSONObject(0);
                String sun = today.optString("sunrise");
                if (sun.length() > 0) {
                    col.add(ui.text("Mặt trời mọc " + sun + " · lặn " + today.optString("sunset"), SMALL, Theme.DIM, false), 20, ui.px(6));
                }
            }
        }
        col.addFill(ui.spacer());
        col.add(note("weather", w.optString("place") + " · Open-Meteo"), 0);
        return col;
    }

    private static String dayLabel(String iso, boolean withDate) {
        try {
            int y = Integer.parseInt(iso.substring(0, 4));
            int m = Integer.parseInt(iso.substring(5, 7));
            int d = Integer.parseInt(iso.substring(8, 10));
            Calendar c = Calendar.getInstance();
            c.clear();
            c.set(y, m - 1, d);
            String wd = Fmt.WEEKDAYS_SHORT[c.get(Calendar.DAY_OF_WEEK)];
            return withDate ? wd + " " + Fmt.two(d) + "/" + Fmt.two(m) : wd;
        } catch (Exception e) {
            return iso;
        }
    }

    // ------------------------------------------------------------ vàng

    View gold() {
        JSONObject g = hub.data("gold");
        if (g == null) return empty("gold");
        FitColumn col = column();
        boolean nar = narrow();
        boolean tiny = tiny();
        if (tiny) col.add(header("Loại", "1.5", "Bán", "1", "±", "0.8"), 0);
        else col.add(header("Loại", nar ? "1.3" : "1.7", "Mua", "1", "Bán", "1", "±", "0.8"), 0);
        JSONArray rows = g.optJSONArray("rows");
        JSONObject world = null;
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            if (r.optBoolean("usd")) {
                world = r;
                continue;
            }
            LinearLayout row = ui.row();
            row.setPadding(0, ui.px(5), 0, 0);
            ui.cell(row, nar ? r.optString("short", r.optString("label")) : r.optString("label"),
                    tiny ? 1.5f : (nar ? 1.3f : 1.7f), false, BODY, Theme.TEXT, false);
            if (!tiny) ui.cell(row, Fmt.million(r.optDouble("buy")), 1f, true, BODY, Theme.TEXT, false);
            ui.cell(row, Fmt.million(r.optDouble("sell")), 1f, true, BODY, Theme.GOLD, true);
            double ch = r.optDouble("changeSell", 0);
            ui.cell(row, Fmt.signed(ch / 1_000_000d, 1), 0.8f, true, SMALL + 1, Fmt.changeColor(ch), false);
            col.add(row, 1 + i);
        }
        if (world != null) {
            LinearLayout row = ui.row();
            row.setPadding(0, ui.px(8), 0, 0);
            ui.cell(row, "Thế giới", tiny ? 1.5f : (nar ? 1.3f : 1.7f), false, BODY, Theme.TEXT, false);
            ui.cell(row, Fmt.dec(world.optDouble("buy"), 1) + (tiny ? "$" : " $/oz"), tiny ? 1f : 2f, true, BODY, Theme.GOLD, true);
            double ch = world.optDouble("changeBuy", 0);
            ui.cell(row, Fmt.signed(ch, 1), 0.8f, true, SMALL + 1, Fmt.changeColor(ch), false);
            col.add(row, 2);
        }
        col.addFill(ui.spacer());
        col.add(note("gold", (nar ? "triệu/lượng · " : "triệu đồng/lượng · ") + g.optString("updated")
                + (nar ? "" : " · vang.today")), 0);
        return col;
    }

    // ------------------------------------------------------------ xăng dầu

    View fuel() {
        JSONObject f = hub.data("fuel");
        if (f == null) return empty("fuel");
        FitColumn col = column();
        boolean nar = narrow();
        if (nar) col.add(header("Mặt hàng", "2.0", "Vùng 1", "1.1", "±", "0.75"), 0);
        else col.add(header("Mặt hàng", "2.1", "Vùng 1", "1", "Vùng 2", "1", "±", "0.8"), 0);
        JSONArray rows = f.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            LinearLayout row = ui.row();
            row.setPadding(0, ui.px(5), 0, 0);
            String name = r.optString("name");
            ui.cell(row, nar ? shortFuel(name) : name, nar ? 2.0f : 2.1f, false, BODY, Theme.TEXT, false);
            ui.cell(row, Fmt.group(Math.round(r.optDouble("zone1"))), nar ? 1.1f : 1f, true, BODY, Theme.GOLD, true);
            if (!nar) {
                double z2 = r.optDouble("zone2");
                ui.cell(row, Double.isNaN(z2) ? "–" : Fmt.group(Math.round(z2)), 1f, true, BODY, Theme.TEXT, false);
            }
            double ch = r.optDouble("change", 0);
            ui.cell(row, ch == 0 ? "0" : Fmt.signed(ch, 0), nar ? 0.75f : 0.8f, true, SMALL + 1, Fmt.changeColor(ch), false);
            col.add(row, 1 + i);
        }
        col.addFill(ui.spacer());
        String upd = f.optString("updated");
        if (nar && upd.length() == 10) upd = upd.substring(0, 5);
        col.add(note("fuel", "đồng/lít · Petrolimex" + (upd.length() > 0 ? " · " + upd : "")), 0);
        return col;
    }

    /** "Xăng E10 RON 95 Mức 5" -> "E10 RON95 M5". */
    static String shortFuel(String n) {
        String s = n.replace("Xăng ", "").replace(" Mức ", " M").replace("RON 9", "RON9");
        if (s.startsWith("Dầu DO")) s = s.substring(4);
        return s.trim();
    }

    // ------------------------------------------------------------ tỷ giá

    View fx() {
        JSONObject f = hub.data("fx");
        if (f == null) return empty("fx");
        FitColumn col = column();
        boolean tiny = tiny();
        if (tiny) col.add(header("Ngoại tệ", "1", "Bán", "1.3"), 0);
        else col.add(header("Ngoại tệ", "1", "Mua CK", "1.3", "Bán", "1.3"), 0);
        JSONArray rows = f.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            LinearLayout row = ui.row();
            row.setPadding(0, ui.px(5), 0, 0);
            ui.cell(row, r.optString("code"), 1f, false, BODY, Theme.TEXT, true);
            if (!tiny) ui.cell(row, money(r.optDouble("transfer")), 1.3f, true, BODY, Theme.TEXT, false);
            ui.cell(row, money(r.optDouble("sell")), 1.3f, true, BODY, Theme.GOLD, true);
            col.add(row, 1 + i);
        }
        col.addFill(ui.spacer());
        col.add(note("fx", (narrow() ? "VCB · " : "đồng · Vietcombank · ") + f.optString("updated")), 0);
        return col;
    }

    private static String money(double v) {
        if (Double.isNaN(v)) return "–";
        return v >= 1000 ? Fmt.dec(v, 0) : Fmt.dec(v, 2);
    }

    // ------------------------------------------------------------ chứng khoán

    View stocks() {
        JSONObject s = hub.data("stocks");
        if (s == null) {
            if (hub.source("stocks") != null && new vn.lcta.Prefs(ui.ctx).watchlist().isEmpty()) {
                return message("Chưa có mã theo dõi.\nThêm trong Cài đặt.", Theme.DIM);
            }
            return empty("stocks");
        }
        FitColumn col = column();
        boolean nar = narrow();
        if (nar) col.add(header("Mã", "1.3", "Giá", "1.3", "%", "1"), 0);
        else col.add(header("Mã", "1.1", "Giá", "1.3", "+/-", "1.1", "%", "1"), 0);
        JSONArray rows = s.optJSONArray("rows");
        for (int i = 0; rows != null && i < rows.length(); i++) {
            JSONObject r = rows.optJSONObject(i);
            LinearLayout row = ui.row();
            row.setPadding(0, ui.px(5), 0, 0);
            ui.cell(row, r.optString("symbol"), nar ? 1.3f : 1.1f, false, BODY, Theme.TEXT, true);
            if (r.has("error")) {
                ui.cell(row, r.optString("error"), 3.4f, true, SMALL, Theme.WARN, false);
            } else {
                boolean idx = r.optBoolean("index");
                double ch = r.optDouble("change", 0);
                int color = Fmt.changeColor(ch);
                ui.cell(row, Fmt.dec(r.optDouble("price"), 2), 1.3f, true, BODY, color, true);
                if (!nar) ui.cell(row, Fmt.signed(ch, 2), 1.1f, true, BODY, color, false);
                ui.cell(row, Fmt.signed(r.optDouble("pct", 0), 2) + "%", 1f, true, SMALL + 1, color, false);
                if (idx) row.setPadding(0, ui.px(5), 0, ui.px(1));
            }
            col.add(row, 1 + i);
        }
        col.addFill(ui.spacer());
        boolean open = StockSource.marketOpen();
        col.add(note("stocks", narrow() ? (open ? "Trong phiên" : "Phiên gần nhất") + " · nghìn đ"
                : (open ? "Đang trong phiên" : "Ngoài giờ giao dịch · phiên gần nhất") + " · giá nghìn đồng"), 0);
        return col;
    }

    // ------------------------------------------------------------ tin tức

    /** @param offset vị trí bắt đầu (xoay vòng các tin). */
    FitColumn news(int offset) {
        JSONObject n = hub.data("news");
        FitColumn col = column();
        if (n == null) {
            col.addFill(empty("news"));
            return col;
        }
        JSONArray items = n.optJSONArray("items");
        int count = items == null ? 0 : items.length();
        if (count == 0) {
            col.addFill(message("Chưa có tin.", Theme.DIM));
            return col;
        }
        long now = System.currentTimeMillis();
        for (int k = 0; k < count; k++) {
            JSONObject it = items.optJSONObject((offset + k) % count);
            LinearLayout box = ui.col();
            box.setPadding(0, ui.px(k == 0 ? 0 : 6), 0, 0);
            box.addView(ui.multi(it.optString("title"), BODY, Theme.TEXT, false, 2));
            box.addView(ui.text(it.optString("source") + " · " + Fmt.ago(it.optLong("time"), now), SMALL, Theme.DIM, false));
            col.add(box, 1 + k);
        }
        col.addFill(ui.spacer());
        int failed = n.optInt("failed");
        col.add(note("news", failed > 0 ? failed + " nguồn RSS lỗi" : "RSS"), 0);
        return col;
    }
}
