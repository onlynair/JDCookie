package com.app.jdcookie;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.chad.library.adapter.base.BaseQuickAdapter;
import com.chad.library.adapter.base.listener.OnItemClickListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import okhttp3.Cookie;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private WebView webBridgeWebView;
    private ProgressBar webBridgeProgressBar;
    private final static String JD_URL = "https://m.jd.com/";
    private MyAdapter adapter;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        initWebView();

        initData();
    }

    private void initView() {

        findViewById(R.id.main_back).setOnClickListener(this);
        findViewById(R.id.main_clear).setOnClickListener(this);
        webBridgeProgressBar = findViewById(R.id.main_progress_bar);

        recyclerView = findViewById(R.id.main_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter = new MyAdapter());
        adapter.setOnItemClickListener((adapter, view, position) -> {
            //复制到剪切板
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            // 创建普通字符型ClipData
            ClipData mClipData = ClipData.newPlainText("Label", MainActivity.this.adapter.getItem(position));
            // 将ClipData内容放到系统剪贴板里。
            cm.setPrimaryClip(mClipData);
            Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
        });

        adapter.setEmptyView(R.layout.empty_layout);
    }

    private void initData() {
        webBridgeWebView.loadUrl(JD_URL);
    }

    private void initWebView() {


        webBridgeWebView = findViewById(R.id.main_web_view);
        WebSettings webSetting = webBridgeWebView.getSettings();
        webSetting.setJavaScriptEnabled(true);

        webSetting.setBuiltInZoomControls(true);
        webSetting.setDisplayZoomControls(false);
        webSetting.setUseWideViewPort(true);

        webSetting.setBlockNetworkImage(false);
//        webSetting.setUserAgentString(userAgent);
//        if (NetWorkUtils.isNetConnected(App.getAppContext())) {
        //缓存模式
        webSetting.setCacheMode(WebSettings.LOAD_NO_CACHE);
//        } else {
//            //缓存模式
//            webSetting.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
//        }

        webSetting.setDatabaseEnabled(true);
        webSetting.setDomStorageEnabled(true);
        webSetting.setAppCacheMaxSize(1024 * 1024 * 8);
        webSetting.setAppCachePath(getFilesDir().getAbsolutePath());
        webSetting.setDatabasePath(getFilesDir().getAbsolutePath());
        webSetting.setAllowFileAccess(true);
        webSetting.setAppCacheEnabled(true);
        webSetting.setTextZoom(100);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            webSetting.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        }

//        WebView.setWebContentsDebuggingEnabled(BuilD);
        webBridgeWebView.setWebViewClient(new MyWebViewClient(webBridgeProgressBar, (cookie, pt_key) -> runOnUiThread(() -> {
            adapter.addData(pt_key);
            recyclerView.scrollToPosition(adapter.getData().size() - 1);
        })));
        webBridgeWebView.setWebChromeClient(new MyWebChromeClient(webBridgeProgressBar));
    }


    public static class MyWebViewClient extends WebViewClient {

        private final ProgressBar webBridgeProgressBar;
        private final CookieListener cookieListener;

        public MyWebViewClient(ProgressBar webBridgeProgressBar, CookieListener cookieListener) {

            this.webBridgeProgressBar = webBridgeProgressBar;
            this.cookieListener = cookieListener;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            if (webBridgeProgressBar != null) {
                webBridgeProgressBar.setVisibility(View.VISIBLE);
            }
            super.onPageStarted(view, url, favicon);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            if (webBridgeProgressBar != null) {
                webBridgeProgressBar.setVisibility(View.GONE);
            }
            super.onPageFinished(view, url);
        }

        @Nullable
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request1) {

            try {
                CookieManager cookieManager = CookieManager.getInstance();
                String cookie = cookieManager.getCookie(request1.getUrl().toString());
                if (cookieListener != null && !TextUtils.isEmpty(cookie) && cookie.contains("pt_key")) {

                    int ptKeyIndex = cookie.indexOf("pt_key");
                    //截取 pt_key 之后的字符串
                    String pt_key = cookie.substring(ptKeyIndex);

                    int ptPinIndex = pt_key.indexOf("pt_pin");
                    String pt_pin = pt_key.substring(ptPinIndex);
                    pt_pin = pt_pin.substring(0, pt_pin.indexOf(";", 1) + 1);

                    //截取到"；"前的 pt_key
                    pt_key = pt_key.substring(0, pt_key.indexOf(";", 1) + 1);


                    cookieListener.onCookie(cookie, pt_key + pt_pin);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return super.shouldInterceptRequest(view, request1);
        }
    }

    public static class MyWebChromeClient extends WebChromeClient {

        private final ProgressBar webBridgeProgressBar;

        public MyWebChromeClient(ProgressBar webBridgeProgressBar) {
            this.webBridgeProgressBar = webBridgeProgressBar;
        }

        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (null != webBridgeProgressBar) {
                webBridgeProgressBar.setProgress(newProgress);
                if (newProgress == 100) {
                }
            }
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            if (!view.getTitle().startsWith("http")) {
//                if (webBridgeNormalTitleBar != null) {
//                    webBridgeNormalTitleBar.setTitleText(view.getTitle());
//                }
            }
        }

    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.main_back) {
            //返回上一页面
            if (webBridgeWebView.canGoBack()) {
                webBridgeWebView.goBack();
            } else {
                onBackPressed();
            }
        } else if (v.getId() == R.id.main_clear) {
            if (adapter != null) {
                adapter.getData().clear();
                adapter.notifyDataSetChanged();
            }
        }
    }
}