package com.aylanetworks.aura.util;

/*
 * Aura_Android
 *
 * Copyright 2016 Ayla Networks, all rights reserved
 */

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.security.MessageDigest;

public class FilePropertyPreviewUtil {
    static final String TAG = "FilePropertyPreviewUtil";

    /**
     * Generate file name from URL, basically for downloading/caching FileProperty's datapoints.
     */
    public static String generateFileName(String url) {
        return md5(url);
    }

    /***
     * Get MD5 digest string of the given string.
     */
    public static String md5(String original) {
        String resultString = null;
        try {
            byte[] bytesOfMessage = original.getBytes("UTF-8");
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] thedigest = md.digest(bytesOfMessage);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < thedigest.length; ++i) {
                sb.append(String.format("%02x", thedigest[i]));
            }
            resultString = sb.toString();
        } catch (Throwable e) {
            Log.e(TAG, e.getLocalizedMessage());
        }

        return resultString;
    }

    /**
     * Try to get content type of a File. For instance, if the file is a jpeg image, will return `image/jpeg`.
     * @param file the file.
     * @return If success return content type of the file, otherwise return null.
     */
    public static String guessContentType(File file) {
        String fileType = null;
        InputStream stream = null;
        try {
            stream = new FileInputStream(file);
            // this method can only guess very few types, exclude images
            fileType = URLConnection.guessContentTypeFromStream(stream);

            // image check
            if (fileType == null) {
                BitmapFactory.Options opt = new BitmapFactory.Options();
                opt.inJustDecodeBounds = true;
                BitmapFactory.decodeStream(stream, null, opt);
                fileType = opt.outMimeType;
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileType;
    }
}
