/*
    Simple PDF Viewer for Android
    Copyright (C) 2016  Lingjun Jiang, Zhuoxue Jin

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation, either version 3 of the
    License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

package org.apache.cordova.oinio;

import java.io.File;

import android.content.Intent;

import android.net.Uri;
import android.util.Log;

import com.artifex.mupdfdemo.MuPDFActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;


public class SimplePdfViewer extends CordovaPlugin {
    private static final String LOG_TAG = "SimplePdfViewer";

    @Override
    public void onPause(boolean multitasking) {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("openPDF")) {
            openPDF(args, callbackContext);
            return true;
        } 

        //callbackContext.success();
        return false;
    }

    private void openPDF(final CordovaArgs args, final CallbackContext callbackContext) {
        this.cordova.getActivity().runOnUiThread(new Runnable() {
                public void run() {
                    Log.d(LOG_TAG, "open pdf file");
                    String localFilePath = args.getString(0);

                    Uri pdfUri = getUriForArg(localFilePath);

                    CordovaResourceApi resourceApi = webView.getResourceApi();

                    File tempFile = resourceApi.mapUriToFile(pdfUri);
                    if (tempFile == null || !tempFile.exists()) {
                        String errorMessage = "PDF file does not exist";
                        callbackContext.error(errorMessage);
                        Log.e(LOG_TAG, errorMessage);
                        return;
                    }

                    Uri uri = Uri.parse(localFilePath);
                    Intent intent = new Intent(cordova.getActivity(), MuPDFActivity.class);
                    intent.setAction(Intent.ACTION_VIEW);
                    intent.setData(pdfUri);

                    cordova.getActivity().startActivity(intent);

                    callbackContext.success();
                }
            });
    }

    private Uri getUriForArg(String arg) {
        CordovaResourceApi resourceApi = webView.getResourceApi();
        Uri tmpTarget = Uri.parse(arg);
        return resourceApi.remapUri(
                tmpTarget.getScheme() != null ? tmpTarget : Uri.fromFile(new File(arg)));
    }


}
