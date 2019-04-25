/*
 * Copyright (c) 2019. MotionEyeClient by Developer From Jokela, All Rights Reserved.
 * Licenced with MIT;
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.developerfromjokela.motioneyeclient.ui.activities;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.developerfromjokela.motioneyeclient.R;
import com.developerfromjokela.motioneyeclient.api.MotionEyeHelper;
import com.developerfromjokela.motioneyeclient.classes.Camera;
import com.developerfromjokela.motioneyeclient.classes.CameraImage;
import com.developerfromjokela.motioneyeclient.classes.Device;
import com.developerfromjokela.motioneyeclient.database.Source;
import com.developerfromjokela.motioneyeclient.other.Utils;
import com.google.gson.Gson;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static android.app.DownloadManager.Request.NETWORK_MOBILE;
import static android.app.DownloadManager.Request.NETWORK_WIFI;


public class FullCameraViewer extends Activity {

    private Source source;
    private boolean loaded = false;
    private boolean attached = true;
    private TextView status;
    private Runnable timerRunnable;
    private Handler timerHandler = new Handler();

    private View mContentView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);


        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_full_camera_viewer);
        ImageView cameraImage = findViewById(R.id.cameraFullImage);
        LinearLayout loadingBar = findViewById(R.id.progressBar);
        RelativeLayout cameraFrame = findViewById(R.id.cameraFrame);
        LinearLayout bottomBar = findViewById(R.id.bottomBar);
        LinearLayout topBar = findViewById(R.id.topBar);
        TextView fps = findViewById(R.id.cameraFPS);
        ProgressBar loadingCircle = findViewById(R.id.progressBar2);
        TextView cameraName = findViewById(R.id.cameraName);

        status = findViewById(R.id.status);

        source = new Source(this);

        Intent intent = getIntent();
        if (intent.getExtras() != null) {

            String ID = intent.getStringExtra("DeviceId");
            Device device = null;
            try {
                device = source.get(ID);
                final Camera camera = new Gson().fromJson(intent.getStringExtra("Camera"), Camera.class);
                MotionEyeHelper helper = new MotionEyeHelper();
                helper.setUsername(device.getUser().getUsername());
                try {
                    helper.setPasswordHash(device.getUser().getPassword());
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }
                String serverurl;
                String cameraId = camera.getId();

                if (device.getDdnsURL().length() > 5) {
                    if ((Utils.getNetworkType(FullCameraViewer.this)) == NETWORK_MOBILE) {
                        serverurl = device.getDDNSUrlCombo();
                    } else if (device.getWlan().networkId == Utils.getCurrentWifiNetworkId(FullCameraViewer.this)) {
                        serverurl = device.getDeviceUrlCombo();

                    } else {
                        serverurl = device.getDDNSUrlCombo();

                    }
                } else {
                    serverurl = device.getDeviceUrlCombo();

                }
                String baseurl;
                if (!serverurl.contains("://"))
                    baseurl = removeSlash("http://" + serverurl);
                else
                    baseurl = removeSlash(serverurl);


                int framerate = Integer.valueOf(camera.getFramerate());
                cameraName.setText(camera.getName());

                cameraImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if ((topBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE) == View.VISIBLE) {
                            topBar.setVisibility(topBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                            Animation slide = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bar);
                            Animation slide_bottom = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_in_bottom_bar);

                            topBar.startAnimation(slide);
                            bottomBar.setVisibility(bottomBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                            bottomBar.startAnimation(slide_bottom);

                        } else {
                            topBar.setVisibility(topBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                            Animation slide = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_bar);
                            Animation slide_bottom = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.slide_out_bottom_bar);

                            topBar.startAnimation(slide);
                            bottomBar.setVisibility(bottomBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
                            bottomBar.startAnimation(slide_bottom);
                        }
                    }
                });

                Device finalDevice = device;
                timerRunnable = new Runnable() {
                    @Override
                    public void run() {
                        String serverurl;
                        String cameraId = camera.getId();

                        if (finalDevice.getDdnsURL().length() > 5) {
                            if ((Utils.getNetworkType(FullCameraViewer.this)) == NETWORK_MOBILE) {
                                serverurl = finalDevice.getDDNSUrlCombo();
                            } else if (finalDevice.getWlan().networkId == Utils.getCurrentWifiNetworkId(FullCameraViewer.this)) {
                                serverurl = finalDevice.getDeviceUrlCombo();

                            } else {
                                serverurl = finalDevice.getDDNSUrlCombo();

                            }
                        } else {
                            serverurl = finalDevice.getDeviceUrlCombo();

                        }
                        String baseurl;
                        if (!serverurl.contains("://"))
                            baseurl = removeSlash("http://" + serverurl);
                        else
                            baseurl = removeSlash(serverurl);

                        String url = baseurl + "/picture/" + cameraId + "/current?_=" + String.valueOf(new Date().getTime());
                        url = helper.addAuthParams("GET", url, "");
                        String finalUrl = url;
                        new DownloadImageFromInternet(cameraImage, loadingBar, fps, status, loadingCircle, camera, cameraFrame).execute(finalUrl);


                    }
                };
                String url = baseurl + "/picture/" + cameraId + "/current?_=" + String.valueOf(new Date().getTime());
                url = helper.addAuthParams("GET", url, "");
                String finalUrl = url;
                new DownloadImageFromInternet(cameraImage, loadingBar, fps, status, loadingCircle, camera, cameraFrame).execute(finalUrl);


            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            finish();
        }


        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
    }


    private class DownloadImageFromInternet extends AsyncTask<String, Void, CameraImage> {
        ImageView imageView;
        LinearLayout progressBar;
        TextView fps, status;
        Camera camera;
        ProgressBar loadingCircle;
        RelativeLayout cameraFrame;

        public DownloadImageFromInternet(ImageView imageView, LinearLayout progressBar, TextView fps, TextView status, ProgressBar loadingCircle, Camera camera, RelativeLayout cameraFrame) {
            this.imageView = imageView;
            this.progressBar = progressBar;
            this.fps = fps;
            this.camera = camera;
            this.status = status;
            this.cameraFrame = cameraFrame;
            this.loadingCircle = loadingCircle;
        }

        protected void onPreExecute() {

            status.setText(R.string.loading);
            loadingCircle.setVisibility(View.VISIBLE);
            if (!loaded) {
                progressBar.setVisibility(View.VISIBLE);
            }
        }

        protected CameraImage doInBackground(String... urls) {
            String imageURL = urls[0];


            try {
                URL url = new URL(imageURL);
                URLConnection connection = url.openConnection();
                Map<String, List<String>> fps = connection.getHeaderFields();
                String humanReadableFPS = "0";
                InputStream in = url.openStream();
                final Bitmap decoded = BitmapFactory.decodeStream(in);
                in.close();
                for (Map.Entry<String, List<String>> key : fps.entrySet()) {
                    for (String string : key.getValue()) {
                        if (string.contains("capture_fps")) {
                            int ii = 0;

                            double d = Double.parseDouble(string.split("capture_fps_" + camera.getId() + "=")[1].split(";")[0].trim());
                            ii = (int) d;
                            humanReadableFPS = String.valueOf(Math.round(ii));
                            return new CameraImage(humanReadableFPS, decoded, true);

                        }

                    }
                }


            } catch (Exception e) {
                Log.e("Error Message", e.getMessage());
                e.printStackTrace();
                return new CameraImage(false, e.getMessage());
            }
            return null;

        }

        protected void onPostExecute(CameraImage result) {
            if (result.isSuccessful()) {
                if (!loaded) {
                    progressBar.animate()
                            .translationY(progressBar.getHeight())
                            .alpha(0.0f)
                            .setDuration(300)
                            .setListener(new AnimatorListenerAdapter() {
                                @Override
                                public void onAnimationEnd(Animator animation) {
                                    super.onAnimationEnd(animation);
                                    progressBar.setVisibility(View.GONE);
                                    cameraFrame.setVisibility(View.VISIBLE);
                                }
                            });

                    loaded = true;
                }
                imageView.setImageBitmap(result.getBitmap());

                fps.setText(result.getFps() + " fps");


            } else {
                loaded = false;
                loadingCircle.setVisibility(View.GONE);
                status.setText(result.getErrorString());
            }

            int framerate = Integer.valueOf(camera.getFramerate());

            if (attached) {
                timerHandler.postDelayed(timerRunnable, 1000 / framerate); //Start timer after 1 sec

            }

        }


    }


    private static String removeSlash(String url) {
        if (!url.endsWith("/"))
            return url;
        String[] parts = url.split("/");

        return parts[0];
    }

    @Override
    public void onResume() {
        super.onResume();
        attached = true;
    }

    @Override
    public void onPause() {
        super.onPause();
        attached = false;


    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        attached = false;
    }


    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public int getNetworkType(Context context) {
        if (!isNetworkAvailable())
            return -1;
        ConnectivityManager conMan = (ConnectivityManager) context.
                getSystemService(Context.CONNECTIVITY_SERVICE);

        //mobile
        NetworkInfo.State mobile = conMan.getNetworkInfo(0).getState();
        //wifi
        NetworkInfo.State wifi = conMan.getNetworkInfo(1).getState();

        int result = 0;

        if (mobile == NetworkInfo.State.CONNECTED || mobile == NetworkInfo.State.CONNECTING) {
            result |= NETWORK_MOBILE;
        }

        if (wifi == NetworkInfo.State.CONNECTED || wifi == NetworkInfo.State.CONNECTING) {
            result |= NETWORK_WIFI;
        }

        return result;
    }

}