package io.github.rd227.facescan;

import android.app.Application;
import java.io.File;

public final class FaceScanApplication extends Application {
    @Override public void onCreate() {
        super.onCreate();
        new FacePhotoStore(this).clearTemporaryFiles();
        File[] captures = new File(getCacheDir(), "camera-capture").listFiles();
        if (captures != null) for (File file : captures) if (!file.delete()) file.deleteOnExit();
    }
}
