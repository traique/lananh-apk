package vn.lcta;

import android.content.Context;
import android.graphics.Typeface;

/** Bảng màu và font dùng chung (nền tối, hợp màn hình treo tường). */
public final class Theme {
    public static final int BG = 0xFF000000;
    public static final int PANEL = 0xFF0E1418;
    public static final int PANEL_LINE = 0xFF1E2A30;
    public static final int TEXT = 0xFFF2F5F4;
    public static final int DIM = 0xFF8FA39C;
    public static final int FAINT = 0xFF56666F;
    public static final int ACCENT = 0xFF5FD3B0;
    public static final int GOLD = 0xFFF2C14E;
    public static final int HOLIDAY = 0xFFFF7A6B;
    public static final int SUNDAY = 0xFFFF8A80;
    public static final int SATURDAY = 0xFF8AB4FF;
    public static final int GOOD_DAY = 0xFF4CD08A;
    public static final int UP = 0xFF3DD68C;
    public static final int DOWN = 0xFFFF5C5C;
    public static final int FLAT = 0xFFF2C14E;
    public static final int WARN = 0xFFFFB84D;
    public static final int TODAY_BG = 0xFF1F6F5A;

    private static Typeface regular;
    private static Typeface bold;

    private Theme() {}

    public static synchronized void loadFonts(Context c) {
        if (regular != null) return;
        try {
            regular = Typeface.createFromAsset(c.getAssets(), "fonts/inter_regular.ttf");
            bold = Typeface.createFromAsset(c.getAssets(), "fonts/inter_semibold.ttf");
        } catch (Throwable t) {
            regular = Typeface.DEFAULT;
            bold = Typeface.DEFAULT_BOLD;
        }
    }

    public static Typeface regular() {
        return regular != null ? regular : Typeface.DEFAULT;
    }

    public static Typeface bold() {
        return bold != null ? bold : Typeface.DEFAULT_BOLD;
    }
}
