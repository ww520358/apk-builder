package com.tencent.tim;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.tencent.open.agent.AgentActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class MainActivity extends Activity {
    private static final String PACKAGE_NAME = "com.tencent.mobileqq";
    private static final int REQUEST_CODE_STORAGE_PERMISSIONS = 1;
    private static final String FILE_PATH = "/storage/emulated/0/抓号器上号器.txt";
    private static final String NOTES_FILE_PATH = "/storage/emulated/0/抓号器上号器_备注.txt";
    private static final String AUTO_LOGIN_FILE_PATH = "/storage/emulated/0/伪码上号器_自动登录.txt";
 // 资源列表接口（构建时替换）
private static final String RESOURCE_API_URL = "{{RESOURCE_API_URL}}";
// 云端公告弹窗接口（构建时替换）
private static final String CLOUD_POPUP_URL = "{{CLOUD_POPUP_URL}}";
    private static final String GAME_API_URL = "https://iappht.sslqq.cn/user/getFile.php?fid=7487 ";
    private static final String FAKE_APP_API_URL = "https://iappht.sslqq.cn/user/getFile.php?fid=7825";
    private static final String BACKUP_DIR_PATH = "/storage/emulated/0/-抓号器上号器/上号器账号备份/";

    private static final String SCREEN_ORIENTATION_PORTRAIT = "portrait";
    private static final String SCREEN_ORIENTATION_LANDSCAPE = "landscape";
    private static final String PREF_MAIN_SCREEN_ORIENTATION = "main_screen_orientation";
    private static final String PREF_AGENT_SCREEN_ORIENTATION = "agent_screen_orientation";
    private static final String PREF_LAST_SELECTED_GAME = "last_selected_game";

    private RadioButton radioButton;
    private RadioButton zh;
    private RadioButton mainPortraitRadio;
    private RadioButton mainLandscapeRadio;
    private RadioButton agentPortraitRadio;
    private RadioButton agentLandscapeRadio;
    private Spinner gameSpinner;
    private String selectedGameUrl;
    private HashMap<String, String> gameUrls;
    private HashMap<String, String> cloudGameUrls;
    private LinearLayout contentLayout;
    private ScrollView scrollView;
    private Button refreshButton;
    private Button shareButton;
    private Button tabFileButton;
    private Button tabResourceButton;
    private LinearLayout fileContentLayout;
    private LinearLayout resourceContentLayout;
    private ListView resourceListView;
    private Button resourceRefreshButton;
    private ProgressBar resourceLoadingProgress;
    private TextView resourceLoadingText;
    private TextView resourceErrorText;
    private TextView resourceEmptyText;
    private List<ResourceItem> resourceItems;
    private ResourceAdapter resourceAdapter;
    private Button jumpButton;
    private Button fakeAppButton;
    private Button hangupModeButton;
    private HashMap<String, String> fakeAppMap;
    private HashMap<String, String> cloudFakeAppMap;
    private String selectedFakeAppId = null;
    private String selectedFakeAppName = null;
    private HashMap<String, android.graphics.Bitmap> imageCache = new HashMap<String, android.graphics.Bitmap>();
    private List<String> fileLines = new ArrayList<String>();
    private HashMap<Integer, String> notesMap = new HashMap<Integer, String>();

    private CheckBox autoLoginCheckbox;
    private boolean isAutoLoginEnabled = false;

    public static class ResourceItem {
        public String iconUrl;
        public String name;
        public String description;
        public String link;

        public ResourceItem(String iconUrl, String name, String description, String link) {
            this.iconUrl = iconUrl;
            this.name = name;
            this.description = description;
            this.link = link;
        }
    }

    public class ResourceAdapter extends ArrayAdapter<ResourceItem> {

        public ResourceAdapter(Context context, List<ResourceItem> items) {
            super(context, 0, items);
        }

        @Override
        public View getView(int position, View convertView, android.view.ViewGroup parent) {
            ViewHolder holder;

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_resource, parent, false);
                holder = new ViewHolder();
                holder.iconImage = (ImageView) convertView.findViewById(R.id.icon_image);
                holder.nameText = (TextView) convertView.findViewById(R.id.name_text);
                holder.descText = (TextView) convertView.findViewById(R.id.desc_text);
                holder.linkText = (TextView) convertView.findViewById(R.id.link_text);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            ResourceItem item = getItem(position);

            if (holder.nameText != null) holder.nameText.setText(item.name != null ? item.name : "未知名称");
            if (holder.descText != null) holder.descText.setText(item.description != null ? item.description : "无介绍");
            if (holder.linkText != null) holder.linkText.setText(item.link != null ? item.link : "无链接");

            if (holder.iconImage != null) {
                if (item.iconUrl != null && !item.iconUrl.isEmpty() && item.iconUrl.startsWith("http")) {
                    new LoadImageTask(holder.iconImage).execute(item.iconUrl);
                } else {
                    holder.iconImage.setImageResource(R.drawable.ic_launcher);
                }
            }

            return convertView;
        }

        private class ViewHolder {
            ImageView iconImage;
            TextView nameText;
            TextView descText;
            TextView linkText;
        }
    }

    private class LoadImageTask extends AsyncTask<String, Void, android.graphics.Bitmap> {
        private ImageView imageView;
        private String imageUrl;

        public LoadImageTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected android.graphics.Bitmap doInBackground(String... urls) {
            if (urls.length == 0 || urls[0] == null) {
                return null;
            }

            imageUrl = urls[0];

            if (imageCache.containsKey(imageUrl)) {
                return imageCache.get(imageUrl);
            }

            try {
                URL url = new URL(imageUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    InputStream input = connection.getInputStream();
                    android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(input);
                    input.close();

                    if (bitmap != null) {
                        imageCache.put(imageUrl, bitmap);
                    }

                    return bitmap;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(android.graphics.Bitmap result) {
            if (result != null && imageView != null) {
                imageView.setImageBitmap(result);
            } else if (imageView != null) {
                imageView.setImageResource(R.drawable.ic_launcher);
            }
        }
    }

    private static class TrustAllCerts implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }

    private SSLContext createTrustAllSSLContext() {
        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, new TrustManager[]{new TrustAllCerts()}, new java.security.SecureRandom());
            return sc;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private javax.net.ssl.HostnameVerifier createTrustAllHostnameVerifier() {
        return new javax.net.ssl.HostnameVerifier() {
            @Override
            public boolean verify(String hostname, javax.net.ssl.SSLSession session) {
                return true;
            }
        };
    }

    private String makeHttpRequest(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();

            if (connection instanceof HttpsURLConnection) {
                HttpsURLConnection httpsConnection = (HttpsURLConnection) connection;
                SSLContext sslContext = createTrustAllSSLContext();
                if (sslContext != null) {
                    httpsConnection.setSSLSocketFactory(sslContext.getSocketFactory());
                    httpsConnection.setHostnameVerifier(createTrustAllHostnameVerifier());
                }
            }

            connection.setRequestMethod("GET");
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setDoInput(true);

            connection.setRequestProperty("User-Agent", "Mozilla/5.0 ( compatible ) ");
            connection.setRequestProperty("Accept", "*/*");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream inputStream = connection.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line).append("\n");
                }
                reader.close();
                return response.toString().trim();
            } else {
                Log.e("HTTP", "HTTP error code: " + responseCode + " for URL: " + urlString);
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("HTTP", "Request failed for URL: " + urlString, e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    // ================== 云端公告弹窗及强制更新 ==================
    private void checkCloudPopup() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(CLOUD_POPUP_URL);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(8000);
                    conn.setReadTimeout(8000);
                    conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android)");
                    int code = conn.getResponseCode();
                    if (code == 200) {
                        InputStream is = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                        StringBuilder sb = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) sb.append(line);
                        reader.close();
                        is.close();
                        conn.disconnect();

                        JSONObject root = new JSONObject(sb.toString());
                        int resultCode = root.optInt("code", 0);
                        if (resultCode == 1) {
                            final JSONArray popups = root.optJSONArray("popups");
if (popups != null && popups.length() > 0) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        showPopupQueue(popups);
                                    }
                                });
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showPopupQueue(JSONArray popups) {
        if (popups == null || popups.length() == 0) return;
        showNextPopup(popups, 0);
    }

    private void showNextPopup(final JSONArray popups, final int index) {
        if (index >= popups.length()) return;
        try {
final JSONObject pop = popups.getJSONObject(index);
final String title = pop.optString("title", "公告");
final String content = pop.optString("content", "");
final String imageUrl = pop.optString("image_url", "");
// 从 extra 对象中获取下载地址
JSONObject extra = pop.optJSONObject("extra");
String tempLinkUrl = "";
if (extra != null) {
    tempLinkUrl = extra.optString("download_url", "");
}
final String linkUrl = tempLinkUrl;   // 关键：声明为 final
final String type = pop.optString("type", "text");
final int duration = pop.optInt("duration", 0);
// 根据 force 字段判断是否强制更新
boolean forceUpdate = pop.optBoolean("force", false);
// 兼容旧格式（可选）
if (!forceUpdate) {
    forceUpdate = "force_update".equals(type) || "更新".equals(type);
}

            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(title);

            if (forceUpdate) {
                builder.setMessage(content);
                builder.setPositiveButton("立即更新", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!TextUtils.isEmpty(linkUrl)) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                            startActivity(intent);
                        }
                        android.os.Process.killProcess(android.os.Process.myPid());
                    }
                });
                AlertDialog dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                return;
            }

            if (type.equals("image") && !TextUtils.isEmpty(imageUrl)) {
                final ImageView imageView = new ImageView(this);
                imageView.setAdjustViewBounds(true);
                imageView.setMaxWidth(400);
                imageView.setMaxHeight(400);
                new PopupImageLoadTask(imageView, new Runnable() {
                    @Override
                    public void run() {
                        builder.setView(imageView);
                        final AlertDialog dialog = builder.create();
                        dialog.setCancelable(false);
                        dialog.show();
                        if (duration > 0) {
                            new android.os.Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                    showNextPopup(popups, index + 1);
                                }
                            }, duration);
                        } else {
                            dialog.setButton(AlertDialog.BUTTON_POSITIVE, "关闭", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface d, int w) {
                                    d.dismiss();
                                    showNextPopup(popups, index + 1);
                                }
                            });
                            if (!TextUtils.isEmpty(linkUrl)) {
                                dialog.setButton(AlertDialog.BUTTON_NEUTRAL, "查看详情", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface d, int w) {
                                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                                        startActivity(intent);
                                        d.dismiss();
                                        showNextPopup(popups, index + 1);
                                    }
                                });
                            }
                        }
                    }
                }).execute(imageUrl);
                return;
            }

            builder.setMessage(content);
            if (duration > 0) {
                final AlertDialog dialog = builder.create();
                dialog.setCancelable(false);
                dialog.show();
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        showNextPopup(popups, index + 1);
                    }
                }, duration);
            } else {
                builder.setPositiveButton("关闭", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        d.dismiss();
                        showNextPopup(popups, index + 1);
                    }
                });
                if (!TextUtils.isEmpty(linkUrl)) {
                    builder.setNeutralButton("查看详情", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(linkUrl));
                            startActivity(intent);
                            d.dismiss();
                            showNextPopup(popups, index + 1);
                        }
                    });
                }
                builder.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            showNextPopup(popups, index + 1);
        }
    }

    
    private class PopupImageLoadTask extends AsyncTask<String, Void, android.graphics.Bitmap> {
        private ImageView imageView;
        private Runnable onComplete;
        PopupImageLoadTask(ImageView iv, Runnable onComplete) {
            this.imageView = iv;
            this.onComplete = onComplete;
        }
        @Override
        protected android.graphics.Bitmap doInBackground(String... urls) {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000);
                conn.setDoInput(true);
                conn.connect();
                InputStream is = conn.getInputStream();
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                is.close();
                conn.disconnect();
                return bmp;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        @Override
        protected void onPostExecute(android.graphics.Bitmap bmp) {
            if (bmp != null) imageView.setImageBitmap(bmp);
            if (onComplete != null) onComplete.run();
        }
    }

    // ================== 原有方法 ==================
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(lp);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        setContentView(R.layout.activity_main);

        handleSharedFile(getIntent());

        initBasicViews();
        initTabViews();
        initResourceList();

        loadFileContent();

        new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    loadResourceList();
                }
            }, 1000);

        checkCloudPopup();
    }

    // 处理分享的文件
    private void handleSharedFile(Intent intent) {
        String action = intent.getAction();
        String type = intent.getType();

        Log.d("ShareReceiver", "Action: " + action + ", Type: " + type);

        if (Intent.ACTION_SEND.equals(action)) {
            Uri fileUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (fileUri != null) {
                Log.d("ShareReceiver", "Received single file: " + fileUri.toString());
                processSharedFile(fileUri);
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(action)) {
            ArrayList<Uri> fileUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (fileUris != null && !fileUris.isEmpty()) {
                Log.d("ShareReceiver", "Received multiple files: " + fileUris.size());
                for (Uri fileUri : fileUris) {
                    processSharedFile(fileUri);
                }
            }
        } else if (Intent.ACTION_VIEW.equals(action)) {
            Uri fileUri = intent.getData();
            if (fileUri != null) {
                Log.d("ShareReceiver", "Received view action: " + fileUri.toString());
                processSharedFile(fileUri);
            }
        }
    }

    // 处理分享的文件
    private void processSharedFile(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);
            String mimeType = getContentResolver().getType(fileUri);

            Log.d("ShareReceiver", "File: " + fileName + ", MIME: " + mimeType);

            if (isJsonFile(fileName, mimeType)) {
                showFileImportDialog(fileUri, fileName);
            } else {
                Toast.makeText(this, "只支持JSON文件格式", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "文件处理失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    // 获取文件名
    private String getFileName(Uri uri) {
        String fileName = null;

        if (uri.getScheme().equals("file")) {
            fileName = new File(uri.getPath()).getName();
        } else if (uri.getScheme().equals("content")) {
            Cursor cursor = null;
            try {
                cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) cursor.close();
            }
        }

        return fileName != null ? fileName : "unknown_file_" + System.currentTimeMillis();
    }

    // 检查是否是JSON文件
    private boolean isJsonFile(String fileName, String mimeType) {
        if (fileName != null && fileName.toLowerCase().endsWith(".json")) {
            return true;
        }
        if (mimeType != null && (mimeType.equals("application/json") || mimeType.equals("text/json"))) {
            return true;
        }
        return false;
    }

    // 显示文件导入对话框
    private void showFileImportDialog(final Uri sourceUri, final String fileName) {
        File backupDir = new File(BACKUP_DIR_PATH);
        if (!backupDir.exists()) {
            backupDir.mkdirs();
        }

        final File targetFile = new File(BACKUP_DIR_PATH + fileName);

        String message;
        if (targetFile.exists()) {
            message = "文件 \"" + fileName + "\" 已存在，您想要：";
        } else {
            message = "确定要导入文件 \"" + fileName + "\" 吗？";
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("导入JSON文件")
            .setMessage(message);

        if (targetFile.exists()) {
            builder.setPositiveButton("替换", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copyFileToBackup(sourceUri, targetFile, true);
                    }
                })
                .setNeutralButton("两个都保存", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                        String extension = fileName.substring(fileName.lastIndexOf('.'));
                        String newFileName = baseName + "_" + System.currentTimeMillis() + extension;
                        File newTargetFile = new File(BACKUP_DIR_PATH + newFileName);
                        copyFileToBackup(sourceUri, newTargetFile, false);
                    }
                });
        } else {
            builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        copyFileToBackup(sourceUri, targetFile, false);
                    }
                });
        }

        builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Toast.makeText(MainActivity.this, "已取消导入", Toast.LENGTH_SHORT).show();
                }
            })
            .setCancelable(false)
            .show();
    }

    // 复制文件到备份目录
    private void copyFileToBackup(Uri sourceUri, File targetFile, boolean isReplace) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(sourceUri);
            if (inputStream == null) {
                Toast.makeText(this, "无法读取源文件", Toast.LENGTH_SHORT).show();
                return;
            }

            FileOutputStream outputStream = new FileOutputStream(targetFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            String message = isReplace ? 
                "文件替换成功: " + targetFile.getName() : 
                "文件导入成功: " + targetFile.getName();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();

            loadFileContent();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "文件操作失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleSharedFile(intent);
    }

    // 初始化基础视图
    private void initBasicViews() {
        mainPortraitRadio = (RadioButton) findViewById(R.id.main_portrait_radio);
        mainLandscapeRadio = (RadioButton) findViewById(R.id.main_landscape_radio);
        agentPortraitRadio = (RadioButton) findViewById(R.id.agent_portrait_radio);
        agentLandscapeRadio = (RadioButton) findViewById(R.id.agent_landscape_radio);

        jumpButton = (Button) findViewById(R.id.jump_button);
        jumpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    startAgentActivity();
                }
            });

        // 初始化过滤器按钮
        Button filterButton = (Button) findViewById(R.id.btn_open_filter);
        if (filterButton != null) {
            filterButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this, FilterActivity.class);
                        startActivity(intent);
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    }
                });
            Log.d("MainActivity", "过滤器按钮初始化成功");
        } else {
            Log.e("MainActivity", "过滤器按钮未找到！");
        }

        final SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        final String mainOrientation = prefs.getString(PREF_MAIN_SCREEN_ORIENTATION, SCREEN_ORIENTATION_LANDSCAPE);
        final String agentOrientation = prefs.getString(PREF_AGENT_SCREEN_ORIENTATION, SCREEN_ORIENTATION_PORTRAIT);
        updateScreenOrientationUI(mainOrientation, agentOrientation);

        initScreenOrientation();
        initCloudGameSpinner();
        initFakeAppButton();
        initHangupMode();
        initAutoLogin();
        initFileContentDisplay();

        zh = (RadioButton) findViewById(R.id.zh);
        radioButton = (RadioButton) findViewById(R.id.sh);

        mainPortraitRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setMainScreenOrientation(SCREEN_ORIENTATION_PORTRAIT);
                }
            });

        mainLandscapeRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setMainScreenOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                }
            });

        agentPortraitRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAgentScreenOrientation(SCREEN_ORIENTATION_PORTRAIT);
                }
            });

        agentLandscapeRadio.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setAgentScreenOrientation(SCREEN_ORIENTATION_LANDSCAPE);
                }
            });

        zh.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setZhMode();
                }
            });

        radioButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setShMode();
                }                                                    
            });

        updateModeState();

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                                   android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                   android.Manifest.permission.READ_EXTERNAL_STORAGE},
                               REQUEST_CODE_STORAGE_PERMISSIONS);
        }
    }

    // 初始化自动登录功能
    private void initAutoLogin() {
        autoLoginCheckbox = (CheckBox) findViewById(R.id.auto_login_checkbox);
        
        loadAutoLoginSetting();
        
        autoLoginCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
                String currentMode = sharedPreferences.getString("pd_key", "2");
                
                if ("1".equals(currentMode)) {
                    isAutoLoginEnabled = autoLoginCheckbox.isChecked();
                    saveAutoLoginSetting();
                } else {
                    autoLoginCheckbox.setChecked(false);
                }
            }
        });
        
        updateAutoLoginCheckbox();
    }

    private void updateAutoLoginCheckbox() {
        if (autoLoginCheckbox == null) return;
        
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String currentMode = sharedPreferences.getString("pd_key", "2");
        
        if ("1".equals(currentMode)) {
            autoLoginCheckbox.setEnabled(true);
            autoLoginCheckbox.setAlpha(1.0f);
            autoLoginCheckbox.setChecked(isAutoLoginEnabled);
        } else {
            autoLoginCheckbox.setEnabled(false);
            autoLoginCheckbox.setAlpha(0.5f);
            autoLoginCheckbox.setChecked(false);
        }
    }

    private void saveAutoLoginSetting() {
        try {
            File autoLoginFile = new File(AUTO_LOGIN_FILE_PATH);
            FileWriter writer = new FileWriter(autoLoginFile);
            
            if (isAutoLoginEnabled) {
                writer.write("1");
            } else {
                if (autoLoginFile.exists()) {
                    autoLoginFile.delete();
                }
            }
            
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "保存自动登录设置失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadAutoLoginSetting() {
        File autoLoginFile = new File(AUTO_LOGIN_FILE_PATH);
        if (autoLoginFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(autoLoginFile));
                String content = reader.readLine();
                reader.close();
                
                isAutoLoginEnabled = "1".equals(content);
            } catch (IOException e) {
                e.printStackTrace();
                isAutoLoginEnabled = false;
            }
        } else {
            isAutoLoginEnabled = false;
        }
    }

    private void setShMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pd_key", "1");
        editor.apply();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        updateModeState();
        updateAutoLoginCheckbox();
    }

    private void setZhMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("pd_key", "2");
        editor.apply();

        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        updateModeState();
        updateAutoLoginCheckbox();
    }

    private void setHangupMode() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String currentMode = sharedPreferences.getString("pd_key", "2");

        if ("3".equals(currentMode)) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pd_key", "2");
            editor.apply();

            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            updateModeState();
            Toast.makeText(MainActivity.this, "已关闭挂机模式", Toast.LENGTH_SHORT).show();
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("pd_key", "3");
            editor.apply();

            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            updateModeState();
            Toast.makeText(MainActivity.this, "挂机模式已开启 - 手机将保持唤醒", Toast.LENGTH_SHORT).show();
        }

        updateHangupModeButton();
    }

    private void updateModeState() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String content = sharedPreferences.getString("pd_key", "2");

        radioButton.setChecked("1".equals(content));
        zh.setChecked("2".equals(content));

        updateHangupModeButton();
        updateAutoLoginCheckbox();

        if ("3".equals(content)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    private void updateHangupModeButton() {
        if (hangupModeButton == null) return;

        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String currentMode = sharedPreferences.getString("pd_key", "2");

        if ("3".equals(currentMode)) {
            hangupModeButton.setText("关闭挂机");
            hangupModeButton.setTextColor(0xFFFFFFFF);
            hangupModeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700));
        } else {
            hangupModeButton.setText("挂机模式");
            hangupModeButton.setTextColor(0xFFFFFFFF);
            hangupModeButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700));
        }
    }

    private void initHangupMode() {
        hangupModeButton = (Button) findViewById(R.id.hangup_mode_button);

        updateHangupModeButton();

        hangupModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setHangupMode();
            }
        });
    }

    private void initCloudGameSpinner() {
        gameSpinner = (Spinner) findViewById(R.id.game_spinner);
        cloudGameUrls = new HashMap<String, String>();

        List<String> defaultGameList = Arrays.asList("王者", "火影", "CF", "和平");
        ArrayAdapter<String> defaultAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, defaultGameList);
        gameSpinner.setAdapter(defaultAdapter);

        setupDefaultGames();

        new LoadCloudGameTask().execute();
    }

    private class LoadCloudGameTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return makeHttpRequest(GAME_API_URL);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                try {
                    parseCloudGames(result);
                } catch (Exception e) {
                    e.printStackTrace();
                    setupDefaultGames();
                }
            } else {
                setupDefaultGames();
            }
        }
    }

    private void parseCloudGames(String content) {
        try {
            String decodedContent = decodeUnicode(content);
            cloudGameUrls.clear();

            String[] lines = decodedContent.split("\n");
            List<String> gameNames = new ArrayList<String>();

            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;

                String name = extractGameValue(line, "名称：", "；");
                String link = extractGameValue(line, "〖链接", "〗");

                if (name == null) {
                    name = extractGameValue(line, "名称:", "；");
                }
                if (link == null) {
                    link = extractGameValue(line, "链接:", "；");
                }

                if (name != null && !name.isEmpty() && link != null && !link.isEmpty()) {
                    cloudGameUrls.put(name, link);
                    gameNames.add(name);
                }
            }

            if (!gameNames.isEmpty()) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
                                                                        android.R.layout.simple_spinner_item, gameNames);
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                gameSpinner.setAdapter(adapter);

                gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        String selectedGame = (String) parent.getItemAtPosition(position);
                        selectedGameUrl = cloudGameUrls.get(selectedGame);

                        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putString(PREF_LAST_SELECTED_GAME, selectedGame);
                        editor.apply();

                        Log.d("CloudGame", "已选择游戏: " + selectedGame);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                    }
                });

                if (!gameNames.isEmpty()) {
                    selectedGameUrl = cloudGameUrls.get(gameNames.get(0));
                }
            } else {
                setupDefaultGames();
            }

        } catch (Exception e) {
            e.printStackTrace();
            setupDefaultGames();
        }
    }

    private String extractGameValue(String line, String start, String end) {
        try {
            int startIndex = line.indexOf(start);
            if (startIndex == -1) return null;
            startIndex += start.length();

            int endIndex = line.indexOf(end, startIndex);
            if (endIndex == -1) return null;

            return line.substring(startIndex, endIndex).trim();
        } catch (Exception e) {
            return null;
        }
    }

    private void setupDefaultGames() {
        gameUrls = new HashMap<String, String>();
        gameUrls.put("王者", "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_enable_pwd=1&appid=716027609&pt_3rd_aid=1104466820&daid=381&pt_skey_valid=0&style=35&force_qr=1&autorefresh=1&s_url=http://connect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=0&redirect_uri=auth://tauth.qq.com/&client_id=1104466820&pf=openmobile_android&response_type=token&scope=all&sdkp=a&sdkv=3.5.14&sign=0&status_machine=0&switch=1&time=0&loginfrom=add&h5sig=0&loginty=6");
        gameUrls.put("火影", "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_enable_pwd=1&appid=716027609&pt_3rd_aid=1104307008&daid=381&pt_skey_valid=0&style=35&force_qr=1&autorefresh=1&s_url=http://connect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=0&redirect_uri=auth://tauth.qq.com/&client_id=1104307008&pf=openmobile_android&response_type=token&scope=all&sdkp=a&sdkv=3.5.14&sign=0&status_machine=0&switch=1&time=0&loginfrom=add&h5sig=0&loginty=6");
        gameUrls.put("CF", "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_enable_pwd=1&appid=716027609&pt_3rd_aid=1104512706&daid=381&pt_skey_valid=0&style=35&force_qr=1&autorefresh=1&s_url=http://connect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=0&redirect_uri=auth://tauth.qq.com/&client_id=1104512706&pf=openmobile_android&response_type=token&scope=all&sdkp=a&sdkv=3.5.14&sign=0&status_machine=0&switch=1&time=0&loginfrom=add&h5sig=0&loginty=6");
        gameUrls.put("和平", "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_enable_pwd=1&appid=716027609&pt_3rd_aid=1106467070&daid=381&pt_skey_valid=0&style=35&force_qr=1&autorefresh=1&s_url=http://connect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=0&redirect_uri=auth://tauth.qq.com/&client_id=1106467070&pf=openmobile_android&response_type=token&scope=all&sdkp=a&sdkv=3.5.14&sign=0&status_machine=0&switch=1&time=0&loginfrom=add&h5sig=0&loginty=6");

        List<String> gameList = new ArrayList<String>(gameUrls.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, gameList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        gameSpinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String lastSelectedGame = prefs.getString(PREF_LAST_SELECTED_GAME, null);

        int selectionPosition = 0;
        if (lastSelectedGame != null && gameList.contains(lastSelectedGame)) {
            selectionPosition = gameList.indexOf(lastSelectedGame);
        }

        gameSpinner.setSelection(selectionPosition);
        selectedGameUrl = gameUrls.get(gameList.get(selectionPosition));

        gameSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    String selectedGame = (String) parent.getItemAtPosition(position);
                    selectedGameUrl = gameUrls.get(selectedGame);

                    SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(PREF_LAST_SELECTED_GAME, selectedGame);
                    editor.apply();

                    Log.d("GameSelection", "已选择游戏: " + selectedGame + ", URL: " + selectedGameUrl);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
    }

    private void initFakeAppButton() {
        fakeAppButton = (Button) findViewById(R.id.fake_app_button);
        cloudFakeAppMap = new HashMap<String, String>();

        setupDefaultFakeApps();

        new LoadCloudFakeAppTask().execute();

        fakeAppButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showFakeAppDialog();
            }
        });
    }

    private class LoadCloudFakeAppTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... voids) {
            return makeHttpRequest(FAKE_APP_API_URL);
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                try {
                    parseCloudFakeApps(result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void parseCloudFakeApps(String content) {
        try {
            String decodedContent = decodeUnicode(content);
            cloudFakeAppMap.clear();

            Log.d("FakeApp", "原始内容: " + decodedContent);

            int listStart = decodedContent.indexOf("列表:");
            if (listStart == -1) {
                Log.e("FakeApp", "未找到列表开始标记");
                return;
            }

            int listEnd = decodedContent.indexOf(":", listStart + 3);
            if (listEnd == -1) {
                Log.e("FakeApp", "未找到列表结束标记");
                return;
            }

            String listStr = decodedContent.substring(listStart + 3, listEnd).trim();
            Log.d("FakeApp", "列表字符串: " + listStr);

            String[] appNames = listStr.split("℉");
            Log.d("FakeApp", "解析出的应用名称数量: " + appNames.length);

            String idSection = decodedContent.substring(listEnd + 1).trim();
            Log.d("FakeApp", "ID部分: " + idSection);

            for (String appName : appNames) {
                String trimmedName = appName.trim();
                if (!trimmedName.isEmpty()) {
                    String appId = extractFakeAppId(idSection, trimmedName);
                    if (appId != null) {
                        cloudFakeAppMap.put(trimmedName, appId);
                        Log.d("FakeApp", "找到应用: " + trimmedName + " -> " + appId);
                    } else {
                        Log.d("FakeApp", "未找到应用ID: " + trimmedName);
                    }
                }
            }

            Log.d("FakeApp", "最终解析出的应用数量: " + cloudFakeAppMap.size());

            if (!cloudFakeAppMap.isEmpty()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        fakeAppButton.setText("伪应用(" + cloudFakeAppMap.size() + ")");
                        Toast.makeText(MainActivity.this, "伪应用列表已更新", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FakeApp", "解析伪应用列表失败: " + e.getMessage());
            
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    setupDefaultFakeApps();
                    fakeAppButton.setText("伪应用(默认)");
                }
            });
        }
    }

    private String extractFakeAppId(String idSection, String appName) {
        try {
            String trimmedAppName = appName.trim();
            
            String[] searchPatterns = {
                trimmedAppName + ".",
                trimmedAppName + " .",
                " " + trimmedAppName + ".",
                trimmedAppName + ":",
                trimmedAppName + " :"
            };
            
            int nameIndex = -1;
            String usedPattern = "";
            
            for (String pattern : searchPatterns) {
                nameIndex = idSection.indexOf(pattern);
                if (nameIndex != -1) {
                    usedPattern = pattern;
                    break;
                }
            }
            
            if (nameIndex == -1) {
                Log.d("FakeApp", "未找到应用: " + trimmedAppName);
                return null;
            }

            int idStart = nameIndex + usedPattern.length();
            
            int idEnd = -1;
            char[] endMarkers = {':', '.', '\n', '\r'};
            
            for (char marker : endMarkers) {
                int markerPos = idSection.indexOf(marker, idStart);
                if (markerPos != -1 && (idEnd == -1 || markerPos < idEnd)) {
                    idEnd = markerPos;
                }
            }
            
            if (idEnd == -1) {
                idEnd = idSection.length();
            }

            String appId = idSection.substring(idStart, idEnd).trim();
            
            appId = appId.replace("\n", "").replace("\r", "").trim();

            if (appId.matches("\\d+")) {
                Log.d("FakeApp", "成功解析: " + trimmedAppName + " -> " + appId);
                return appId;
            } else {
                Log.d("FakeApp", "ID格式无效: " + appId + " 对于应用: " + trimmedAppName);
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("FakeApp", "解析应用ID异常: " + appName + " - " + e.getMessage());
            return null;
        }
    }

    private void setupDefaultFakeApps() {
        fakeAppMap = new HashMap<String, String>();
        fakeAppMap.put("和平精英", "1106467070");
        fakeAppMap.put("和平营地", "1105412664");
        fakeAppMap.put("王者荣耀", "1104466820");
        fakeAppMap.put("PUBG", "1106545419");
        fakeAppMap.put("火影忍者", "1104307008");
        fakeAppMap.put("QQ音乐", "100497308");
        fakeAppMap.put("4399游戏盒", "100266617");
        fakeAppMap.put("拼多多", "1104790111");
        fakeAppMap.put("迅猛兔加速器", "1112330014");
        fakeAppMap.put("金铲铲", "1109811436");
        fakeAppMap.put("作业帮", "1101233570");
        fakeAppMap.put("象棋", "100880170");
        fakeAppMap.put("腾讯视频", "101795054");
        fakeAppMap.put("斗地主", "716027609");
        fakeAppMap.put("无畏契约", "1111677210");
    }

    private void showFakeAppDialog() {
        final HashMap<String, String> appMap = !cloudFakeAppMap.isEmpty() ? 
            cloudFakeAppMap : fakeAppMap;

        if (appMap.isEmpty()) {
            Toast.makeText(this, "没有可用的伪应用", Toast.LENGTH_SHORT).show();
            return;
        }

        final String[] appList = new String[appMap.size() + 1];
        int i = 0;
        for (String appName : appMap.keySet()) {
            appList[i++] = appName;
        }
        appList[i] = "关闭伪应用";

        final MainActivity activity = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(selectedFakeAppName != null ? 
                     "当前伪应用: " + selectedFakeAppName : 
                     "选择伪应用 (" + appMap.size() + "个)")
        .setItems(appList, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == appList.length - 1) {
                    selectedFakeAppId = null;
                    selectedFakeAppName = null;
                    fakeAppButton.setText("伪应用");
                    Toast.makeText(activity, "已关闭伪应用", Toast.LENGTH_SHORT).show();
                    return;
                }

                String selectedApp = appList[which];
                String appId = appMap.get(selectedApp);

                if (appId != null) {
                    selectedFakeAppId = appId;
                    selectedFakeAppName = selectedApp;
                    fakeAppButton.setText("伪:" + selectedApp);
                    showAppIdInfo(selectedApp, appId);
                } else {
                    Toast.makeText(activity, "未找到对应的应用ID", Toast.LENGTH_SHORT).show();
                }
            }
        })
        .setNegativeButton("取消", null)
        .show();
    }

    private void showAppIdInfo(String appName, String appId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("伪应用已设置")
            .setMessage("应用: " + appName + "\n" + "" + "\n\n仅供学习参考")
            .setPositiveButton("确定", null)
            .show();
    }

    private void initTabViews() {
        tabFileButton = (Button) findViewById(R.id.tab_file);
        tabResourceButton = (Button) findViewById(R.id.tab_resource);
        fileContentLayout = (LinearLayout) findViewById(R.id.file_content_layout);
        resourceContentLayout = (LinearLayout) findViewById(R.id.resource_content_layout);

        if (tabFileButton == null || tabResourceButton == null) {
            Toast.makeText(this, "Tab按钮初始化失败", Toast.LENGTH_SHORT).show();
            return;
        }

        tabFileButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToFileTab();
                }
            });

        tabResourceButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    switchToResourceTab();
                }
            });
    }

    private void switchToFileTab() {
        if (tabFileButton != null && tabResourceButton != null) {
            tabFileButton.setTextColor(0xFFFFFFFF);
            tabFileButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700));
            tabResourceButton.setTextColor(0xFFFFD700);
            tabResourceButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
        }

        if (fileContentLayout != null) fileContentLayout.setVisibility(View.VISIBLE);
        if (resourceContentLayout != null) resourceContentLayout.setVisibility(View.GONE);
    }

    private void switchToResourceTab() {
        if (tabFileButton != null && tabResourceButton != null) {
            tabFileButton.setTextColor(0xFFFFD700);
            tabFileButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFFFFF));
            tabResourceButton.setTextColor(0xFFFFFFFF);
            tabResourceButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFFD700));
        }

        if (fileContentLayout != null) fileContentLayout.setVisibility(View.GONE);
        if (resourceContentLayout != null) resourceContentLayout.setVisibility(View.VISIBLE);
    }

    private void initResourceList() {
        try {
            resourceListView = (ListView) findViewById(R.id.resource_list_view);
            resourceRefreshButton = (Button) findViewById(R.id.resource_refresh_button);
            resourceLoadingProgress = (ProgressBar) findViewById(R.id.resource_loading_progress);
            resourceLoadingText = (TextView) findViewById(R.id.resource_loading_text);
            resourceErrorText = (TextView) findViewById(R.id.resource_error_text);
            resourceEmptyText = (TextView) findViewById(R.id.resource_empty_text);

            resourceItems = new ArrayList<ResourceItem>();
            resourceAdapter = new ResourceAdapter(this, resourceItems);

            if (resourceListView != null) {
                resourceListView.setAdapter(resourceAdapter);
            }

            if (resourceRefreshButton != null) {
                resourceRefreshButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            loadResourceList();
                        }
                    });
            }

            if (resourceListView != null) {
                resourceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            if (resourceItems != null && position < resourceItems.size()) {
                                final ResourceItem item = resourceItems.get(position);
                                openLinkInBrowser(item);
                            }
                        }
                    });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "资源列表初始化失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void openLinkInBrowser(ResourceItem item) {
        if (item == null || item.link == null || item.link.isEmpty()) {
            Toast.makeText(this, "链接无效", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(item.link));
            startActivity(intent);
            Toast.makeText(this, "正在打开链接...", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadResourceList() {
        try {
            new LoadResourceTask().execute();
        } catch (Exception e) {
            e.printStackTrace();
            showErrorState("加载失败: " + e.getMessage());
        }
    }

    private class LoadResourceTask extends AsyncTask<Void, Void, String> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showLoadingState();
        }

        @Override
        protected String doInBackground(Void... voids) {
            return makeHttpRequest(RESOURCE_API_URL);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null && !result.isEmpty()) {
                try {
                    parseResourceContent(result);
                    showResourceListState();
                } catch (Exception e) {
                    e.printStackTrace();
                    showErrorState("解析失败");
                }
            } else {
                showErrorState("获取数据失败");
            }
        }
    }

    // 解析资源列表（已适配新接口JSON格式）
    private void parseResourceContent(String content) {
        if (resourceItems == null) {
            resourceItems = new ArrayList<>();
        }
        resourceItems.clear();

        try {
            JSONObject jsonRoot = new JSONObject(content);
            int code = jsonRoot.optInt("code", -1);
            if (code != 1) {
                String msg = jsonRoot.optString("msg", "未知错误");
                Log.e("Resource", "接口返回错误: " + msg);
                showErrorState("加载失败：" + msg);
                return;
            }

            JSONArray dataArray = jsonRoot.optJSONArray("data");
            if (dataArray == null || dataArray.length() == 0) {
                showEmptyState();
                return;
            }

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject itemObj = dataArray.getJSONObject(i);
                String icon = itemObj.optString("icon", "");
                String name = itemObj.optString("name", "");
                String description = itemObj.optString("description", "");
                String url = itemObj.optString("url", "");

                // 处理 icon 中的转义反斜杠 \/ 变成 /
                if (icon.contains("\\/")) {
                    icon = icon.replace("\\/", "/");
                }

                resourceItems.add(new ResourceItem(icon, name, description, url));
            }

            if (resourceAdapter != null) {
                resourceAdapter.notifyDataSetChanged();
            }
            showResourceListState();

        } catch (JSONException e) {
            e.printStackTrace();
            showErrorState("数据解析失败：" + e.getMessage());
        }
    }

    private void showEmptyState() {
        if (resourceLoadingProgress != null) resourceLoadingProgress.setVisibility(View.GONE);
        if (resourceLoadingText != null) resourceLoadingText.setVisibility(View.GONE);
        if (resourceErrorText != null) resourceErrorText.setVisibility(View.GONE);
        if (resourceEmptyText != null) resourceEmptyText.setVisibility(View.VISIBLE);
        if (resourceListView != null) resourceListView.setVisibility(View.GONE);
    }

    private void showLoadingState() {
        if (resourceLoadingProgress != null) resourceLoadingProgress.setVisibility(View.VISIBLE);
        if (resourceLoadingText != null) resourceLoadingText.setVisibility(View.VISIBLE);
        if (resourceErrorText != null) resourceErrorText.setVisibility(View.GONE);
        if (resourceEmptyText != null) resourceEmptyText.setVisibility(View.GONE);
        if (resourceListView != null) resourceListView.setVisibility(View.GONE);
    }

    private void showResourceListState() {
        if (resourceLoadingProgress != null) resourceLoadingProgress.setVisibility(View.GONE);
        if (resourceLoadingText != null) resourceLoadingText.setVisibility(View.GONE);
        if (resourceErrorText != null) resourceErrorText.setVisibility(View.GONE);

        if (resourceItems == null || resourceItems.isEmpty()) {
            if (resourceEmptyText != null) resourceEmptyText.setVisibility(View.VISIBLE);
            if (resourceListView != null) resourceListView.setVisibility(View.GONE);
        } else {
            if (resourceEmptyText != null) resourceEmptyText.setVisibility(View.GONE);
            if (resourceListView != null) resourceListView.setVisibility(View.VISIBLE);
        }
    }

    private void showErrorState(String errorMessage) {
        if (resourceLoadingProgress != null) resourceLoadingProgress.setVisibility(View.GONE);
        if (resourceLoadingText != null) resourceLoadingText.setVisibility(View.GONE);
        if (resourceErrorText != null) {
            resourceErrorText.setVisibility(View.VISIBLE);
            resourceErrorText.setText(errorMessage);
        }
        if (resourceEmptyText != null) resourceEmptyText.setVisibility(View.GONE);
        if (resourceListView != null) resourceListView.setVisibility(View.GONE);
    }

    private void initScreenOrientation() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);

        String mainOrientation = prefs.getString(PREF_MAIN_SCREEN_ORIENTATION, SCREEN_ORIENTATION_LANDSCAPE);
        String agentOrientation = prefs.getString(PREF_AGENT_SCREEN_ORIENTATION, SCREEN_ORIENTATION_PORTRAIT);

        applyMainScreenOrientation(mainOrientation);

        updateScreenOrientationUI(mainOrientation, agentOrientation);
    }

    private void updateScreenOrientationUI(String mainOrientation, String agentOrientation) {
        if (mainPortraitRadio != null && mainLandscapeRadio != null) {
            if (SCREEN_ORIENTATION_PORTRAIT.equals(mainOrientation)) {
                mainPortraitRadio.setChecked(true);
                mainLandscapeRadio.setChecked(false);
            } else {
                mainPortraitRadio.setChecked(false);
                mainLandscapeRadio.setChecked(true);
            }
        }

        if (agentPortraitRadio != null && agentLandscapeRadio != null) {
            if (SCREEN_ORIENTATION_PORTRAIT.equals(agentOrientation)) {
                agentPortraitRadio.setChecked(true);
                agentLandscapeRadio.setChecked(false);
            } else {
                agentPortraitRadio.setChecked(false);
                agentLandscapeRadio.setChecked(true);
            }
        }
    }

    private void initFileContentDisplay() {
        contentLayout = (LinearLayout) findViewById(R.id.content_layout);
        scrollView = (ScrollView) findViewById(R.id.scroll_view);
        refreshButton = (Button) findViewById(R.id.refresh_button);
        shareButton = (Button) findViewById(R.id.share_button);

        refreshButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    loadFileContent();
                }
            });

        shareButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    shareFileContent();
                }
            });
    }

    private void setMainScreenOrientation(String orientation) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_MAIN_SCREEN_ORIENTATION, orientation);
        editor.apply();

        applyMainScreenOrientation(orientation);

        if (SCREEN_ORIENTATION_PORTRAIT.equals(orientation)) {
            mainPortraitRadio.setChecked(true);
            mainLandscapeRadio.setChecked(false);
        } else {
            mainPortraitRadio.setChecked(false);
            mainLandscapeRadio.setChecked(true);
        }
    }

    private void setAgentScreenOrientation(String orientation) {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_AGENT_SCREEN_ORIENTATION, orientation);
        editor.apply();

        if (SCREEN_ORIENTATION_PORTRAIT.equals(orientation)) {
            agentPortraitRadio.setChecked(true);
            agentLandscapeRadio.setChecked(false);
        } else {
            agentPortraitRadio.setChecked(false);
            agentLandscapeRadio.setChecked(true);
        }
    }

    private void applyMainScreenOrientation(String orientation) {
        if (SCREEN_ORIENTATION_PORTRAIT.equals(orientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private void startAgentActivity() {
        Intent intent = new Intent(MainActivity.this, AgentActivity.class);
        intent.putExtra("WEBVIEW_URL", getSelectedGameUrl());

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String agentOrientation = prefs.getString(PREF_AGENT_SCREEN_ORIENTATION, SCREEN_ORIENTATION_PORTRAIT);
        intent.putExtra("SCREEN_ORIENTATION", agentOrientation);

        startActivity(intent);
    }

    private String getSelectedGameUrl() {
        String baseUrl = selectedGameUrl != null ? selectedGameUrl : 
            "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_enable_pwd=1&appid=716027609&pt_3rd_aid=1106467070&daid=381&pt_skey_valid=0&style=35&force_qr=1&autorefresh=1&s_url=http%3A%2F%2Fconnect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=13&redirect_uri=auth%3A%2F%2Ftauth.qq.com%2F&client_id=1106467070&pf=openmobile_android&response_type=token&scope=all&sdkp=a&sdkv=3.5.14&sign=DD2B8B16CCAEE72B390F070106F00F0E&status_machine=M2102J2SC&switch=1&time=1720462937&loginfrom=add&h5sig=08nxvF7WAloOfZfomAK9EVeia3NFMcB4U_J0I5Z7r6M&loginty=6";

        if (selectedFakeAppId != null) {
            return replaceAppIdInUrl(baseUrl, selectedFakeAppId);
        }

        return baseUrl;
    }

    private String replaceAppIdInUrl(String url, String newAppId) {
        try {
            String startMarker = "&pt_3rd_aid=";
            String endMarker = "&daid";

            int startIndex = url.indexOf(startMarker);
            if (startIndex == -1) {
                return url;
            }
            startIndex += startMarker.length();

            int endIndex = url.indexOf(endMarker, startIndex);
            if (endIndex == -1) {
                return url;
            }

            String originalId = url.substring(startIndex, endIndex);

            String newUrl = url.substring(0, startIndex) + newAppId + url.substring(endIndex);

            Log.d("URLReplace", "原始ID: " + originalId + ", 新ID: " + newAppId);
            Log.d("URLReplace", "新URL: " + newUrl);

            return newUrl;

        } catch (Exception e) {
            e.printStackTrace();
            return url;
        }
    }

    private void loadFileContent() {
        File file = new File(FILE_PATH);
        fileLines.clear();
        if (contentLayout != null) {
            contentLayout.removeAllViews();
        }

        loadNotes();

        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                String line;
                int lineNumber = 1;

                while ((line = reader.readLine()) != null) {
                    fileLines.add(line);

                    LinearLayout lineLayout = new LinearLayout(this);
                    lineLayout.setOrientation(LinearLayout.VERTICAL);
                    lineLayout.setPadding(12, 8, 12, 8);
                    lineLayout.setBackgroundResource(android.R.drawable.list_selector_background);
                    lineLayout.setClickable(true);

                    LinearLayout contentRow = new LinearLayout(this);
                    contentRow.setOrientation(LinearLayout.HORIZONTAL);

                    TextView numberTextView = new TextView(this);
                    numberTextView.setText(lineNumber + ".");
                    numberTextView.setTextSize(12);
                    numberTextView.setTextColor(0xFF666666);
                    numberTextView.setPadding(0, 0, 8, 0);
                    numberTextView.setMinWidth(30);

                    TextView contentTextView = new TextView(this);
                    contentTextView.setText(line);
                    contentTextView.setTextSize(12);
                    contentTextView.setTextColor(0xFF333333);
                    contentTextView.setLayoutParams(new LinearLayout.LayoutParams(
                                                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                    contentTextView.setSingleLine(true);
                    contentTextView.setEllipsize(TextUtils.TruncateAt.END);

                    contentRow.addView(numberTextView);
                    contentRow.addView(contentTextView);

                    String note = notesMap.get(lineNumber);
                    if (note != null && !note.isEmpty()) {
                        TextView noteTextView = new TextView(this);
                        noteTextView.setText("📝 " + note);
                        noteTextView.setTextSize(10);
                        noteTextView.setTextColor(0xFF888888);
                        noteTextView.setPadding(30, 2, 0, 0);
                        noteTextView.setBackgroundColor(0xFFF8F8F8);
                        lineLayout.addView(noteTextView);
                    }

                    lineLayout.addView(contentRow);

                    final int currentLineNumber = lineNumber;
                    final String currentLine = line;

                    lineLayout.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showOperationDialog(currentLineNumber, currentLine);
                            }
                        });

                    if (contentLayout != null) {
                        contentLayout.addView(lineLayout);
                    }

                    View separator = new View(this);
                    separator.setBackgroundColor(0xFFE0E0E0);
                    separator.setLayoutParams(new LinearLayout.LayoutParams(
                                                  LinearLayout.LayoutParams.MATCH_PARENT, 1));
                    if (contentLayout != null) {
                        contentLayout.addView(separator);
                    }

                    lineNumber++;
                }
                reader.close();

            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "读取文件失败", Toast.LENGTH_SHORT).show();
            }
        } else {
            TextView emptyView = new TextView(this);
            emptyView.setText("暂无数据文件\n文件路径: " + FILE_PATH);
            emptyView.setTextSize(12);
            emptyView.setTextColor(0xFF999999);
            emptyView.setPadding(8, 16, 8, 16);
            emptyView.setGravity(View.TEXT_ALIGNMENT_CENTER);
            if (contentLayout != null) {
                contentLayout.addView(emptyView);
            }
        }
    }

    private void showOperationDialog(final int lineNumber, final String lineContent) {
        String currentNote = notesMap.get(lineNumber);
        String noteStatus = (currentNote != null && !currentNote.isEmpty()) ? 
            "📝 查看/修改备注" : "➕ 添加备注";

        String[] options = {
            "📤 分享此行",
            "🗑️ 删除此行", 
            noteStatus,
            "✏️ 重新输入内容",
            "📋 复制内容"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("操作选项 - 第" + lineNumber + "行")
            .setItems(options, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case 0:
                            shareSingleLine(lineNumber, lineContent);
                            break;
                        case 1:
                            showDeleteConfirmDialog(lineNumber, lineContent);
                            break;
                        case 2:
                            showNoteDialog(lineNumber, lineContent);
                            break;
                        case 3:
                            showReinputDialog(lineNumber, lineContent);
                            break;
                        case 4:
                            copyLineContent(lineNumber, lineContent);
                            break;
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void shareSingleLine(int lineNumber, String lineContent) {
        String note = notesMap.get(lineNumber);
        StringBuilder shareText = new StringBuilder();
        shareText.append("第").append(lineNumber).append("行数据\n");
        shareText.append("内容: ").append(lineContent);
        if (note != null && !note.isEmpty()) {
            shareText.append("\n备注: ").append(note);
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "第" + lineNumber + "行数据");

        try {
            startActivity(Intent.createChooser(shareIntent, "分享此行数据"));
        } catch (Exception e) {
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void copyLineContent(int lineNumber, String lineContent) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("line_data", lineContent);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "已复制第" + lineNumber + "行内容", Toast.LENGTH_SHORT).show();
    }

    private void showNoteDialog(final int lineNumber, final String lineContent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("备注管理 - 第" + lineNumber + "行");

        final EditText input = new EditText(this);
        String existingNote = notesMap.get(lineNumber);
        if (existingNote != null) {
            input.setText(existingNote);
        }
        input.setHint("请输入备注内容");
        input.setSelection(input.getText().length());

        builder.setView(input)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String note = input.getText().toString().trim();
                    saveNote(lineNumber, note);
                }
            })
            .setNeutralButton("删除备注", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteNote(lineNumber);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showReinputDialog(final int lineNumber, final String lineContent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重新输入内容 - 第" + lineNumber + "行");

        final EditText input = new EditText(this);
        input.setText(lineContent);
        input.setSelection(input.getText().length());

        builder.setView(input)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newContent = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(newContent)) {
                        updateLineContent(lineNumber, newContent);
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void updateLineContent(int lineNumber, String newContent) {
        try {
            List<String> lines = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
            String line;
            int currentLine = 1;

            while ((line = reader.readLine()) != null) {
                if (currentLine == lineNumber) {
                    lines.add(newContent);
                } else {
                    lines.add(line);
                }
                currentLine++;
            }
            reader.close();

            FileWriter writer = new FileWriter(FILE_PATH);
            for (String savedLine : lines) {
                writer.write(savedLine + "\n");
            }
            writer.close();

            Toast.makeText(this, "已更新第" + lineNumber + "行内容", Toast.LENGTH_SHORT).show();

            loadFileContent();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "更新失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveNote(int lineNumber, String note) {
        notesMap.put(lineNumber, note);
        saveNotesToFile();
        Toast.makeText(this, "已保存第" + lineNumber + "行备注", Toast.LENGTH_SHORT).show();
        loadFileContent();
    }

    private void deleteNote(int lineNumber) {
        if (notesMap.containsKey(lineNumber)) {
            notesMap.remove(lineNumber);
            saveNotesToFile();
            Toast.makeText(this, "已删除第" + lineNumber + "行备注", Toast.LENGTH_SHORT).show();
            loadFileContent();
        } else {
            Toast.makeText(this, "该行没有备注", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadNotes() {
        notesMap.clear();
        File notesFile = new File(NOTES_FILE_PATH);
        if (notesFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(notesFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        int lineNum = Integer.parseInt(parts[0].trim());
                        String note = parts[1].trim();
                        notesMap.put(lineNum, note);
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveNotesToFile() {
        try {
            FileWriter writer = new FileWriter(NOTES_FILE_PATH);
            for (Integer lineNumber : notesMap.keySet()) {
                String note = notesMap.get(lineNumber);
                if (note != null && !note.isEmpty()) {
                    writer.write(lineNumber + ":" + note + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showDeleteConfirmDialog(final int lineNumber, final String lineContent) {
        new AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除第" + lineNumber + "行吗？\n内容: " + 
                        (lineContent.length() > 50 ? lineContent.substring(0, 50) + "..." : lineContent))
            .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteLineFromFile(lineNumber);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void deleteLineFromFile(int lineNumberToDelete) {
        try {
            List<String> lines = new ArrayList<String>();
            BufferedReader reader = new BufferedReader(new FileReader(FILE_PATH));
            String line;
            int currentLine = 1;

            while ((line = reader.readLine()) != null) {
                if (currentLine != lineNumberToDelete) {
                    lines.add(line);
                }
                currentLine++;
            }
            reader.close();

            FileWriter writer = new FileWriter(FILE_PATH);
            for (String savedLine : lines) {
                writer.write(savedLine + "\n");
            }
            writer.close();

            rebuildNotesMap(lineNumberToDelete);

            Toast.makeText(this, "已删除第" + lineNumberToDelete + "行", Toast.LENGTH_SHORT).show();

            loadFileContent();

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void rebuildNotesMap(int deletedLineNumber) {
        HashMap<Integer, String> newNotesMap = new HashMap<Integer, String>();

        File notesFile = new File(NOTES_FILE_PATH);
        if (notesFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(notesFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(":", 2);
                    if (parts.length == 2) {
                        int originalLineNum = Integer.parseInt(parts[0].trim());
                        String note = parts[1].trim();

                        if (originalLineNum < deletedLineNumber) {
                            newNotesMap.put(originalLineNum, note);
                        } else if (originalLineNum > deletedLineNumber) {
                            newNotesMap.put(originalLineNum - 1, note);
                        }
                    }
                }
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        notesMap = newNotesMap;
        saveNotesToFile();
    }

    private void shareFileContent() {
        if (fileLines.isEmpty()) {
            Toast.makeText(this, "没有内容可分享", Toast.LENGTH_SHORT).show();
            return;
        }

        StringBuilder shareText = new StringBuilder();
        shareText.append("伪码上号器数据分享\n\n");

        for (int i = 0; i < fileLines.size(); i++) {
            String line = fileLines.get(i);
            String note = notesMap.get(i + 1);
            shareText.append((i + 1) + ". ").append(line);
            if (note != null && !note.isEmpty()) {
                shareText.append(" [备注: ").append(note).append("]");
            }
            shareText.append("\n");
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText.toString());
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "伪码上号器数据");

        try {
            startActivity(Intent.createChooser(shareIntent, "分享数据"));
        } catch (Exception e) {
            Toast.makeText(this, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String mainOrientation = prefs.getString(PREF_MAIN_SCREEN_ORIENTATION, SCREEN_ORIENTATION_LANDSCAPE);
        applyMainScreenOrientation(mainOrientation);

        loadFileContent();
        updateModeState();
        updateAutoLoginCheckbox();
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        }
    }

    public void hidePackageClicked(View view) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String cmd = "pm hide " + PACKAGE_NAME + "\n";
            os.writeBytes(cmd);
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Toast.makeText(this, "隐藏成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "隐藏失败，请给root再重试", Toast.LENGTH_SHORT).show();
        }
    }

    public void unhidePackageClicked(View view) {
        try {
            Process process = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(process.getOutputStream());
            String cmd = "pm unhide " + PACKAGE_NAME + "\n";
            os.writeBytes(cmd);
            os.writeBytes("exit\n");
            os.flush();
            process.waitFor();
            Toast.makeText(this, "恢复成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "恢复失败", Toast.LENGTH_SHORT).show();
        }
    }

    // 辅助方法：解码 Unicode 字符（已在 parseCloudGames 等处使用）
    private String decodeUnicode(String unicodeStr) {
        if (unicodeStr == null) return "";
        try {
            Pattern pattern = Pattern.compile("(\\\\u[0-9a-fA-F]{4})");
            Matcher matcher = pattern.matcher(unicodeStr);
            StringBuffer sb = new StringBuffer();
            while (matcher.find()) {
                String unicode = matcher.group(1);
                String ch = String.valueOf((char) Integer.parseInt(unicode.substring(2), 16));
                matcher.appendReplacement(sb, ch);
            }
            matcher.appendTail(sb);
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return unicodeStr;
        }
    }
}
