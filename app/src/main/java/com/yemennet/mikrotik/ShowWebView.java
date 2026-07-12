package com.yemennet.mikrotik;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;
import android.util.Log;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.messaging.FirebaseMessaging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ShowWebView extends AppCompatActivity {

    private WebView webView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ProgressBar progressBar;
    private ImageButton btnWhatsApp;
    private ImageButton btnReportIssue;
    private View fabContainer;
    private FrameLayout fullscreenContainer;
    private View customView;
    private WebChromeClient.CustomViewCallback customViewCallback;

    private static final String WEB_URL = "http://www.y.net/status";
    private static final String WHATSAPP_NUMBER = "967771908495";
    private static final String SUPPORT_EMAIL = "hanialbairy1996@gmail.com";
    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private static final int STORAGE_PERMISSION_CODE = 101;
    private static final String DOWNLOAD_CHANNEL_ID = "download_channel";
    private static final int DOWNLOAD_NOTIFICATION_BASE = 2000;

    private BroadcastReceiver networkReceiver;
    private final ExecutorService downloadExecutor = Executors.newSingleThreadExecutor();
    private int activeDownloadCount = 0;

    private boolean haveNetworkConnection() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo[] netInfo = cm.getAllNetworkInfo();
            for (NetworkInfo ni : netInfo) {
                if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                    if (ni.isConnected())
                        haveConnectedWifi = true;
                if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                    if (ni.isConnected())
                        haveConnectedMobile = true;
            }
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.show_web_view);

        webView = (WebView) findViewById(R.id.webView1);
        swipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipeRefreshLayout);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnWhatsApp = findViewById(R.id.btnWhatsApp);
        btnReportIssue = findViewById(R.id.btnReportIssue);
        fabContainer = findViewById(R.id.fabContainer);

        try {
            AssetManager assetManager = getAssets();
            Bitmap whatsappBitmap = BitmapFactory.decodeStream(assetManager.open("whatsapp.png"));
            btnWhatsApp.setImageBitmap(whatsappBitmap);
        } catch (Exception e) {
            Log.e("ShowWebView", "Could not load whatsapp.png from assets", e);
        }

        fullscreenContainer = new FrameLayout(this);
        fullscreenContainer.setBackgroundColor(0xFF000000);

        btnWhatsApp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openWhatsApp();
            }
        });

        btnReportIssue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showReportIssueDialog();
            }
        });

        swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                webView.reload();
            }
        });

        createDownloadNotificationChannel();
        setupWebView();

        requestNotificationPermission();

        FirebaseMessaging.getInstance().getToken()
            .addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    return;
                }
                String token = task.getResult();
                Log.d("FCM", "Token: " + token);
            });

        if (haveNetworkConnection()) {
            webView.loadUrl(WEB_URL);
        } else {
            webView.loadUrl("file:///android_asset/error.html");
        }
    }

    private void createDownloadNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    DOWNLOAD_CHANNEL_ID,
                    "التنزيلات",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("إشعارات تقدم التنزيل");
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private void openWhatsApp() {
        try {
            String url = "https://api.whatsapp.com/send?phone=" + WHATSAPP_NUMBER;
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.setPackage(null);
            Intent chooser = Intent.createChooser(intent, "اختر تطبيق");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(chooser);
        } catch (Exception e) {
            Toast.makeText(this, "WhatsApp غير مثبت على الجهاز", Toast.LENGTH_SHORT).show();
        }
    }

    private void showReportIssueDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.report_issue_dialog, null);
        builder.setView(dialogView);

        final Spinner spinnerIssueType = dialogView.findViewById(R.id.spinnerIssueType);
        final EditText editSubscriberNumber = dialogView.findViewById(R.id.editSubscriberNumber);
        final EditText editDescription = dialogView.findViewById(R.id.editDescription);

        String[] issueTypes = {
            "اختيار نوع العطل",
            "انقطاع الإنترنت",
            "بطء السرعة",
            "انقطع الكهرباء",
            "عطل في الراوتر",
            "مشكلة في الفاتورة",
            "أخرى"
        };

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            issueTypes
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerIssueType.setAdapter(adapter);

        builder.setPositiveButton("إرسال", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String issueType = spinnerIssueType.getSelectedItem().toString();
                String subscriberNumber = editSubscriberNumber.getText().toString().trim();
                String description = editDescription.getText().toString().trim();

                if (subscriberNumber.isEmpty()) {
                    Toast.makeText(ShowWebView.this, "يرجى إدخال رقم المشترك", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (issueType.equals("اختيار نوع العطل")) {
                    Toast.makeText(ShowWebView.this, "يرجى اختيار نوع العطل", Toast.LENGTH_SHORT).show();
                    return;
                }

                sendReportEmail(issueType, subscriberNumber, description);
            }
        });

        builder.setNegativeButton("إلغاء", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void sendReportEmail(String issueType, String subscriberNumber, String description) {
        String subject = "بلاغ عطل - " + issueType;
        String body = "نوع العطل: " + issueType + "\n" +
                      "رقم المشترك: " + subscriberNumber + "\n" +
                      "وصف العطل: " + (description.isEmpty() ? "لا يوجد وصف" : description);

        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            startActivity(Intent.createChooser(intent, "إرسال البلاغ عبر..."));
        } catch (Exception e) {
            Toast.makeText(this, "لا يوجد تطبيق بريد إلكتروني", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);
        settings.setSupportZoom(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                if (newProgress < 100) {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }

            @Override
            public void onShowCustomView(View view, CustomViewCallback callback) {
                if (customView != null) {
                    callback.onCustomViewHidden();
                    return;
                }

                customView = view;
                customViewCallback = callback;

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.addView(fullscreenContainer, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                webView.setVisibility(View.GONE);
                fabContainer.setVisibility(View.GONE);

                fullscreenContainer.addView(view, new FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT));

                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }

            @Override
            public void onHideCustomView() {
                if (customView == null) return;

                FrameLayout decor = (FrameLayout) getWindow().getDecorView();
                decor.removeView(fullscreenContainer);

                fullscreenContainer.removeView(customView);

                webView.setVisibility(View.VISIBLE);
                fabContainer.setVisibility(View.VISIBLE);

                customView = null;
                if (customViewCallback != null) {
                    customViewCallback.onCustomViewHidden();
                    customViewCallback = null;
                }

                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }
        });

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (swipeRefreshLayout.isRefreshing()) {
                    swipeRefreshLayout.setRefreshing(false);
                }
            }
        });

        webView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {
                swipeRefreshLayout.setEnabled(webView.getScrollY() == 0);
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                    if (ContextCompat.checkSelfPermission(ShowWebView.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(ShowWebView.this,
                                new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                STORAGE_PERMISSION_CODE);
                        return;
                    }
                }
                startDownload(url, userAgent, contentDisposition, mimeType);
            }
        });
    }

    private void startDownload(String url, String userAgent, String contentDisposition, String mimeType) {
        final String fileName = URLUtil.guessFileName(url, contentDisposition, mimeType);
        final int notifId = DOWNLOAD_NOTIFICATION_BASE + activeDownloadCount++;

        NotificationManagerCompat nm = NotificationManagerCompat.from(this);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(fileName)
                .setContentText("جاري التنزيل...")
                .setProgress(100, 0, true)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        try {
            nm.notify(notifId, builder.build());
        } catch (Exception ignored) {}

        Toast.makeText(this, "بدأ التنزيل: " + fileName, Toast.LENGTH_SHORT).show();

        final String allCookies = CookieManager.getInstance().getCookie(url);
        final String pageUrl = webView.getUrl() != null ? webView.getUrl() : url;
        final String referer = webView.getUrl() != null ? webView.getUrl() : url;

        downloadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                InputStream input = null;
                OutputStream output = null;
                HttpURLConnection connection = null;
                try {
                    URL downloadUrl = new URL(url);
                    connection = (HttpURLConnection) downloadUrl.openConnection();
                    connection.setConnectTimeout(30000);
                    connection.setReadTimeout(60000);
                    connection.setInstanceFollowRedirects(true);
                    connection.setRequestProperty("User-Agent", webView.getSettings().getUserAgentString());
                    connection.setRequestProperty("Accept", "*/*");
                    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9,ar;q=0.8");
                    connection.setRequestProperty("Referer", referer);
                    connection.setRequestProperty("Accept-Encoding", "identity");

                    if (allCookies != null && !allCookies.isEmpty()) {
                        connection.setRequestProperty("Cookie", allCookies);
                    }

                    connection.connect();

                    int responseCode = connection.getResponseCode();
                    Log.d("ShowWebView", "Download response code: " + responseCode + " for URL: " + url);

                    if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                        String newUrl = connection.getHeaderField("Location");
                        connection.disconnect();
                        if (newUrl != null) {
                            Log.d("ShowWebView", "Redirect to: " + newUrl);
                            connection = (HttpURLConnection) new URL(newUrl).openConnection();
                            connection.setConnectTimeout(30000);
                            connection.setReadTimeout(60000);
                            connection.setInstanceFollowRedirects(true);
                            connection.setRequestProperty("User-Agent", webView.getSettings().getUserAgentString());
                            connection.setRequestProperty("Accept", "*/*");
                            connection.setRequestProperty("Referer", referer);
                            if (allCookies != null && !allCookies.isEmpty()) {
                                connection.setRequestProperty("Cookie", allCookies);
                            }
                            connection.connect();
                            responseCode = connection.getResponseCode();
                        }
                    }

                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        throw new Exception("خطأ في الخادم: " + responseCode);
                    }

                    String contentType = connection.getContentType();
                    int contentLength = connection.getContentLength();
                    Log.d("ShowWebView", "Content-Type: " + contentType + " Length: " + contentLength);

                    if (contentType != null && contentType.contains("text/html")) {
                        throw new Exception("الخادم أعاد صفحة HTML بدلاً من الملف - تأكد من تسجيل الدخول");
                    }

                    input = connection.getInputStream();

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Downloads.DISPLAY_NAME, fileName);
                        values.put(MediaStore.Downloads.MIME_TYPE, mimeType != null ? mimeType : "application/octet-stream");
                        values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);

                        Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
                        if (uri == null) {
                            throw new Exception("فشل في إنشاء ملف");
                        }
                        output = getContentResolver().openOutputStream(uri);
                    } else {
                        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                        if (!downloadsDir.exists()) {
                            downloadsDir.mkdirs();
                        }
                        File outputFile = new File(downloadsDir, fileName);
                        output = new FileOutputStream(outputFile);
                    }

                    byte[] buffer = new byte[8192];
                    long total = 0;
                    int count;
                    int lastProgress = 0;
                    while ((count = input.read(buffer)) != -1) {
                        total += count;
                        output.write(buffer, 0, count);

                        int progress;
                        if (contentLength > 0) {
                            progress = (int) (total * 100 / contentLength);
                        } else {
                            progress = (int) (total / 1024);
                        }
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            final int p = progress;
                            final long t = total;
                            final int fl = contentLength;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    String text;
                                    if (fl > 0) {
                                        text = "جاري التنزيل... " + p + "% (" + (t / 1024) + " KB)";
                                    } else {
                                        text = "جاري التنزيل... " + (t / 1024) + " KB";
                                    }
                                    builder.setProgress(100, p, fl <= 0);
                                    builder.setContentText(text);
                                    try {
                                        nm.notify(notifId, builder.build());
                                    } catch (Exception ignored) {}
                                }
                            });
                        }
                    }

                    output.flush();
                    output.close();
                    output = null;
                    input.close();
                    input = null;
                    connection.disconnect();

                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                        MediaScannerConnection.scanFile(ShowWebView.this,
                                new String[]{new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName).getAbsolutePath()},
                                null, null);
                    }

                    final long finalTotal = total;
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ShowWebView.this, "اكتمل التنزيل: " + fileName + " (" + (finalTotal / 1024) + " KB)", Toast.LENGTH_LONG).show();
                        }
                    });

                    builder.setProgress(0, 0, false)
                            .setOngoing(false)
                            .setContentText("اكتمل التنزيل - " + (total / 1024) + " KB")
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setAutoCancel(true);
                    try {
                        nm.notify(notifId, builder.build());
                    } catch (Exception ignored) {}

                } catch (Exception e) {
                    Log.e("ShowWebView", "Download failed", e);
                    final String errorMsg = e.getMessage();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(ShowWebView.this, "فشل التنزيل: " + (errorMsg != null ? errorMsg : "خطأ غير معروف"), Toast.LENGTH_LONG).show();
                        }
                    });
                    builder.setProgress(0, 0, false)
                            .setOngoing(false)
                            .setContentText("فشل التنزيل")
                            .setSmallIcon(android.R.drawable.stat_notify_error)
                            .setAutoCancel(true);
                    try {
                        nm.notify(notifId, builder.build());
                    } catch (Exception ignored) {}
                } finally {
                    try {
                        if (output != null) output.close();
                        if (input != null) input.close();
                    } catch (Exception ignored) {}
                    if (connection != null) connection.disconnect();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        webView.onResume();
        networkReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (!haveNetworkConnection()) {
                    showNoNetworkDialog();
                }
            }
        };
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(networkReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(networkReceiver, filter);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        webView.onPause();
        if (networkReceiver != null) {
            unregisterReceiver(networkReceiver);
        }
    }

    private void showNoNetworkDialog() {
        new AlertDialog.Builder(this)
            .setTitle("انقطع الاتصال")
            .setMessage("يرجى التأكد من اتصالك بشبكة الواي فاي الخاصة بنا")
            .setPositiveButton("حسناً", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .setCancelable(false)
            .show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "تم تفعيل الإشعارات", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "يمكنك الآن تنزيل الملفات", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "لا يمكن التنزيل بدون إذن التخزين", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (customView != null) {
            webView.setWebChromeClient(new WebChromeClient());
            webView.getWebChromeClient().onHideCustomView();
        } else if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            webView.destroy();
        }
        downloadExecutor.shutdownNow();
        super.onDestroy();
    }
}
