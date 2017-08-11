package com.aylanetworks.aura;
/*
 * Android_AylaSDK
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.TextUtils;
import android.text.method.PasswordTransformationMethod;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Response;
import com.aylanetworks.aura.util.AuraConfig;
import com.aylanetworks.aura.util.SharedPreferencesUtil;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaEmailTemplate;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.auth.AylaAuthorization;
import com.aylanetworks.aylasdk.auth.AylaOAuthProvider;
import com.aylanetworks.aylasdk.auth.AylaWeChatAuthProvider;
import com.aylanetworks.aylasdk.auth.CachedAuthProvider;
import com.aylanetworks.aylasdk.auth.FaceBookOAuthProvider;
import com.aylanetworks.aylasdk.auth.GoogleOAuthProvider;
import com.aylanetworks.aylasdk.auth.UsernameAuthProvider;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.api.Status;

import java.util.ArrayList;
import java.util.List;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.READ_EXTERNAL_STORAGE;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>,
        GoogleApiClient.OnConnectionFailedListener, GoogleApiClient.ConnectionCallbacks{

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int REQUEST_READ_CONTACTS = 0;
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_GOOGLE_SIGN_IN = 2;

    private static final String LOG_TAG = "LoginActivity";

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private View mProgressView;
    private View mLoginFormView;
    private WebView mWebView;
    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onNewIntent(Intent intent) {
       super.onNewIntent(intent);
        AylaLog.d("LoginActivity", "onNewIntent: " + intent);
        String url = intent.getStringExtra(MainActivity.ARG_URL);
        if (url != null) {
            handleUrl(url);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String url = intent.getStringExtra(MainActivity.ARG_URL);
        setContentView(R.layout.activity_login);

        //Request storage permission for logs
        requestStorage();

        // Set up the login form.
        mEmailView = (AutoCompleteTextView) findViewById(R.id.signin_email);
        populateAutoComplete();
        String cachedEmail = SharedPreferencesUtil.getInstance(this).getCachedEmail();
        mEmailView.setText(cachedEmail);

        mPasswordView = (EditText) findViewById(R.id.login_password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button button = (Button) findViewById(R.id.email_sign_in_button);
        if (button != null) {
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    attemptLogin();
                }
            });
        }


        mWebView = (WebView) findViewById(R.id.webview);
        ImageButton mGoogleSignInButton = (ImageButton) findViewById(R.id.google_login);
        if (mGoogleSignInButton != null) {
            mGoogleSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    googleOAuthSignIn();
                }
            });
        }

        ImageButton mFaceBookSignInButton = (ImageButton) findViewById(R.id.facebook_login);
        if (mFaceBookSignInButton != null) {
            mFaceBookSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    fbOAuthSignIn();
                }
            });
        }
        ImageButton mWeChatSignInButton = (ImageButton) findViewById(R.id.wechat_login);
        if (mWeChatSignInButton != null) {
            mWeChatSignInButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    wechatOAuthSignIn();
                }
            });
        }

        button = (Button) findViewById(R.id.sign_up);
        if (button != null) {
            button.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    signUpClicked();
                }
            });
        }
        mLoginFormView = findViewById(R.id.layout_sign_in);
        mProgressView = findViewById(R.id.login_progress);

        TextView tv = (TextView) findViewById(R.id.resend_confirmation);
        if (tv != null) {
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    resendConfirmationClicked();
                }
            });
        }

        tv = (TextView) findViewById(R.id.forgot_password);
        if (tv != null) {
            tv.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    forgotPasswordClicked();
                }
            });
        }

        setTitle(R.string.app_name);

        TextView version = (TextView)findViewById(R.id.version);
        String versionString = "Aura version " + MainActivity.sharedInstance().getAppVersion();
        if (version != null) {
            version.setText(versionString);
        }

        version.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(LoginActivity.this, DeveloperOptionsActivity.class);
                startActivity(intent);
            }
        });

        TextView auraConfig = (TextView)findViewById(R.id.aura_config);
        if (auraConfig != null) {
            AuraConfig config = AuraConfig.getSelectedConfiguration();
            if (config != null) {
                String configText = "Configuration: " + config.toString();
                auraConfig.setText(configText);
            }
        }

        if (url != null) {
            handleUrl(url);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        TextView auraConfig = (TextView)findViewById(R.id.aura_config);
        if (auraConfig != null) {
            AuraConfig config = AuraConfig.getSelectedConfiguration();
            if (config != null) {
                String configText = "Configuration: " + config.toString();
                auraConfig.setText(configText);
            }
        }
    }

   private void resendConfirmationClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_email_address);
        final EditText et = new EditText(this);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(et);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showProgress(true);
                String emailTemplateId = getResources().getString(R.string
                        .confirmation_template_id);
                String emailSubject = getResources().getString(R.string
                        .confirmation_email_subject);

                AylaEmailTemplate template = new AylaEmailTemplate();
                template.setEmailSubject(emailSubject);
                template.setEmailTemplateId(emailTemplateId);
                AylaNetworks.sharedInstance().getLoginManager()
                        .resendConfirmationEmail(et.getText().toString(), template,
                                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        showProgress(false);
                                        View v = findViewById(android.R.id.content);
                                        if (v != null) {
                                            Snackbar.make(v, R.string.confirmation_resent,
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        showProgress(false);
                                        View v = findViewById(android.R.id.content);
                                        if (v != null) {
                                            Snackbar.make(v, error.getMessage(),
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                });
            }
        });
        builder.show();
    }

    private void forgotPasswordClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.enter_email_address);
        final EditText et = new EditText(this);
        et.setText(mEmailView.getText().toString());
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        builder.setView(et);
        builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                if (Constants.BACKDOOR.equals(et.getText().toString())) {
                    Intent intent = new Intent(LoginActivity.this, DeveloperOptionsActivity.class);
                    startActivity(intent);
                    return;
                }

                showProgress(true);

                AylaEmailTemplate template = new AylaEmailTemplate();
                String emailTemplateId = getResources().getString(R.string
                        .password_reset_template_id);
                String emailSubject = getResources().getString(R.string
                        .password_reset_email_subject);

                template.setEmailTemplateId(emailTemplateId);
                template.setEmailSubject(emailSubject);
                AylaNetworks.sharedInstance().getLoginManager()
                        .requestPasswordReset(et.getText().toString(), template,
                                new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                    @Override
                                    public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                        showProgress(false);
                                        View v = findViewById(android.R.id.content);
                                        if (v != null) {
                                            Snackbar.make(v, R.string.password_reset,
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                },
                                new ErrorListener() {
                                    @Override
                                    public void onErrorResponse(AylaError error) {
                                        showProgress(false);
                                        View v = findViewById(android.R.id.content);
                                        if (v != null) {
                                            Snackbar.make(v, error.getMessage(),
                                                    Snackbar.LENGTH_SHORT).show();
                                        }
                                    }
                                });
            }
        });
        builder.show();
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    private void handleUrl(String url) {
        AylaLog.d("URL", "Opened with URL: " + url);
        // sign-up confirmation:
        // auracontrol://user_sign_up_token?token=pdsWFmcU

        // Reset password confirmation:
        // auracontrol://user_reset_password_token?token=3DrjCTqs

        String[] parts = url.split(":");
        if (parts.length < 2) {
            AylaLog.e("URL", "Bad URL: " + url);
            return;
        }

        String scheme = parts[0];
        if (!TextUtils.equals(scheme, MainActivity.URL_SCHEME)) {
            AylaLog.e("URL", "Bad scheme: " + scheme);
        }

        parts = parts[1].split("\\?");
        if (parts.length != 2) {
            AylaLog.e("URL", "No query string found in URL: " + url);
            return;
        }
        String request = parts[0].substring(2);
        String query = parts[1];

        parts = query.split("=");
        String token;
        if (parts.length == 2) {
            token = parts[1];
            AylaLog.d("URL", "Sign-up token: " + token);
        } else {
            AylaLog.e("URL", "Bad query string: " + query);
            return;
        }

        if (TextUtils.equals(request, "user_sign_up_token")) {
            handleSignUpToken(token);
        } else if (TextUtils.equals(request, "user_reset_password_token")) {
            handleResetPasswordToken(token);
        }
    }

    private void handleSignUpToken(String token) {
        final Snackbar sb = Snackbar.make(mEmailView, R.string.sending_signup_token,
                Snackbar .LENGTH_INDEFINITE);

        final AylaAPIRequest request = AylaNetworks.sharedInstance().getLoginManager()
                .confirmSignUp(token, new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                sb.dismiss();
                                Snackbar.make(mEmailView, R.string.sign_up_complete,
                                        Snackbar.LENGTH_LONG).show();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                sb.dismiss();
                                Snackbar.make(mEmailView, error.getMessage(),
                                        Snackbar.LENGTH_SHORT).show();
                            }
                        });

        sb.setAction(android.R.string.cancel, new OnClickListener() {
            @Override
            public void onClick(View v) {
                request.cancel();
                sb.dismiss();
            }
        });
        sb.show();
    }

    private void handleResetPasswordToken(final String token) {
        // Prompt the user for the new password and confirmation
        AlertDialog.Builder d = new AlertDialog.Builder(this);
        d.setTitle(R.string.reset_password_title);
        final EditText password = new EditText(this);
        final EditText confirm = new EditText(this);
        password.setTransformationMethod(PasswordTransformationMethod.getInstance());
        confirm.setTransformationMethod(PasswordTransformationMethod.getInstance());
        password.setHint(R.string.password);
        confirm.setHint(R.string.confirm_password);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(password);
        layout.addView(confirm);
        d.setView(layout);
        d.setPositiveButton(R.string.set_new_password, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, int which) {
                View v = findViewById(android.R.id.content);

                final Snackbar sb = Snackbar.make(v, R.string.updating_password, Snackbar
                        .LENGTH_INDEFINITE);

                if (password.getText().toString().equals(confirm.getText().toString())) {
                    final AylaAPIRequest request = AylaNetworks.sharedInstance().getLoginManager()
                            .resetPassword(password.getText().toString(), token,
                                    new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                                        @Override
                                        public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                            dialog.dismiss();
                                            sb.dismiss();
                                            Toast.makeText(LoginActivity.this,
                                                    R.string.change_password_success,
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    },
                                    new ErrorListener() {
                                        @Override
                                        public void onErrorResponse(AylaError error) {
                                            sb.dismiss();
                                            confirm.setText("");
                                            Toast.makeText(LoginActivity.this,
                                                    error.getMessage(),
                                                    Toast.LENGTH_LONG).show();
                                        }
                                    });
                    if (request != null) {
                        sb.setAction(android.R.string.cancel, new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                request.cancel();
                                sb.dismiss();
                            }
                        });

                        sb.show();
                    }
                } else {
                    Toast.makeText(LoginActivity.this, R.string.password_no_match, Toast
                            .LENGTH_LONG).show();
                }
            }
        });

        d.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        d.create().show();
    }

    private void requestStorage() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }
        if (checkSelfPermission(READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            return;
        }
        requestPermissions(new String[]{READ_EXTERNAL_STORAGE}, REQUEST_EXTERNAL_STORAGE);
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            boolean permissionGranted =  grantResults.length == 1 && grantResults[0] ==
                    PackageManager.PERMISSION_GRANTED;
            if(!permissionGranted){
                MainActivity.setFileLoglevel(AylaLog.LogLevel.None);
            }

        }
    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        // Reset errors.
        mEmailView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_field_required));
            focusView = mEmailView;
            cancel = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            focusView = mEmailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner and perform the user login attempt.
            showProgress(true);
            UsernameAuthProvider authProvider = new UsernameAuthProvider(email, password);
            AylaNetworks.sharedInstance().getLoginManager().signIn(authProvider,
                    MainActivity.SESSION_NAME,
                    new Response.Listener<AylaAuthorization>() {
                        @Override
                        public void onResponse(AylaAuthorization response) {
                            // Cache the authorization
                            CachedAuthProvider.cacheAuthorization(LoginActivity.this, response);
                            setResult(Activity.RESULT_OK);
                            showProgress(false);
                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            finish();

                            // Cache account email, avoid users have to input every time
                            SharedPreferencesUtil.getInstance(LoginActivity.this).saveAccountEmail(mEmailView.getText().toString());
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(AylaError error) {
                            Toast.makeText(LoginActivity.this, error.toString(), Toast.LENGTH_LONG)
                                    .show();
                            CachedAuthProvider.clearCachedAuthorization(LoginActivity.this);
                            showProgress(false);
                            mPasswordView.requestFocus();
                        }
                    });
        }
    }

    private void fbOAuthSignIn() {
        mWebView.setVisibility(View.VISIBLE);
        // Clear out any previous contents of the webview
        String webViewEmptyHTML = this.getResources().getString(R.string.oauth_empty_html);
        webViewEmptyHTML = webViewEmptyHTML.replace("[[PROVIDER]]", FaceBookOAuthProvider.FACEBOOK_AUTH);
        mWebView.loadDataWithBaseURL("", webViewEmptyHTML, "text/html", "UTF-8", "");
        mWebView.bringToFront();

        FaceBookOAuthProvider aylaOAuthProvider = new FaceBookOAuthProvider(mWebView);

        AylaNetworks.sharedInstance().getLoginManager().signIn(aylaOAuthProvider,
                MainActivity.SESSION_NAME,
                new Response.Listener<AylaAuthorization>() {
                    @Override
                    public void onResponse(AylaAuthorization response) {
                        // Cache the authorization
                        finish();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(LoginActivity.this, error.toString(), Toast.LENGTH_LONG)
                                .show();
                    }
                });
    }

    private void googleOAuthSignIn(){

        //Create Google sign in options for Google oAuth.
        GoogleSignInOptions googleSignInOptions = new GoogleSignInOptions.Builder
                (GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail()
                .requestServerAuthCode(getString(R.string.server_client_id))  //Replace this
                // string with client id for your app
                .requestScopes(new Scope((Scopes.EMAIL)))
                .build();

        //Build a GoogleApiClient
        mGoogleApiClient = new GoogleApiClient.Builder(this).enableAutoManage(this,
                this).addApi(Auth.GOOGLE_SIGN_IN_API, googleSignInOptions)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .build();

        //This intent will be fired when the google account is selected on the google sign in
        // client.
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        startActivityForResult(signInIntent, REQUEST_GOOGLE_SIGN_IN);
        showProgress(true);
    }

    private void wechatOAuthSignIn() {
        //Check if WeChat App is installed or not. This is needed for WeChat feature to work
         if(!isAppInstalled("com.tencent.mm")){
            Toast.makeText(LoginActivity.this, R.string.wechat_install_required, Toast.LENGTH_LONG)
                    .show();
            return;
        }

        View view = findViewById(android.R.id.content);
        final Snackbar sb = Snackbar.make(view, R.string.wechat_login_wait, Snackbar.LENGTH_INDEFINITE);
        AylaOAuthProvider aylaOAuthProvider = new AylaWeChatAuthProvider(Constants.WECHAT_APP_ID);
        AylaNetworks.sharedInstance().getLoginManager().signIn(aylaOAuthProvider,
                MainActivity.SESSION_NAME,
                new Response.Listener<AylaAuthorization>() {
                    @Override
                    public void onResponse(AylaAuthorization response) {
                        // Cache the authorization
                        finish();
                        sb.dismiss();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Toast.makeText(LoginActivity.this, error.toString(), Toast.LENGTH_LONG)
                                .show();
                        sb.dismiss();
                    }
                });
        sb.show();
    }

    private boolean isAppInstalled(String uri) {
        PackageManager pm = getPackageManager();
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            AylaLog.e("WeChat", "PackageManager error: " + e);
        }

        return false;
    }


    private void signUpClicked() {
        // Sign up
        // Get rid of the main view and show the sign-up view
        View v = findViewById(R.id.layout_sign_in);
        if (v != null) {
            v.setVisibility(View.GONE);
        }

        v = findViewById(R.id.layout_account_details);
        if (v != null) {
            v.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (mWebView != null && mWebView.getVisibility() == View.VISIBLE) {
            mWebView.setVisibility(View.GONE);
            return;
        }

        View signInView = findViewById(R.id.layout_sign_in);
        View detailView = findViewById(R.id.layout_account_details);
        if (signInView != null && detailView != null &&
            detailView.getVisibility() == View.VISIBLE) {
            detailView.setVisibility(View.GONE);
            signInView.setVisibility(View.VISIBLE);
        } else {
            super.onBackPressed();
        }
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        AylaLog.d(LOG_TAG, "onConnectionFailed "+connectionResult.getErrorMessage());
        Toast.makeText(this, connectionResult.getErrorMessage(), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        AylaLog.d(LOG_TAG, "onConnected ");
    }

    @Override
    public void onConnectionSuspended(int i) {
        AylaLog.d(LOG_TAG, "onConnectionSuspended ");
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        AylaLog.d(LOG_TAG, "onActivityResult ");

        //Check for result returned from signInIntent
       if(requestCode == REQUEST_GOOGLE_SIGN_IN){
           //check if sign in was successful
           GoogleSignInResult googleSignInResult = Auth.GoogleSignInApi.
                   getSignInResultFromIntent(data);
           if(googleSignInResult.isSuccess()){
               // Google sign in is success. Now login to Ayla User service using auth code
               // returned from Google service.
               String serverAuthCode = googleSignInResult.getSignInAccount().getServerAuthCode();
               AylaLog.consoleLogDebug(LOG_TAG, "serverAuthCode "+serverAuthCode);

               GoogleOAuthProvider googleOAuthProvider = new GoogleOAuthProvider(serverAuthCode,mWebView);

               //Sign in to Ayla user service using GoogleOAuthProvider
               AylaNetworks.sharedInstance().getLoginManager().signIn(googleOAuthProvider,
                       MainActivity.SESSION_NAME, new Response.Listener<AylaAuthorization>() {
                           @Override
                           public void onResponse(AylaAuthorization response) {
                               showProgress(false);
                               Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                               intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                               startActivity(intent);
                               finish();
                           }
                       }, new ErrorListener() {
                           @Override
                           public void onErrorResponse(AylaError error) {
                               showProgress(false);
                               Toast.makeText(MainActivity.sharedInstance(), "Sign in to Ayla " +
                                       "cloud using Google oAuth failed with error "+error
                                       .getMessage(), Toast
                                       .LENGTH_SHORT).show();
                              disconnectGoogleClient();
                           }
                       });
               Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback
                       (new ResultCallback<Status>() {
                           @Override
                           public void onResult(@NonNull Status status) {
                               AylaLog.d(LOG_TAG, "Signed out from Google "+status);
                           }
                       });

           } else{
               showProgress(false);
               AylaLog.d(LOG_TAG, "GoogleSignInFailed "+googleSignInResult.getStatus());
              disconnectGoogleClient();
           }
       }
    }

    private void disconnectGoogleClient(){
        if(mGoogleApiClient != null){
            mGoogleApiClient.stopAutoManage(this);
            mGoogleApiClient.disconnect();
        }
    }
}

