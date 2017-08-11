package com.aylanetworks.aura;
/*
 * Android_AylaSDK
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.localdevice.AuraLocalDeviceManager;
import com.aylanetworks.aura.util.AuraConfig;
import com.aylanetworks.aura.util.PushUtils;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaDeviceManager;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaSystemSettings;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.aylanetworks.aylasdk.util.SystemInfoUtils;
import com.aylanetworks.localdevice.ble.AylaBLEDeviceManager;
import com.baidu.android.pushservice.PushConstants;
import com.baidu.android.pushservice.PushManager;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, AylaSessionManager.SessionManagerListener {

    private static final String LOG_TAG = "MainActivity";
    public static final String URL_SCHEME="auracontrol";
    public static final String ARG_URL = "url";

    // Activity result codes
    public static final int REQUEST_SIGN_IN = 1;
    public static final String SESSION_NAME = "AuraSession";
    public static final String supportEmail = "mobile-libraries@aylanetworks.com";
    private static MainActivity __sharedInstance;

    private String _launchUrl;

    public static AylaLog.LogLevel getConsoleLogLevel() {
        return consoleLogLevel;
    }

    public static void setConsoleLogLevel(AylaLog.LogLevel consoleLogLevel) {
        MainActivity.consoleLogLevel = consoleLogLevel;
    }

    public static AylaLog.LogLevel getFileLoglevel() {
        return fileLoglevel;
    }

    public static void setFileLoglevel(AylaLog.LogLevel fileLoglevel) {
        MainActivity.fileLoglevel = fileLoglevel;
    }

    private static AylaLog.LogLevel consoleLogLevel = AylaLog.LogLevel.Debug;
    private static AylaLog.LogLevel fileLoglevel = AylaLog.LogLevel.Error;

    public static MainActivity sharedInstance() {
        return __sharedInstance;
    }

    public String getAppVersion() {
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pi.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        AylaLog.d(LOG_TAG, "onNewIntent: " + intent);
        if (intent.hasCategory(Intent.CATEGORY_BROWSABLE) && intent.getScheme().equals(URL_SCHEME)) {
            // Get the URL
            String url = intent.getDataString();
            if (url != null) {
                _launchUrl = url;
                navigateSignIn();
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        __sharedInstance = this;

        // Initialize the SDK
        AylaNetworks.initialize(getSystemSettings());

        AuraLocalDeviceManager ldm = new AuraLocalDeviceManager(this);
        AylaLog.initAylaLog("ayla_logs", consoleLogLevel, fileLoglevel);

        // Install the AylaBLEDeviceManager as the local device plugin
        AylaNetworks.sharedInstance().installPlugin(AylaLocalDeviceManager.PLUGIN_ID_LOCAL_DEVICE,
                ldm);

        // This same plugin is also responsible for device class creation
        AylaNetworks.sharedInstance().installPlugin(AylaNetworks.PLUGIN_ID_DEVICE_CLASS, ldm);

        // This same plugin is also responsible for manipulating the device list
        AylaNetworks.sharedInstance().installPlugin(AylaNetworks.PLUGIN_ID_DEVICE_LIST, ldm);

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent.hasCategory(Intent.CATEGORY_BROWSABLE) && intent.getScheme().equals(URL_SCHEME)){
            // Get the URL
            String url = intent.getDataString();
            AylaLog.i(LOG_TAG, "URLHandler opening URL: " + url);
            _launchUrl = url;
        } else {
            _launchUrl = null;
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();


        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        TextView navHeaderAppName = (TextView) navigationView.getHeaderView(0)
                .findViewById(R.id.nav_header_app_name);
        String appName = "Aura version " + getAppVersion();
        navHeaderAppName.setText(appName);


        // Are we already signed in?
        if (getSession() == null ) {
            // Not currently signed in. See if there was a previous login we saved.
            CachedAuthProvider cachedProvider = CachedAuthProvider.getCachedProvider(this);
            if (cachedProvider != null) {
                // Log in using the cached credentials
                final Snackbar sb = Snackbar.make(findViewById(R.id.fragment_container),
                        R.string.signing_in_cached, Snackbar.LENGTH_INDEFINITE);
                sb.show();

                AylaNetworks.sharedInstance().getLoginManager().signIn(cachedProvider,
                        SESSION_NAME,
                        new Response.Listener<AylaAuthorization>() {
                            @Override
                            public void onResponse(AylaAuthorization response) {
                                sb.dismiss();
                                Snackbar.make(findViewById(R.id.fragment_container),
                                        R.string.signed_in_cached, Snackbar.LENGTH_SHORT).show();
                                // Save the updated credentials for the next login
                                CachedAuthProvider.cacheAuthorization(MainActivity.this, response);
                                signInComplete();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                sb.dismiss();
                                AylaLog.e(LOG_TAG, "Cached sign-in failed: " + error.getMessage());
                                Snackbar.make(findViewById(R.id.fragment_container),
                                        error.toString(), Snackbar.LENGTH_LONG).show();
                                navigateSignIn();
                            }
                        });
            } else {
                navigateSignIn();
            }
        } else{
            signInComplete();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        AylaNetworks.sharedInstance().onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AylaNetworks.sharedInstance().onResume();
        navigateHome();
    }

    // Helper methods

    public static AylaSessionManager getSession() {
        return AylaNetworks.sharedInstance().getSessionManager(SESSION_NAME);
    }

    public static AylaDeviceManager getDeviceManager() {
        AylaSessionManager session = getSession();
        if (session != null) {
            return session.getDeviceManager();
        }
        return null;
    }

    private AylaSystemSettings getSystemSettings() {
        // Create a new settings object with default values
        AylaSystemSettings settings = new AylaSystemSettings();

        // Override with the selected Aura configuration
        AuraConfig.getSelectedConfiguration().apply(settings);

        // Add in our own app-specific details
        settings.deviceDetailProvider = new AuraDeviceDetailProvider();
        settings.pushNotificationSenderId = PushNotification.registrationId;
        settings.context = this;

        return settings;
    }

    public void signInComplete() {

        AylaSessionManager sm = AylaNetworks.sharedInstance().getSessionManager(SESSION_NAME);
        if (sm != null) {
            sm.addListener(this);
        } else {
            // User hit the back button while the sign-in activity was in the foreground
            onPause();
            finish();
        }
        if(getSession() != null && !getSession().isCachedSession()){
            try {
                AylaNetworks.sharedInstance()
                        .getSessionManager(MainActivity.SESSION_NAME)
                        .fetchUserProfile(new Response.Listener<AylaUser>() {
                                              @Override
                                              public void onResponse(AylaUser response) {
                                                  AylaSystemSettings settings = AylaNetworks.
                                                          sharedInstance().getSystemSettings();
                                                  if (settings != null &&
                                                          settings.pushNotificationSenderId != null) {
                                                      PushNotification pushNotification =
                                                              new PushNotification();
                                                      pushNotification.init(
                                                              settings.pushNotificationSenderId,
                                                              response.getEmail(),
                                                              settings.appId);

                                                  }

                                                  if (PushUtils.isBaiduPushAvailable(settings.context)) {
                                                      PushManager.startWork(
                                                              settings.context
                                                              , PushConstants.LOGIN_TYPE_API_KEY
                                                              , PushUtils.getMetaValue(
                                                                      settings.context
                                                                      , "api_key")
                                                      );
                                                  }
                                              }
                                          },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        AylaLog.i(LOG_TAG, " Unable to fetch user profile" +
                                                error.getMessage());
                                    }
                                });

            } catch (Exception ex) {
                //If push notification fails just continue.
                AylaLog.i(LOG_TAG, "pushNotification Init Failed : " + ex.getMessage());
            }
        }

        navigateHome();
    }

    public void navigateSignIn() {
        Intent intent = new Intent(this, LoginActivity.class);
        if (_launchUrl != null) {
            intent.putExtra(ARG_URL, _launchUrl);
            _launchUrl = null;
        }

        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

        startActivityForResult(intent, REQUEST_SIGN_IN);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Fragment fragment = null;
        switch (id) {
            case R.id.device_list:
                fragment = DeviceListFragment.newInstance();
                break;

            case R.id.shares_list:
                fragment = ShareListFragment.newInstance();
                break;

            case R.id.setup_wiz:
                fragment = SetupWizardFragment.newInstance();
                break;

            case R.id.wifi_setup:
                fragment = WifiSetupFragment.newInstance();
                break;

            case R.id.registration:
                fragment = RegistrationFragment.newInstance();
                break;

            case R.id.ota:
                fragment = LanOTAFragment.newInstance();
                break;

            case R.id.edit_account:
                fragment = AccountDetailsFragment.newInstance(true);
                break;

            case R.id.delete_account:
                handleAccountDelete();
                break;

            case R.id.sign_out:
                handleSignOut();
                break;

            case R.id.email_logs:
                handleSendEmail();
                break;

            case R.id.about:
                fragment = AboutFragment.newInstance();
                break;

        }

        pushFragment(fragment);
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void pushFragment(Fragment fragment) {
        if (fragment != null) {
            if (fragment instanceof DeviceListFragment) {
                getSupportFragmentManager().popBackStack(null,
                        FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit();
            } else {
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGN_IN) {
            // We should be signed in now.
            Snackbar.make(findViewById(R.id.fragment_container),
                    R.string.signed_in, Snackbar.LENGTH_SHORT).show();
            signInComplete();
        }
    }

    @Override
    public void sessionClosed(String sessionName, AylaError error) {
        AylaLog.d(LOG_TAG, "Session \"" + sessionName + "\" closed: " + error);
        navigateSignIn();
        if (error == null) {
            // The user signed out. Clear the cached authorization.
            CachedAuthProvider.clearCachedAuthorization(this);
        }
    }

    public void handleSignOut() {
        AylaSessionManager session = getSession();
        if (session != null) {
            final Snackbar sb = Snackbar.make(findViewById(R.id.fragment_container),
                    R.string.signing_out, Snackbar.LENGTH_INDEFINITE);

            AylaAPIRequest request = session.shutDown(
                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            sb.dismiss();
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            AylaLog.e(LOG_TAG, "Error sending sign-out request: " + error
                                    .getMessage());
                            sb.dismiss();
                        }
                    });
            if (request != null) {
                sb.show();
            }
        } else {
            // Not sure how we got here, but we should show the sign-in view if we don't have a
            // session.
            navigateSignIn();
        }
    }

    public void handleAccountDelete() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.confirm_delete_account_title)
                .setMessage(R.string.confirm_delete_account_message)
                .setPositiveButton(R.string.delete_account, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deleteAccount();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create().show();
    }

    public void deleteAccount() {
        AylaSessionManager session = getSession();
        if (session != null) {
            final Snackbar sb = Snackbar.make(findViewById(R.id.fragment_container),
                    R.string.deleting_account, Snackbar.LENGTH_INDEFINITE);
            AylaAPIRequest request = session.deleteAccount(
                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                        @Override
                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                            sb.dismiss();
                            handleSignOut();
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            AylaLog.e(LOG_TAG, "Error deleting account: " + error
                                    .getMessage());
                            Snackbar.make(findViewById(R.id.fragment_container),
                                    error.toString(), Snackbar.LENGTH_LONG).show();
                            sb.dismiss();
                        }
                    });
            if (request != null) {
                sb.show();
            }
        }else {
            navigateSignIn();
        }
    }

    @Override
    public void authorizationRefreshed(String sessionName, AylaAuthorization authorization) {
        AylaLog.d(LOG_TAG, "Authorization refreshed for session \"" + sessionName + "\"");

        // Update our cached authorization
        CachedAuthProvider.cacheAuthorization(this, authorization);
    }

    public void navigateHome() {
        // We always start with the device list
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        DeviceListFragment frag = DeviceListFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, frag)
                .commit();
    }

    public void handleSendEmail(){
        String[] supportEmailAddress = {supportEmail};
        StringBuilder strBuilder = new StringBuilder(200);
        strBuilder.append( "Latest logs from Aura app attached");
        strBuilder.append("\n\n OS Version: " + SystemInfoUtils.getOSVersion());
        strBuilder.append("\n SDK Version: " + SystemInfoUtils.getSDKVersion());
        strBuilder.append("\nCountry: " + SystemInfoUtils.getCountry());
        strBuilder.append("\nLanguage: " + SystemInfoUtils.getLanguage());
        strBuilder.append("\nNetwork Operator: " + SystemInfoUtils.getNetworkOperator());
        strBuilder.append("\nAyla SDK version: " + AylaNetworks.getVersion());
        strBuilder.append("\nAura app version: " + getAppVersion());
        Intent emailIntent = AylaLog.getEmailIntent(supportEmailAddress, "Aura Logs",
              strBuilder.toString() );
        if(emailIntent != null){
            startActivity(emailIntent);
        } else{
            Toast.makeText(this, getResources().getString(R.string.email_send_error), Toast
                    .LENGTH_SHORT).show();
        }
    }
}
