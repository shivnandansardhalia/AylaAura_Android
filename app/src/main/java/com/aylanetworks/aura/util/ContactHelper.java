package com.aylanetworks.aura.util;
/*
 * AylaSDK
 *
 * Copyright 2015 Ayla Networks, all rights reserved
 */

import android.util.Log;

import com.aylanetworks.aylasdk.AylaContact;

import java.util.ArrayList;
import java.util.List;

public class ContactHelper {
    private static final String LOG_TAG = "ContactHelper";
    private static List<AylaContact> _contacts = new ArrayList<>();
    public static void setContactList(List<AylaContact> aylaContacts){
        _contacts=aylaContacts;
    }
    public static AylaContact getContactByID(Integer id) {
        if(_contacts != null) {
            for (AylaContact contact : _contacts) {
                if (contact.getId().equals(id)) {
                    return contact;
                }
            }
        } else {
            Log.d(LOG_TAG, "_contacts is null in getContactByID");
        }
        Log.d(LOG_TAG, "getContactByID is null for Id " +id);
        return null;
    }
    public static List<AylaContact> getContacts() {
        return _contacts;
    }

}
