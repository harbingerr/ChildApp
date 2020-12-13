package at.ciit.usagestats;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AppOpsManager;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static android.app.AppOpsManager.MODE_ALLOWED;
import static android.app.AppOpsManager.OPSTR_GET_USAGE_STATS;

public class MainActivity extends AppCompatActivity {

    Button enableBtn, showBtn, changeNameBtn;
    EditText nameEt;
    TextView tv_userId, usageTv;
    ListView appsList;
    private String id = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        enableBtn = findViewById(R.id.enable_btn);
        showBtn =  findViewById(R.id.show_btn);
        tv_userId =findViewById(R.id.id_tv);
        usageTv =  findViewById(R.id.usage_tv);
        nameEt = findViewById(R.id.tv_name);
        appsList =  findViewById(R.id.apps_list);
        changeNameBtn = findViewById(R.id.button);
        startService(new Intent(this, TimeService.class));
        this.loadStatistics();
    }



    // each time the application gets in foreground -> getGrantStatus and render the corresponding buttons
    @Override
    protected void onStart() {
        super.onStart();
        if (getGrantStatus()) {
            //if there is no permission
            showHideWithPermission();

            SharedPreferences preferences = getSharedPreferences("ids", MODE_PRIVATE);
            id = preferences.getString("UserId","");

            if(!id.isEmpty()){
                tv_userId.setText(id);
                showBtn.setOnClickListener(view -> {
                    loadStatistics();
                });
            } else {
                addChild();
                showBtn.setOnClickListener(view -> {
                    loadStatistics();
                });
            }
        } else {
            showHideNoPermission();
            enableBtn.setOnClickListener(view -> {
                startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
            });
        }
    }

    private boolean addChild() {

        AccountManager manager = AccountManager.get(this);
        Account[] accounts = manager.getAccountsByType("com.google");
        List<String> possibleEmails = new LinkedList<String>();
        String username = nameEt.getText().toString();

        Response.Listener<String> response = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                try {
                    Log.i("tagconvertstr", "["+response+"]");
                    JSONObject jsonResponse = new JSONObject(response);
                    String userId = jsonResponse.getString("name");

                    if(!userId.isEmpty() && userId != "nothing"){
                        SharedPreferences preferences = getSharedPreferences("ids", MODE_PRIVATE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("UserId", userId);
                        editor.apply();
                        tv_userId.setText(userId);
                    }
                    else if(userId == "Please use another name"){
                        tv_userId.setText("use another name");
                        changeNameBtn.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                addChild();
                            }
                        });

                    }
                    else{
                        AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
                        alert.setMessage("Nah...nepodarilo sa velmi").setNegativeButton("Retry", null).create().show();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        };

        SetChild loginRequest = new SetChild( username,response);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(loginRequest);
        return true;
    }


    /**
     * load the usage stats for last 24h
     */
    public void loadStatistics() {
        UsageStatsManager usm = (UsageStatsManager) this.getSystemService(USAGE_STATS_SERVICE);
        List<UsageStats> appList = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY,  System.currentTimeMillis() - 1000*3600*24,  System.currentTimeMillis());

        // Group the usageStats by application and sort them by total time in foreground
        if (appList != null && appList.size() > 0) {
            SortedMap<Long, UsageStats> mySortedMap = new TreeMap<>();
            for (UsageStats usageStats : appList) {
                mySortedMap.put(usageStats.getTotalTimeInForeground(), usageStats);
            }
            showAppsUsage(mySortedMap);
        }
    }


    public void showAppsUsage(SortedMap<Long, UsageStats> mySortedMap) {
        ArrayList<App> appsList = new ArrayList<>();
        List<UsageStats> usageStatsList = mySortedMap.values().stream().filter(this::isAppInfoAvailable).collect(Collectors.toList());

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        WifiInfo wInfo = wifiManager.getConnectionInfo();
        String date = getDateTime();

        // get total time of apps usage to calculate the usagePercentage for each app
        long totalTime = usageStatsList.stream().map(UsageStats::getTotalTimeInForeground).mapToLong(Long::longValue).sum();


        boolean shitCode = true;

        HashMap <String,String> stats =  new HashMap<String,String>();

        //fill the appsList
        for (UsageStats usageStats : usageStatsList) {
            try {
                String packageName = usageStats.getPackageName();
                ApplicationInfo ai = getApplicationContext().getPackageManager().getApplicationInfo(packageName, 0);
                String appName = getApplicationContext().getPackageManager().getApplicationLabel(ai).toString();
                Log.i("[*** appky ***]", appName);

                if(appName.toLowerCase().matches("youtube") || appName.toLowerCase().matches("instagram") || appName.toLowerCase().matches("snapchat") || appName.toLowerCase().matches("tiktok") ||  appName.toLowerCase().matches("facebook") ||  appName.toLowerCase().matches("twitter") ||  appName.toLowerCase().matches("twitch") || appName.toLowerCase().matches("messenger")){
                    Drawable icon = getApplicationContext().getPackageManager().getApplicationIcon(ai);
                    String usageDuration = getDurationBreakdown(usageStats.getTotalTimeInForeground());
                    int usagePercentage = (int) (usageStats.getTotalTimeInForeground() * 100 / totalTime);

                    App usageStatDTO = new App(icon, appName, usagePercentage, usageDuration);
                    appsList.add(usageStatDTO);

                    stats.put(appName.toLowerCase().toString(),usageDuration.toString());
                }
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }


        Log.i("[*** staty ***]", stats.toString());

        sendJSON(stats);
        // reverse the list to get most usage first
        Collections.reverse(appsList);
        // build the adapter
        AppsAdapter adapter = new AppsAdapter(this, appsList);

        // attach the adapter to a ListView
        ListView listView = findViewById(R.id.apps_list);
        listView.setAdapter(adapter);

        showHideItemsWhenShowApps();
    }

    private void sendJSON(HashMap<String, String> map) {

        String youtube ="0";
        String instagram ="0";
        String snapchat ="0";
        String tiktok = "0";
        String facebook = "0";
        String twitter = "0";
        String twitch = "0";
        String messenger = "0";
        String date = getDateTime();

        if(map.get("youtube") != null) youtube =(String) map.get("youtube");
        if(map.get("instagram") != null) instagram =(String) map.get("instagram");
        if(map.get("snapchat") != null) snapchat =(String) map.get("snapchat");
        if(map.get("tiktok") != null) tiktok =(String) map.get("tiktok");
        if(map.get("facebook") != null) facebook =(String) map.get("facebook");
        if(map.get("twitter") != null) twitter =(String) map.get("twitter");
        if(map.get("twitch") != null) twitch =(String) map.get("twitch");
        if(map.get("messenger") != null) messenger =(String) map.get("messenger");

            Response.Listener<String> response = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                Log.i("tagconvertstr", "["+response+"]");
            }
        };

        SetStats setStatRequest = new SetStats(tv_userId.getText().toString(), date, youtube, instagram, snapchat, tiktok, facebook, twitter, twitch, messenger , response);
        RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
        queue.add(setStatRequest);
    }

    private String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd");
        Date date = new Date();
        Log.i("[*** date ***]", dateFormat.format(date));
        return dateFormat.format(date);
    }

    /**
     * check if PACKAGE_USAGE_STATS permission is aloowed for this application
     * @return true if permission granted
     */
    private boolean getGrantStatus() {
        AppOpsManager appOps = (AppOpsManager) getApplicationContext()
                .getSystemService(Context.APP_OPS_SERVICE);

        int mode = appOps.checkOpNoThrow(OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getApplicationContext().getPackageName());

        if (mode == AppOpsManager.MODE_DEFAULT) {
            return (getApplicationContext().checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            return (mode == MODE_ALLOWED);
        }
    }

    /**
     * check if the application info is still existing in the device / otherwise it's not possible to show app detail
     * @return true if application info is available
     */
    private boolean isAppInfoAvailable(UsageStats usageStats) {
        try {
            getApplicationContext().getPackageManager().getApplicationInfo(usageStats.getPackageName(), 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }


    /**
     * helper method to get string in format hh:mm:ss from miliseconds
     *
     * @param millis (application time in foreground)
     * @return string in format hh:mm:ss from miliseconds
     */
    private String getDurationBreakdown(long millis) {
        if (millis < 0) {
            throw new IllegalArgumentException("Duration must be greater than zero!");
        }

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);

        return (hours + " h " +  minutes + " m " + seconds + " s");
    }








    /**
     * helper method used to show/hide items in the view when  PACKAGE_USAGE_STATS permission is not allowed
     */
    public void showHideNoPermission() {
        enableBtn.setVisibility(View.VISIBLE);
        changeNameBtn.setVisibility(View.VISIBLE);
        tv_userId.setVisibility(View.GONE);
        showBtn.setVisibility(View.GONE);
        usageTv.setVisibility(View.GONE);
        appsList.setVisibility(View.GONE);
        nameEt.setVisibility(View.VISIBLE);

    }

    /**
     * helper method used to show/hide items in the view when  PACKAGE_USAGE_STATS permission allowed
     */
    public void showHideWithPermission() {
        enableBtn.setVisibility(View.GONE);
        tv_userId.setVisibility(View.VISIBLE);
        showBtn.setVisibility(View.VISIBLE);
        nameEt.setVisibility(View.VISIBLE);
        changeNameBtn.setVisibility(View.VISIBLE);
        usageTv.setVisibility(View.GONE);
        appsList.setVisibility(View.GONE);
    }

    /**
     * helper method used to show/hide items in the view when showing the apps list
     */
    public void showHideItemsWhenShowApps() {
        enableBtn.setVisibility(View.GONE);
        tv_userId.setVisibility(View.GONE);
        nameEt.setVisibility(View.GONE);
        showBtn.setVisibility(View.GONE);
        changeNameBtn.setVisibility(View.GONE);
        usageTv.setVisibility(View.VISIBLE);
        appsList.setVisibility(View.VISIBLE);

    }




}