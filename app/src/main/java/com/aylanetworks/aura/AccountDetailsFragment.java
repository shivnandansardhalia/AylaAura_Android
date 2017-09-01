package com.aylanetworks.aura;

/*
 * Android_AylaSDK
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.android.volley.Response;
import com.aylanetworks.aura.util.SharedPreferencesUtil;
import com.aylanetworks.aylasdk.AylaAPIRequest;
import com.aylanetworks.aylasdk.AylaEmailTemplate;
import com.aylanetworks.aylasdk.AylaLog;
import com.aylanetworks.aylasdk.AylaNetworks;
import com.aylanetworks.aylasdk.AylaSessionManager;
import com.aylanetworks.aylasdk.AylaUser;
import com.aylanetworks.aylasdk.error.AylaError;
import com.aylanetworks.aylasdk.error.ErrorListener;

import java.util.Locale;


/**
 * A simple {@link Fragment} subclass.
 */
public class AccountDetailsFragment extends Fragment {
    private final static String ARG_EDIT_PROFILE = "editProfile";

    private EditText _email;
    private EditText _newEmail;
    private EditText _password;
    private EditText _confirmPassword;
    private EditText _firstName;
    private EditText _lastName;
    private EditText _country;
    private EditText _phoneCountryCode;
    private EditText _phoneNumber;
    private EditText _city;
    private EditText _state;
    private EditText _zip;
    private LinearLayout _updateEmailLayout;
    private Button _button;
    private Button _updateEmailButton;

    private AylaUser _user;
    private LinearLayout _passwordGroupLayout;

    public AccountDetailsFragment() {
        // Required empty public constructor
    }

    /**
     * Returns a new AccountDetailsFragment. If the user field is non-null, the fragment will be
     * populated with the information from the specified user, and the password fields will be
     * hidden. Otherwise a new account is assumed, and all fields will be blank and the password
     * fields will be visible.
     *
     * @param editExisting if true, will fetch the user's profile for editing. Otherwise fields
     *                     will be blank for a new sign-up.
     * @return an AccountDetailsFragment
     */
    public static AccountDetailsFragment newInstance(boolean editExisting) {
        AccountDetailsFragment frag = new AccountDetailsFragment();
        Bundle bundle = new Bundle();
        bundle.putBoolean(ARG_EDIT_PROFILE, editExisting);
        frag.setArguments(bundle);

        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_account_details, container, false);

        _updateEmailLayout = (LinearLayout) view.findViewById(R.id.update_email_layout);
        _email = (EditText) view.findViewById(R.id.email);
        _newEmail = (EditText) view.findViewById(R.id.new_email);
        _password = (EditText) view.findViewById(R.id.password);
        _confirmPassword = (EditText) view.findViewById(R.id.confirm_password);
        _firstName = (EditText) view.findViewById(R.id.first_name);
        _lastName = (EditText) view.findViewById(R.id.last_name);
        _country = (EditText) view.findViewById(R.id.country);
        _phoneCountryCode = (EditText) view.findViewById(R.id.phone_country_code);
        _phoneNumber = (EditText) view.findViewById(R.id.phone_number);
        _city = (EditText) view.findViewById(R.id.city);
        _state = (EditText) view.findViewById(R.id.state);
        _zip = (EditText) view.findViewById(R.id.zipcode);
        _button = (Button) view.findViewById(R.id.account_details_button);
        _passwordGroupLayout = (LinearLayout) view.findViewById(R.id.password_group_layout);
        _updateEmailButton = (Button) view.findViewById(R.id.update_email_button);
        _updateEmailLayout.setVisibility(View.GONE);
        _updateEmailButton.setVisibility(View.GONE);

        _button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signUpClicked();
            }
        });
        _updateEmailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateEmailClicked();
            }
        });

        // See if we're editing an existing profile or not
        boolean editExisting = false;
        Bundle args = getArguments();
        if (args != null) {
            editExisting = args.getBoolean(ARG_EDIT_PROFILE);
        }

        if (editExisting && _user == null) {
            fetchUserProfile();
            _button.setText(R.string.update_profile);
            setUpdateEmailUI();
            _button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    updateProfileClicked();
                }
            });
        }

        if (_user != null) {
            populateUI(_user, false);
        }

        return view;
    }

    private void fetchUserProfile() {
        final View view = getActivity().findViewById(android.R.id.content);
        final Snackbar sb = Snackbar.make(view, R.string.fetching_profile,
                Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME)
                .fetchUserProfile(new Response.Listener<AylaUser>() {
                                      @Override
                                      public void onResponse(AylaUser response) {
                                          sb.dismiss();
                                          _user = response;
                                          if (getView() != null) {
                                              populateUI(_user, false);
                                          }
                                      }
                                  },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                sb.dismiss();
                                Snackbar.make(view, error.getMessage(), Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        });

        sb.setAction(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request.cancel();
                sb.dismiss();
            }
        });
        sb.show();
    }

    private void updateEmailClicked() {
        final String newEmail = _newEmail.getText().toString();
        if (TextUtils.isEmpty(newEmail) || !newEmail.contains("@")) {
            Snackbar.make(getView(), "Invalid email address!", Snackbar.LENGTH_SHORT).show();
            _newEmail.requestFocus();
            return;
        }

        final Snackbar sb = Snackbar.make(getView(), "Updating your email address...", Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME)
                .updateUserEmailAddress(newEmail,
                        new Response.Listener<AylaAPIRequest.EmptyResponse>() {
                            @Override
                            public void onResponse(AylaAPIRequest.EmptyResponse response) {
                                sb.dismiss();
                                SharedPreferencesUtil.getInstance(getActivity()).saveAccountEmail(newEmail);

                                new AlertDialog.Builder(getActivity())
                                        .setTitle(android.R.string.dialog_alert_title)
                                        .setMessage("Your email address has been changed.  The new email address will be required to log in.")
                                        .setCancelable(false)
                                        .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                AylaSessionManager session = AylaNetworks.sharedInstance().getSessionManager(MainActivity.SESSION_NAME);

                                                final Snackbar sb = Snackbar.make(getView(),
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
                                                                AylaLog.e("AccountDetailFragment", "Error sending sign-out request: " + error
                                                                        .getMessage());
                                                                sb.dismiss();
                                                            }
                                                        });
                                                if (request != null) {
                                                    sb.show();
                                                }
                                            }
                                        })
                                        .create().show();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                sb.dismiss();
                                Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        });
        sb.setAction(android.R.string.cancel, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                request.cancel();
                sb.dismiss();
            }
        });
        sb.show();
    }

    private void updateProfileClicked() {
        AylaUser user = new AylaUser();
        AylaError error = updateUser(user);
        if (error != null) {
            Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT).show();
            return;
        }

        final Snackbar sb = Snackbar.make(getView(), R.string.updating_profile,
                Snackbar.LENGTH_INDEFINITE);
        final AylaAPIRequest request = AylaNetworks.sharedInstance()
                .getSessionManager(MainActivity.SESSION_NAME)
                .updateUserProfile(user, new Response.Listener<AylaUser>() {
                            @Override
                            public void onResponse(AylaUser response) {
                                sb.dismiss();
                                Snackbar.make(getView(), R.string.profile_udpated, Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(AylaError error) {
                                sb.dismiss();
                                Snackbar.make(getView(), error.getMessage(), Snackbar.LENGTH_SHORT)
                                        .show();
                            }
                        });

        if (request != null) {
            sb.setAction(android.R.string.cancel, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    request.cancel();
                    sb.dismiss();
                }
            });
            sb.show();
        }
    }

    public AylaError updateUser(AylaUser user) {
        String password = _password.getText().toString();
        String confirm = _confirmPassword.getText().toString();
        if (password.length() > 0) {
            if (!TextUtils.equals(password, confirm)) {
                return new AylaError(AylaError.ErrorType.AylaError,
                        "Username and password do not match");
            }
        }

        user.setEmail(_email.getText().toString());
        user.setFirstname(_firstName.getText().toString());
        user.setLastname(_lastName.getText().toString());
        user.setCity(_city.getText().toString());
        user.setState(_state.getText().toString());
        user.setCountry(_country.getText().toString());
        user.setPhoneCountryCode(_phoneCountryCode.getText().toString());
        user.setPhone(_phoneNumber.getText().toString());
        user.setPassword(_password.getText().toString());
        user.setZip(_zip.getText().toString());

        // Service wants null passwords if we are updating profiles.
        if (user.getPassword().length() == 0) {
            user.setPassword(null);
        }

        return null;
    }

    public void populateUI(AylaUser user, boolean showPassword) {
        if (showPassword) {
            _passwordGroupLayout.setVisibility(View.VISIBLE);
        } else {
            _passwordGroupLayout.setVisibility(View.GONE);
        }

        // Set the password fields to blank
        _password.setText("");
        _confirmPassword.setText("");

        _email.setText(user.getEmail());
        _firstName.setText(user.getFirstname());
        _lastName.setText(user.getLastname());
        _city.setText(user.getCity());
        _state.setText(user.getState());
        _zip.setText(user.getZip());
        _country.setText(user.getCountry());
        _phoneCountryCode.setText(user.getPhoneCountryCode());
        _phoneNumber.setText(user.getPhone());

        // Update our title if we are editing an account
        if (_user != null) {
            getActivity().setTitle(R.string.account_details);
        }
    }

    private void signUpClicked() {
        AylaUser newUser = new AylaUser();
        AylaError error = updateUser(newUser);
        if (error != null) {
            _confirmPassword.setText("");
            _password.selectAll();
            Snackbar.make(_password, error.getMessage(), Snackbar.LENGTH_SHORT).show();
            return;
        }

        AylaEmailTemplate template = new AylaEmailTemplate();

        String emailTemplateId = getResources().getString(R.string.confirmation_template_id);
        String emailSubject = getResources().getString(R.string.confirmation_email_subject);

        template.setEmailTemplateId(emailTemplateId);
        template.setEmailSubject(emailSubject);
        String language = Locale.getDefault().getDisplayLanguage();
        
        if (language.equalsIgnoreCase("हिन्दी")) {
            template.setEmailTemplateId("template_hindi");
            template.setEmailSubject("आभा में आपका स्वागत है");
        }

        AylaNetworks.sharedInstance().getLoginManager().signUp(newUser, template,
                new Response.Listener<AylaUser>() {
                    @Override
                    public void onResponse(AylaUser response) {
                        Snackbar.make(_button, R.string.sign_up_success, Snackbar.LENGTH_LONG)
                                .show();
                        signUpFinish();
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(AylaError error) {
                        Snackbar.make(_button, error.getMessage(), Snackbar.LENGTH_LONG).show();
                    }
                });
    }

    private void signUpFinish() {
        // Finish signup view and show Login view
        Activity activity = getActivity();
        if (activity != null) {
            View v = activity.findViewById(R.id.layout_account_details);
            if (v != null) {
                v.setVisibility(View.GONE);
            }
            v = activity.findViewById(R.id.layout_sign_in);
            if (v != null) {
                v.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setUpdateEmailUI() {
        _updateEmailLayout.setVisibility(View.VISIBLE);
        _updateEmailButton.setVisibility(View.VISIBLE);
        _email.setEnabled(false);

    }
}

