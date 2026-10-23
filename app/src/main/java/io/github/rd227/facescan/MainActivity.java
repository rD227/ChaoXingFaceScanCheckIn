package io.github.rd227.facescan;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebSettings;
import android.webkit.WebStorage;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONObject;
import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

public final class MainActivity extends Activity {
    static final AtomicLong SESSION_EPOCH = new AtomicLong();
    private static final int PICK_PHOTO = 1001;
    private static final int LOGIN = 1002;
    private static final String ACCOUNT_URL = "https://sso.chaoxing.com/apis/login/userLogin4Uname.do";
    private static final String LOGIN_URL = "https://passport2.chaoxing.com/login?refer=https%3A%2F%2Fi.chaoxing.com";
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private FacePhotoStore photos;
    private TextView accountStatus;
    private TextView photoStatus;
    private TextView qrStatus;
    private ImageView photoPreview;
    private Button refreshButton;
    private Button openQrButton;
    private URI pendingQr;
    private boolean checking;
    private boolean clearingSession;
    private boolean photoBusy;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        photos = new FacePhotoStore(this);
        CookieManager.getInstance().setAcceptCookie(true);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.BACKGROUND);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(content, new ScrollView.LayoutParams(-1, -2));
        setContentView(scroll);
        Ui.edgeInsets(this, content, 20, 20);

        content.addView(Ui.text(this, "ANDROID · 本机运行", 12, Ui.BLUE, true));
        content.addView(Ui.text(this, "FaceScan 实验室", 30, Ui.INK, true));
        content.addView(Ui.text(this, "保存本人照片，扫描课堂二维码。", 15, Ui.MUTED, false));

        LinearLayout accountCard = Ui.card(this);
        accountCard.addView(Ui.text(this, "01  账号会话", 18, Ui.INK, true));
        accountStatus = Ui.text(this, "尚未检查登录状态", 14, Ui.MUTED, false);
        accountStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        accountCard.addView(accountStatus);
        Button login = Ui.button(this, "在超星官方页面登录", true);
        login.setOnClickListener(view -> openOfficial(OfficialUrl.parse(LOGIN_URL), true));
        accountCard.addView(login);
        refreshButton = Ui.button(this, "检查本机登录会话", false);
        refreshButton.setOnClickListener(view -> checkSession());
        accountCard.addView(refreshButton);
        content.addView(accountCard);

        LinearLayout qrCard = Ui.card(this);
        qrCard.addView(Ui.text(this, "02  二维码签到", 18, Ui.INK, true));
        qrStatus = Ui.text(this, "扫码后先确认活动，再打开官方签到页。", 14, Ui.MUTED, false);
        qrStatus.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        qrCard.addView(qrStatus);
        Button scan = Ui.button(this, "扫描课堂二维码", true);
        scan.setOnClickListener(view -> new IntentIntegrator(this)
                .setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                .setPrompt("扫描超星官方课堂二维码")
                .setBeepEnabled(false).setOrientationLocked(false).initiateScan());
        qrCard.addView(scan);
        Button paste = Ui.button(this, "粘贴签到链接", false);
        paste.setOnClickListener(view -> pasteLink());
        qrCard.addView(paste);
        openQrButton = Ui.button(this, "打开官方签到页", false);
        openQrButton.setEnabled(false);
        openQrButton.setOnClickListener(view -> { if (pendingQr != null) openOfficial(pendingQr, false); });
        qrCard.addView(openQrButton);
        content.addView(qrCard);

        LinearLayout photoCard = Ui.card(this);
        photoCard.addView(Ui.text(this, "03  本人人脸照片", 18, Ui.INK, true));
        photoStatus = Ui.text(this, "还没有保存照片", 14, Ui.MUTED, false);
        photoCard.addView(photoStatus);
        photoPreview = new ImageView(this);
        photoPreview.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        photoPreview.setContentDescription("本机保存的人脸照片预览");
        photoPreview.setBackground(Ui.rounded(this, Ui.BACKGROUND, 14));
        photoCard.addView(photoPreview, new LinearLayout.LayoutParams(-1, Ui.dp(this, 190)));
        Button choose = Ui.button(this, "选择并保存本人照片", false);
        choose.setOnClickListener(view -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.setType("image/*");
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(intent, PICK_PHOTO);
        });
        photoCard.addView(choose);
        photoCard.addView(Ui.text(this, "照片加密保存在本机。官方页面允许选图时，可在上传菜单中选用；本机保存不代表官方人脸验证已通过。",
                12, Ui.MUTED, false));
        content.addView(photoCard);

        LinearLayout info = Ui.card(this);
        info.addView(Ui.text(this, "0.1 实验版", 16, Ui.INK, true));
        info.addView(Ui.text(this, "不连接本项目服务器。账号登录、验证和签到由超星官方服务处理。官方页面要求实时拍摄时，仍需完成相应操作。",
                13, Ui.MUTED, false));
        Button clear = Ui.button(this, "清除本机照片与登录状态", false);
        clear.setOnClickListener(view -> confirmClear());
        info.addView(clear);
        content.addView(info);
    }

    @Override protected void onResume() {
        super.onResume();
        loadPreview();
    }

    private void loadPreview() {
        if (!photos.exists()) {
            photoPreview.setImageDrawable(null);
            photoStatus.setText("还没有保存照片");
            return;
        }
        worker.execute(() -> {
            try {
                Bitmap bitmap = photos.preview();
                runOnUiThread(() -> {
                    if (isDestroyed()) { bitmap.recycle(); return; }
                    photoPreview.setImageBitmap(bitmap);
                    photoStatus.setText("已保存到本机 · 尚不代表官方验证通过");
                });
            } catch (Exception exception) { ui(() -> photoStatus.setText("照片无法解密，请重新选择。")); }
        });
    }

    private void checkSession() {
        if (checking || clearingSession) return;
        checking = true;
        refreshButton.setEnabled(false);
        accountStatus.setText("正在通过手机网络检查官方会话…");
        long generation = SESSION_EPOCH.get();
        String userAgent = WebSettings.getDefaultUserAgent(this);
        OfficialHttp client = new OfficialHttp(new WebSessionCookies(CookieManager.getInstance(),
                SESSION_EPOCH), userAgent);
        worker.execute(() -> {
            String result;
            try {
                OfficialHttp.Response response = client.get(ACCOUNT_URL);
                JSONObject object = new JSONObject(response.body);
                JSONObject account = object.optJSONObject("msg");
                String uid = account == null ? "" : account.optString("uid", "");
                if (response.status == 200 && uid.matches("[1-9][0-9]*")) {
                    result = "已确认登录：" + account.optString("name", "当前用户");
                } else {
                    result = "尚未确认登录，请先完成官方网页登录。";
                }
            } catch (Exception exception) {
                result = "未能读取官方会话；请确认网络与网页登录状态。";
            }
            String message = result;
            ui(() -> {
                checking = false;
                refreshButton.setEnabled(!clearingSession);
                if (generation == SESSION_EPOCH.get()) accountStatus.setText(message);
            });
        });
    }

    private void pasteLink() {
        EditText input = new EditText(this);
        input.setHint("https://mobilelearn.chaoxing.com/…");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setMaxLines(5);
        int padding = Ui.dp(this, 20);
        input.setPadding(padding, padding, padding, padding);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("粘贴超星官方链接")
                .setView(input).setNegativeButton("取消", null).setPositiveButton("解析", null).create();
        dialog.setOnShowListener(ignored -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            if (acceptQr(input.getText().toString())) dialog.dismiss();
            else input.setError("请检查超星官方链接格式");
        }));
        dialog.show();
    }

    private boolean acceptQr(String value) {
        try {
            URI parsed = OfficialUrl.parse(value);
            String activity = OfficialUrl.activityId(parsed);
            pendingQr = parsed;
            qrStatus.setText("来源：" + parsed.getHost() + (activity.isEmpty()
                    ? "\n未提取到活动编号，请在官方页面确认。" : "\n活动编号：" + activity));
            openQrButton.setEnabled(true);
            toast("链接已解析，尚未提交签到。");
            return true;
        } catch (IllegalArgumentException exception) {
            toast("这不是受支持的超星官方签到链接。");
            return false;
        }
    }

    private void openOfficial(URI uri, boolean login) {
        Intent intent = new Intent(this, OfficialWebActivity.class);
        intent.putExtra("url", uri.toASCIIString());
        if (login) startActivityForResult(intent, LOGIN);
        else startActivity(intent);
    }

    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        IntentResult scan = IntentIntegrator.parseActivityResult(request, result, data);
        if (scan != null) {
            if (scan.getContents() != null) acceptQr(scan.getContents());
            return;
        }
        if (request == LOGIN) { checkSession(); return; }
        if (request == PICK_PHOTO && result == RESULT_OK && data != null && data.getData() != null) {
            if (photoBusy) return;
            photoBusy = true;
            photoStatus.setText("正在压缩并加密保存…");
            Uri source = data.getData();
            worker.execute(() -> {
                try {
                    photos.save(source);
                    ui(() -> { photoBusy = false; loadPreview(); toast("照片已保存到本机。"); });
                } catch (Exception exception) {
                    ui(() -> { photoBusy = false; photoStatus.setText("照片保存失败，请换一张图片重试。"); });
                }
            });
        }
    }

    private void confirmClear() {
        if (photoBusy) { toast("请等照片保存完成。"); return; }
        new AlertDialog.Builder(this).setTitle("清除本机资料？")
                .setMessage("将删除本机保存的照片与登录状态。不会修改超星服务器上的资料或签到记录。")
                .setNegativeButton("取消", null).setPositiveButton("清除", (dialog, which) -> {
                    clearingSession = true;
                    refreshButton.setEnabled(false);
                    synchronized (SESSION_EPOCH) { SESSION_EPOCH.incrementAndGet(); }
                    CookieManager.getInstance().removeAllCookies(removed -> {
                        CookieManager.getInstance().flush();
                        WebStorage.getInstance().deleteAllData();
                        clearingSession = false;
                        refreshButton.setEnabled(!checking);
                        accountStatus.setText("本机登录状态已清除");
                    });
                    worker.execute(() -> {
                        try {
                            photos.delete();
                            ui(() -> { photoPreview.setImageDrawable(null); photoStatus.setText("还没有保存照片"); });
                        } catch (Exception exception) { ui(() -> toast("照片删除失败，请重试。")); }
                    });
                    pendingQr = null;
                    qrStatus.setText("扫码后先确认活动，再打开官方签到页。");
                    openQrButton.setEnabled(false);
                }).show();
    }

    private void ui(Runnable action) {
        runOnUiThread(() -> { if (!isFinishing() && !isDestroyed()) action.run(); });
    }
    private void toast(String message) { Toast.makeText(this, message, Toast.LENGTH_LONG).show(); }

    @Override protected void onDestroy() {
        worker.shutdownNow();
        super.onDestroy();
    }
}
