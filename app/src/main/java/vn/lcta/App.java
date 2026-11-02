package vn.lcta;

import android.app.Application;

import vn.lcta.net.Tls;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Theme.loadFonts(this);
        Tls.install(this);
    }
}
