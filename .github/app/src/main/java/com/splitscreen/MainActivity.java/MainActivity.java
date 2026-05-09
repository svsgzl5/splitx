package com.splitscreen;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI
    private LinearLayout rootLayout, panelA, panelB;
    private View divider;
    private WebView webViewA, webViewB;
    private EditText urlInputA, urlInputB;

    // Drag state
    private float dragStartY, panelAStartWeight, panelBStartWeight;
    private boolean isDragging = false;
    private static final float MIN_WEIGHT = 0.15f;
    private static final float MAX_WEIGHT = 0.85f;

    // Preset apps — kullanıcı ⊞ butonuna basınca bunlar çıkar
    private final AppItem[] PRESET_APPS = {
        new AppItem("ChatGPT", "🤖", "https://chatgpt.com"),
        new AppItem("YouTube", "▶", "https://m.youtube.com"),
        new AppItem("Instagram", "📸", "https://www.instagram.com"),
        new AppItem("TikTok", "🎵", "https://www.tiktok.com"),
        new AppItem("Twitter / X", "𝕏", "https://mobile.twitter.com"),
        new AppItem("WhatsApp Web", "💬", "https://web.whatsapp.com"),
        new AppItem("Google", "G", "https://www.google.com"),
        new AppItem("Gmail", "✉", "https://mail.google.com"),
        new AppItem("Google Maps", "📍", "https://maps.google.com"),
        new AppItem("Netflix", "N", "https://www.netflix.com"),
        new AppItem("Spotify Web", "♫", "https://open.spotify.com"),
        new AppItem("Reddit", "R", "https://www.reddit.com"),
        new AppItem("Wikipedia", "W", "https://tr.m.wikipedia.org"),
        new AppItem("Hepsiburada", "H", "https://www.hepsiburada.com"),
        new AppItem("Trendyol", "T", "https://www.trendyol.com"),
        new AppItem("Ekşi Sözlük", "E", "https://eksisozluk.com"),
        new AppItem("n11", "n", "https://www.n11.com"),
        new AppItem("GitHub", "⌥", "https://github.com"),
        new AppItem("Özel URL", "+", null),
    };

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind views
        rootLayout = findViewById(R.id.rootLayout);
        panelA     = findViewById(R.id.panelA);
        panelB     = findViewById(R.id.panelB);
        divider    = findViewById(R.id.divider);
        webViewA   = findViewById(R.id.webViewA);
        webViewB   = findViewById(R.id.webViewB);
        urlInputA  = findViewById(R.id.urlInputA);
        urlInputB  = findViewById(R.id.urlInputB);

        // Setup WebViews
        setupWebView(webViewA, "A");
        setupWebView(webViewB, "B");

        // Buttons
        findViewById(R.id.btnGoA).setOnClickListener(v -> loadUrl('A'));
        findViewById(R.id.btnGoB).setOnClickListener(v -> loadUrl('B'));
        findViewById(R.id.btnReloadA).setOnClickListener(v -> webViewA.reload());
        findViewById(R.id.btnReloadB).setOnClickListener(v -> webViewB.reload());
        findViewById(R.id.btnPickA).setOnClickListener(v -> showAppPicker('A'));
        findViewById(R.id.btnPickB).setOnClickListener(v -> showAppPicker('B'));

        // URL input — klavyeden "Git" (Go)
        urlInputA.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) { loadUrl('A'); return true; }
            return false;
        });
        urlInputB.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO) { loadUrl('B'); return true; }
            return false;
        });

        // Divider drag
        divider.setOnTouchListener(this::onDividerTouch);

        // Load initial pages
        loadUrl('A');
        loadUrl('B');
    }

    // ── WebView setup ──────────────────────────────────────────
    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView(WebView wv, String panel) {
        WebSettings s = wv.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(true);
        s.setBuiltInZoomControls(true);
        s.setDisplayZoomControls(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 13; Redmi 14C) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36"
        );

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true);

        wv.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                view.loadUrl(url);
                // URL barını güncelle
                if (panel.equals("A")) urlInputA.setText(url);
                else urlInputB.setText(url);
                return true;
            }
        });

        wv.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(PermissionRequest request) {
                // Kamera ve mikrofon iznini otomatik ver
                request.grant(request.getResources());
            }
        });
    }

    // ── URL yükleme ────────────────────────────────────────────
    private void loadUrl(char panel) {
        EditText input = (panel == 'A') ? urlInputA : urlInputB;
        WebView wv    = (panel == 'A') ? webViewA  : webViewB;

        String raw = input.getText().toString().trim();
        if (raw.isEmpty()) return;

        String url = normalizeUrl(raw);
        input.setText(url);
        wv.loadUrl(url);
    }

    private String normalizeUrl(String raw) {
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
        // Eğer nokta içeriyorsa URL, yoksa Google araması
        if (raw.contains(".") && !raw.contains(" ")) return "https://" + raw;
        return "https://www.google.com/search?q=" + raw.replace(" ", "+");
    }

    // ── App Picker Dialog ──────────────────────────────────────
    private void showAppPicker(char panel) {
        Dialog dialog = new Dialog(this, R.style.AppPickerDialog);
        dialog.setContentView(R.layout.dialog_app_picker);
        dialog.getWindow().setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );

        RecyclerView rv = dialog.findViewById(R.id.appList);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(new AppPickerAdapter(PRESET_APPS, item -> {
            dialog.dismiss();
            if (item.url == null) {
                // Özel URL — input'a odaklan
                EditText input = (panel == 'A') ? urlInputA : urlInputB;
                input.setText("");
                input.requestFocus();
                Toast.makeText(this, "URL girin ve Git'e basın", Toast.LENGTH_SHORT).show();
            } else {
                EditText input = (panel == 'A') ? urlInputA : urlInputB;
                WebView wv    = (panel == 'A') ? webViewA  : webViewB;
                input.setText(item.url);
                wv.loadUrl(item.url);
            }
        }));

        dialog.show();
    }

    // ── Divider drag ───────────────────────────────────────────
    private boolean onDividerTouch(View v, MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isDragging = true;
                dragStartY = e.getRawY();
                panelAStartWeight = ((LinearLayout.LayoutParams) panelA.getLayoutParams()).weight;
                panelBStartWeight = ((LinearLayout.LayoutParams) panelB.getLayoutParams()).weight;
                return true;

            case MotionEvent.ACTION_MOVE:
                if (!isDragging) return false;
                float dy = e.getRawY() - dragStartY;
                float totalH = rootLayout.getHeight() - divider.getHeight();
                if (totalH <= 0) return false;

                float delta = dy / totalH;
                float newA = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT,
                    panelAStartWeight + delta));
                float newB = (panelAStartWeight + panelBStartWeight) - newA;

                LinearLayout.LayoutParams lpA = (LinearLayout.LayoutParams) panelA.getLayoutParams();
                LinearLayout.LayoutParams lpB = (LinearLayout.LayoutParams) panelB.getLayoutParams();
                lpA.weight = newA;
                lpB.weight = newB;
                panelA.setLayoutParams(lpA);
                panelB.setLayoutParams(lpB);
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isDragging = false;
                return true;
        }
        return false;
    }

    // ── Back button ────────────────────────────────────────────
    @Override
    public void onBackPressed() {
        // Aktif WebView'de geri git
        if (webViewB.hasFocus() && webViewB.canGoBack()) {
            webViewB.goBack();
        } else if (webViewA.canGoBack()) {
            webViewA.goBack();
        } else {
            super.onBackPressed();
        }
    }

    // ══════════════════════════════════════════════════════════
    // AppItem model
    // ══════════════════════════════════════════════════════════
    static class AppItem {
        String name, emoji, url;
        AppItem(String name, String emoji, String url) {
            this.name = name; this.emoji = emoji; this.url = url;
        }
    }

    // ══════════════════════════════════════════════════════════
    // AppPickerAdapter
    // ══════════════════════════════════════════════════════════
    static class AppPickerAdapter extends RecyclerView.Adapter<AppPickerAdapter.VH> {
        interface OnPick { void pick(AppItem item); }
        private final AppItem[] items;
        private final OnPick listener;

        AppPickerAdapter(AppItem[] items, OnPick listener) {
            this.items = items; this.listener = listener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = android.view.LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_app, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            AppItem item = items[pos];
            h.name.setText(item.name);
            h.url.setText(item.url != null ? item.url : "Özel URL gir");
            h.emoji.setText(item.emoji);
            h.itemView.setOnClickListener(v -> listener.pick(item));
        }

        @Override public int getItemCount() { return items.length; }

        static class VH extends RecyclerView.ViewHolder {
            TextView name, url, emoji;
            VH(View v) {
                super(v);
                name  = v.findViewById(R.id.appName);
                url   = v.findViewById(R.id.appUrl);
                emoji = v.findViewById(R.id.appIcon);
            }
        }
    }
}
