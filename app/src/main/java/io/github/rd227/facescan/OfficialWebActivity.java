package io.github.rd227.facescan;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.SslErrorHandler;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.FileProvider;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Official pages run in a separate WebView with no JavaScript-to-native bridge. */
public final class OfficialWebActivity extends Activity {
    private static final int CHOOSE_FILE = 2001;
    private static final int CAPTURE = 2002;
    private static final int CAMERA_PERMISSION = 2003;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private WebView web;
    private TextView status;
    private FacePhotoStore photos;
    private ValueCallback<Uri[]> fileCallback;
    private Runnable pendingCamera;
    private Runnable pendingCameraDenied;
    private PermissionRequest browserPermission;
    private Uri cameraUri;
    private File cameraFile;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        URI target;
        try { target = OfficialUrl.parse(getIntent().getStringExtra("url")); }
        catch (IllegalArgumentException exception) { finish(); return; }
        photos = new FacePhotoStore(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Ui.BACKGROUND);
        setContentView(root);
        Ui.edgeInsets(this, root, 0, 0);
        LinearLayout toolbar = new LinearLayout(this);
        toolbar.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), Ui.dp(this, 8));
        toolbar.setOrientation(LinearLayout.HORIZONTAL);
        status = Ui.text(this, "超星官方页面", 13, Ui.INK, true);
        toolbar.addView(status, new LinearLayout.LayoutParams(0, -2, 1));
        Button done = Ui.button(this, "完成", false);
        toolbar.addView(done, new LinearLayout.LayoutParams(Ui.dp(this, 76), Ui.dp(this, 46)));
        done.setOnClickListener(view -> finish());
        root.addView(toolbar);

        web = new WebView(this);
        root.addView(web, new LinearLayout.LayoutParams(-1, 0, 1));
        WebSettings settings = web.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(true);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setSupportMultipleWindows(false);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, false);
        web.setWebViewClient(new WebViewClient() {
            @Override public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (!OfficialUrl.isAllowed(url)) {
                    view.stopLoading();
                    status.setText("此页面暂不支持在 App 内打开");
                }
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String raw = request.getUrl().toString();
                try {
                    String safe = OfficialUrl.parse(raw).toASCIIString();
                    if (!safe.equals(raw)) { view.loadUrl(safe); return true; }
                    return false;
                } catch (IllegalArgumentException exception) {
                    toast("App 内仅打开超星官方网页；此跳转暂不支持。");
                    return true;
                }
            }
            @Override public void onPageFinished(WebView view, String url) {
                if (OfficialUrl.isAllowed(url)) status.setText(Uri.parse(url).getHost());
                CookieManager.getInstance().flush();
            }
            @Override public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) status.setText("页面未加载，请检查网络后重试");
            }
            @Override public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.cancel();
                status.setText("证书验证失败，已停止加载");
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                                       FileChooserParams params) {
                finishFile(null);
                if (!OfficialUrl.isAllowed(view.getUrl())) { callback.onReceiveValue(null); return true; }
                fileCallback = callback;
                boolean image = params.getAcceptTypes().length == 0;
                for (String type : params.getAcceptTypes()) {
                    if (type.isEmpty() || type.startsWith("image/")) image = true;
                }
                if (!image) { launchPicker(params); return true; }
                if (params.isCaptureEnabled()) { capturePhoto(); return true; }
                List<String> labels = new ArrayList<>();
                List<Runnable> actions = new ArrayList<>();
                if (photos.exists()) {
                    labels.add("使用本机保存的本人照片");
                    actions.add(() -> worker.execute(() -> {
                        try {
                            Uri photo = photos.exportForUpload();
                            runOnUiThread(() -> {
                                if (!isDestroyed() && OfficialUrl.isAllowed(web.getUrl())) finishFile(new Uri[]{photo});
                                else finishFile(null);
                            });
                        } catch (Exception exception) {
                            runOnUiThread(() -> { finishFile(null); if (!isDestroyed()) toast("无法读取本机照片，请重新选择。"); });
                        }
                    }));
                }
                labels.add("拍摄照片"); actions.add(() -> capturePhoto());
                labels.add("从相册选择"); actions.add(() -> launchPicker(params));
                new AlertDialog.Builder(OfficialWebActivity.this).setTitle("选择上传图片")
                        .setItems(labels.toArray(new String[0]), (dialog, index) -> actions.get(index).run())
                        .setOnCancelListener(dialog -> finishFile(null)).show();
                return true;
            }
            @Override public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    boolean video = false;
                    for (String resource : request.getResources()) {
                        if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(resource)) video = true;
                    }
                    if (!video || !OfficialUrl.isAllowed(request.getOrigin().toString())
                            || !OfficialUrl.isAllowed(web.getUrl())) { request.deny(); return; }
                    if (browserPermission != null) { request.deny(); return; }
                    browserPermission = request;
                    withCamera(() -> {
                        if (browserPermission == request) {
                            browserPermission = null;
                            request.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                        }
                    }, () -> {
                        if (browserPermission == request) { browserPermission = null; request.deny(); }
                    });
                });
            }
            @Override public void onPermissionRequestCanceled(PermissionRequest request) {
                if (browserPermission == request) {
                    browserPermission = null;
                    pendingCamera = null;
                    pendingCameraDenied = null;
                }
            }
        });
        if (state == null || web.restoreState(state) == null) web.loadUrl(target.toASCIIString());
    }

    private void launchPicker(WebChromeClient.FileChooserParams params) {
        try { startActivityForResult(params.createIntent(), CHOOSE_FILE); }
        catch (Exception exception) { toast("没有可用的文件选择器。"); finishFile(null); }
    }

    private void capturePhoto() {
        withCamera(() -> {
            try {
                File folder = new File(getCacheDir(), "camera-capture");
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IllegalStateException();
                cameraFile = new File(folder, UUID.randomUUID() + ".jpg");
                cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", cameraFile);
                Intent capture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                capture.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                capture.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(capture, CAPTURE);
            } catch (Exception exception) { toast("无法打开相机。"); finishFile(null); }
        }, () -> { toast("未获得相机权限。"); finishFile(null); });
    }

    private void withCamera(Runnable granted, Runnable denied) {
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            granted.run();
        } else if (pendingCamera != null) {
            denied.run();
        } else {
            pendingCamera = granted;
            pendingCameraDenied = denied;
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        }
    }

    @Override public void onRequestPermissionsResult(int request, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request != CAMERA_PERMISSION) return;
        Runnable action = results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
                ? pendingCamera : pendingCameraDenied;
        pendingCamera = null;
        pendingCameraDenied = null;
        if (action != null) action.run();
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (request == CAPTURE) {
            finishFile(result == RESULT_OK && cameraUri != null && cameraFile != null && cameraFile.length() > 0
                    ? new Uri[]{cameraUri} : null);
            cameraUri = null;
        } else if (request == CHOOSE_FILE) {
            Uri[] selected = WebChromeClient.FileChooserParams.parseResult(result, data);
            if (selected != null) for (Uri uri : selected) {
                if (uri == null || !"content".equals(uri.getScheme())) { selected = null; break; }
            }
            finishFile(selected);
        }
    }

    private void finishFile(Uri[] value) {
        ValueCallback<Uri[]> callback = fileCallback;
        fileCallback = null;
        if (callback != null) callback.onReceiveValue(value);
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (web != null) web.saveState(state);
        super.onSaveInstanceState(state);
    }

    @Override protected void onPause() {
        if (web != null) web.onPause();
        CookieManager.getInstance().flush();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (web != null) web.onResume();
    }

    @Override public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override protected void onDestroy() {
        finishFile(null);
        if (browserPermission != null) browserPermission.deny();
        pendingCamera = null;
        pendingCameraDenied = null;
        worker.shutdownNow();
        if (web != null) { web.stopLoading(); web.destroy(); }
        if (photos != null) photos.clearTemporaryFiles();
        File[] captures = new File(getCacheDir(), "camera-capture").listFiles();
        if (captures != null) for (File file : captures) if (!file.delete()) file.deleteOnExit();
        super.onDestroy();
    }

    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }
}
