package vn.lcta.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import java.util.Calendar;

import vn.lcta.Theme;
import vn.lcta.lunar.Lunar;
import vn.lcta.lunar.LunarDate;

/**
 * Lịch tháng tự vẽ: ngày dương lớn, ngày âm nhỏ, ngày lễ tô đỏ,
 * chấm xanh = ngày hoàng đạo. Tuần bắt đầu từ Thứ Hai.
 */
public class MonthView extends View {
    private static final String[] HEAD = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};

    private final Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint solar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint lunar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();

    private int year;
    private int month; // 1..12
    private int todayD, todayM, todayY;
    private int firstOffset;
    private int daysInMonth;
    private LunarDate[] lunars = new LunarDate[0];
    private boolean[] holiday = new boolean[0];
    private boolean[] good = new boolean[0];
    private float scale = 1f;

    public MonthView(Context context) {
        super(context);
        head.setTypeface(Theme.bold());
        solar.setTypeface(Theme.bold());
        lunar.setTypeface(Theme.regular());
        head.setTextAlign(Paint.Align.CENTER);
        solar.setTextAlign(Paint.Align.CENTER);
        lunar.setTextAlign(Paint.Align.RIGHT);
    }

    /** Hệ số cỡ chữ theo cài đặt người dùng. */
    public void setScale(float s) {
        scale = s;
        invalidate();
    }

    public void setMonth(int year, int month) {
        this.year = year;
        this.month = month;
        Calendar today = Calendar.getInstance();
        todayD = today.get(Calendar.DAY_OF_MONTH);
        todayM = today.get(Calendar.MONTH) + 1;
        todayY = today.get(Calendar.YEAR);
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(year, month - 1, 1);
        firstOffset = (c.get(Calendar.DAY_OF_WEEK) + 5) % 7;
        daysInMonth = c.getActualMaximum(Calendar.DAY_OF_MONTH);
        lunars = new LunarDate[daysInMonth + 1];
        holiday = new boolean[daysInMonth + 1];
        good = new boolean[daysInMonth + 1];
        for (int d = 1; d <= daysInMonth; d++) {
            LunarDate ld = Lunar.solarToLunar(d, month, year);
            lunars[d] = ld;
            holiday[d] = Lunar.holidayOn(d, month, ld) != null;
            good[d] = Lunar.isHoangDao(ld.jd, ld.month);
        }
        invalidate();
    }

    public int year() {
        return year;
    }

    public int month() {
        return month;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (daysInMonth == 0) return;
        float w = getWidth();
        float h = getHeight();
        int rows = (firstOffset + daysInMonth + 6) / 7;
        float cw = w / 7f;
        float headH = Math.min(h * 0.12f, cw * 0.55f);
        float ch = (h - headH) / rows;

        float headSize = Math.min(headH * 0.62f, cw * 0.36f);
        head.setTextSize(headSize);
        for (int i = 0; i < 7; i++) {
            head.setColor(i == 6 ? Theme.SUNDAY : (i == 5 ? Theme.SATURDAY : Theme.DIM));
            canvas.drawText(HEAD[i], (i + 0.5f) * cw, headH * 0.72f, head);
        }

        float solarSize = Math.min(ch * 0.50f, cw * 0.44f) * Math.min(1.15f, scale);
        float lunarSize = Math.max(solarSize * 0.46f, Math.min(ch, cw) * 0.22f);
        solar.setTextSize(solarSize);
        lunar.setTextSize(lunarSize);
        float pad = Math.min(cw, ch) * 0.06f;
        float radius = Math.min(cw, ch) * 0.14f;

        for (int d = 1; d <= daysInMonth; d++) {
            int idx = firstOffset + d - 1;
            int col = idx % 7;
            int row = idx / 7;
            float x = col * cw;
            float y = headH + row * ch;
            boolean isToday = d == todayD && month == todayM && year == todayY;
            if (isToday) {
                fill.setColor(Theme.TODAY_BG);
                rect.set(x + pad, y + pad, x + cw - pad, y + ch - pad);
                canvas.drawRoundRect(rect, radius, radius, fill);
            }
            int color;
            if (holiday[d]) color = Theme.HOLIDAY;
            else if (col == 6) color = Theme.SUNDAY;
            else if (col == 5) color = Theme.SATURDAY;
            else color = Theme.TEXT;
            if (isToday) color = Theme.TEXT;
            solar.setColor(color);
            float sy = y + ch * 0.52f;
            canvas.drawText(Integer.toString(d), x + cw / 2f, sy, solar);

            LunarDate ld = lunars[d];
            String lt = ld.day == 1 || d == 1 ? ld.day + "/" + ld.month + (ld.leap ? "n" : "") : Integer.toString(ld.day);
            lunar.setColor(ld.day == 1 || ld.day == 15 ? Theme.ACCENT : Theme.DIM);
            if (isToday) lunar.setColor(0xFFCFF5E8);
            canvas.drawText(lt, x + cw - pad * 2f, y + ch - pad * 2.2f, lunar);

            if (good[d]) {
                fill.setColor(Theme.GOOD_DAY);
                float r = Math.max(2f, Math.min(cw, ch) * 0.055f);
                canvas.drawCircle(x + pad * 2.2f + r, y + pad * 2.2f + r, r, fill);
            }
        }
    }
}
