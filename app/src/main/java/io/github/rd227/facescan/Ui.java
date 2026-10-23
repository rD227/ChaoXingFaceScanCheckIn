package io.github.rd227.facescan;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

final class Ui {
    static final int BLUE = Color.rgb(37, 99, 235);
    static final int INK = Color.rgb(26, 39, 62);
    static final int MUTED = Color.rgb(100, 116, 139);
    static final int BACKGROUND = Color.rgb(243, 246, 251);

    private Ui() { }
    static int dp(Context context, int value) {
        return Math.round(value * context.getResources().getDisplayMetrics().density);
    }
    static TextView text(Context context, String value, int size, int color, boolean bold) {
        TextView view = new TextView(context);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setLineSpacing(dp(context, 3), 1);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        view.setPadding(0, dp(context, 4), 0, dp(context, 4));
        return view;
    }
    static GradientDrawable rounded(Context context, int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(dp(context, radius));
        return shape;
    }
    static LinearLayout card(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int padding = dp(context, 18);
        card.setPadding(padding, padding, padding, padding);
        card.setBackground(rounded(context, Color.WHITE, 22));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.topMargin = dp(context, 14);
        card.setLayoutParams(params);
        return card;
    }
    static Button button(Context context, String label, boolean primary) {
        Button button = new Button(context);
        button.setText(label);
        button.setTextSize(15);
        button.setAllCaps(false);
        button.setTextColor(primary ? Color.WHITE : BLUE);
        button.setMinHeight(dp(context, 50));
        button.setBackgroundTintList(null);
        button.setBackground(rounded(context, primary ? BLUE : Color.rgb(235, 241, 255), 14));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(context, 50));
        params.topMargin = dp(context, 10);
        button.setLayoutParams(params);
        return button;
    }
    static void edgeInsets(Activity activity, View view, int horizontalDp, int verticalDp) {
        if (Build.VERSION.SDK_INT >= 30) activity.getWindow().setDecorFitsSystemWindows(false);
        activity.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        int horizontal = dp(activity, horizontalDp);
        int vertical = dp(activity, verticalDp);
        view.setOnApplyWindowInsetsListener((target, insets) -> {
            target.setPadding(horizontal + insets.getSystemWindowInsetLeft(),
                    vertical + insets.getSystemWindowInsetTop(),
                    horizontal + insets.getSystemWindowInsetRight(),
                    vertical + insets.getSystemWindowInsetBottom());
            return insets;
        });
        view.requestApplyInsets();
    }
}
