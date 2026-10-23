package io.github.rd227.facescan;

import android.webkit.CookieManager;
import java.util.concurrent.atomic.AtomicLong;

/** Invalidates in-flight cookie writes when the user signs out. */
final class WebSessionCookies implements OfficialHttp.Cookies {
    private final CookieManager manager;
    private final AtomicLong epoch;
    private final long generation;

    WebSessionCookies(CookieManager manager, AtomicLong epoch) {
        this.manager = manager;
        this.epoch = epoch;
        generation = epoch.get();
    }

    @Override public String get(String url) {
        return epoch.get() == generation ? manager.getCookie(url) : "";
    }

    @Override public void set(String url, String value) {
        synchronized (epoch) {
            if (epoch.get() == generation) manager.setCookie(url, value);
        }
    }

    @Override public void flush() { manager.flush(); }
}
