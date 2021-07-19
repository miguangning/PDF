package com.mixiao.pdf;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.InputStream;

/**
 * Created by Mi on 7/19/21
 */
public class Tools {
    public static void getPDFBytes(Context context, CallBack<byte[]> callBack) {
        byte[] bytes = assetsFileToBytes(context, "pdf.pdf");
        if (bytes.length == 0) {
            callBack.onError("bytes is empty");
            return;
        }
        callBack.onSuccess(bytes);
    }

    public static byte[] assetsFileToBytes(Context context, String fileName) {
        byte[] buffer = null;
        AssetManager manager = context.getAssets();
        try {
            InputStream inputStream = null;
            inputStream = manager.open(fileName);
            int length = inputStream.available();
            buffer = new byte[length];
            inputStream.read(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return buffer;
    }
}
