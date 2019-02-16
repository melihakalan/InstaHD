package relaxplus.instahd;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.common.net.UrlEscapers;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;
import com.startapp.android.publish.adsCommon.Ad;
import com.startapp.android.publish.adsCommon.StartAppAd;
import com.startapp.android.publish.adsCommon.StartAppSDK;
import com.startapp.android.publish.adsCommon.adListeners.AdEventListener;
import com.turbomanage.httpclient.AsyncCallback;
import com.turbomanage.httpclient.HttpResponse;
import com.turbomanage.httpclient.android.AndroidHttpClient;
import com.wang.avi.AVLoadingIndicatorView;

import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    public static Typeface tfRobotoCondensed = Typeface.create("sans-serif-condensed", Typeface.NORMAL);
    public static Typeface tfRobotoCondensedBold = Typeface.create("sans-serif-condensed", Typeface.BOLD);

    private EditText edit_username;
    private AVLoadingIndicatorView v_loading;
    private ImageView iv_photo;
    private RelativeLayout rl_icons;
    private String image_username;
    private String image_url;

    private StartAppAd startAppAd = new StartAppAd(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        StartAppSDK.init(this, "209942858", true);
        StartAppAd.disableSplash();
        setContentView(R.layout.activity_main);

        edit_username = findViewById(R.id.edit_username);
        v_loading = findViewById(R.id.v_loading);
        iv_photo = findViewById(R.id.iv_photo);
        rl_icons = findViewById(R.id.rl_icons);

        edit_username.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if ((actionId & EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_DONE) {
                    startRetrieveProcess(null);
                    return true;
                }

                return false;
            }
        });

        iv_photo.setOnTouchListener(zoomTouch);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        startAppAd.loadAd(new AdEventListener() {
            @Override
            public void onReceiveAd(Ad ad) {
                startAppAd.showAd();
            }

            @Override
            public void onFailedToReceiveAd(Ad ad) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        StartAppAd.onBackPressed(this);
        super.onBackPressed();
    }

    public void startRetrieveProcess(View v) {
        String username = edit_username.getText().toString().trim();
        if (!username.isEmpty()) {
            if (username.charAt(0) == '@')
                username = username.substring(1);

            image_username = username;

            iv_photo.setVisibility(View.GONE);
            iv_photo.setImageResource(0);
            iv_photo.setScaleType(ImageView.ScaleType.FIT_CENTER);
            rl_icons.setVisibility(View.GONE);
            v_loading.setVisibility(View.VISIBLE);

            final AndroidHttpClient httpClient = new AndroidHttpClient("https://www.instagram.com");
            httpClient.setMaxRetries(2);
            httpClient.setConnectionTimeout(10000);
            httpClient.setReadTimeout(10000);
            String path = UrlEscapers.urlFragmentEscaper().escape("/" + username + "/");
            httpClient.get(path, null, new AsyncCallback() {
                @Override
                public void onComplete(HttpResponse httpResponse) {
                    String response = httpResponse.getBodyAsString();
                    if (response == null || response.length() == 0)
                        onError(new Exception());

                    int spos = response.indexOf("profilePage_");
                    if (spos == -1) {
                        v_loading.setVisibility(View.GONE);
                        iv_photo.setVisibility(View.VISIBLE);

                        SnackbarManager.show(
                                Snackbar.with(MainActivity.this)
                                        .text("Invalid user!")
                                        .textColor(Color.rgb(0xFA, 0xFA, 0xFA))
                                        .textTypeface(tfRobotoCondensed)
                                        .color(Color.rgb(0xFE, 0x2E, 0x2E)));

                        return;
                    }
                    int epos = response.indexOf(",", spos) - 1;

                    String userid = response.substring(spos + 12, epos);
                    getImage(userid);
                }

                @Override
                public void onError(Exception e) {
                    v_loading.setVisibility(View.GONE);
                    iv_photo.setVisibility(View.VISIBLE);

                    SnackbarManager.show(
                            Snackbar.with(MainActivity.this)
                                    .text("Failed retrieving photo from Instagram. (-1)")
                                    .textColor(Color.rgb(0xFA, 0xFA, 0xFA))
                                    .textTypeface(tfRobotoCondensed)
                                    .color(Color.rgb(0xFE, 0x2E, 0x2E)));

                    e.printStackTrace();
                }
            });
        }
    }

    private void getImage(final String userid) {
        final AndroidHttpClient httpClient = new AndroidHttpClient("https://i.instagram.com");
        httpClient.setMaxRetries(2);
        httpClient.setConnectionTimeout(10000);
        httpClient.setReadTimeout(10000);
        String path = UrlEscapers.urlFragmentEscaper().escape("/api/v1/users/" + userid + "/info/");
        httpClient.get(path, null, new AsyncCallback() {
            @Override
            public void onComplete(HttpResponse httpResponse) {
                String response = httpResponse.getBodyAsString();

                try {
                    JSONObject info = new JSONObject(response);
                    JSONObject user = new JSONObject(info.getString("user"));
                    JSONObject pic = new JSONObject(user.getString("hd_profile_pic_url_info"));
                    String photo = pic.getString("url");
                    int w = pic.getInt("width");
                    int h = pic.getInt("height");

                    image_url = photo;

                    Picasso.get().load(photo).resize(w, h).into(iv_photo, new Callback() {
                        @Override
                        public void onSuccess() {
                            v_loading.setVisibility(View.GONE);
                            iv_photo.setVisibility(View.VISIBLE);
                            rl_icons.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Exception e) {
                            v_loading.setVisibility(View.GONE);
                            iv_photo.setVisibility(View.VISIBLE);

                            SnackbarManager.show(
                                    Snackbar.with(MainActivity.this)
                                            .text("Failed retrieving photo from Instagram. (-4)")
                                            .textColor(Color.rgb(0xFA, 0xFA, 0xFA))
                                            .textTypeface(tfRobotoCondensed)
                                            .color(Color.rgb(0xFE, 0x2E, 0x2E)));
                        }
                    });

                } catch (Exception e) {
                    SnackbarManager.show(
                            Snackbar.with(MainActivity.this)
                                    .text("Failed retrieving photo from Instagram. (-3)")
                                    .textColor(Color.rgb(0xFA, 0xFA, 0xFA))
                                    .textTypeface(tfRobotoCondensed)
                                    .color(Color.rgb(0xFE, 0x2E, 0x2E)));

                    e.printStackTrace();
                }
            }

            @Override
            public void onError(Exception e) {
                v_loading.setVisibility(View.GONE);
                iv_photo.setVisibility(View.VISIBLE);

                SnackbarManager.show(
                        Snackbar.with(MainActivity.this)
                                .text("Failed retrieving photo from Instagram. (-2)")
                                .textColor(Color.rgb(0xFA, 0xFA, 0xFA))
                                .textTypeface(tfRobotoCondensed)
                                .color(Color.rgb(0xFE, 0x2E, 0x2E)));

                e.printStackTrace();
            }
        });
    }

    private View.OnTouchListener zoomTouch = new View.OnTouchListener() {
        private static final float MIN_ZOOM = 1f, MAX_ZOOM = 1f;

        Matrix matrix = new Matrix();
        Matrix savedMatrix = new Matrix();

        static final int NONE = 0;
        static final int DRAG = 1;
        static final int ZOOM = 2;
        int mode = NONE;

        PointF start = new PointF();
        PointF mid = new PointF();
        float oldDist = 1f;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            ImageView view = (ImageView) v;
            if (view.getImageMatrix() == null)
                return false;

            view.setScaleType(ImageView.ScaleType.MATRIX);
            float scale;

            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    matrix.set(view.getImageMatrix());
                    savedMatrix.set(matrix);
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;

                case MotionEvent.ACTION_UP:

                case MotionEvent.ACTION_POINTER_UP:

                    mode = NONE;
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:

                    oldDist = spacing(event);
                    if (oldDist > 5f) {
                        savedMatrix.set(matrix);
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:

                    if (mode == DRAG) {
                        matrix.set(savedMatrix);
                        matrix.postTranslate(event.getX() - start.x, event.getY() - start.y);
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 5f) {
                            matrix.set(savedMatrix);
                            scale = newDist / oldDist;
                            matrix.postScale(scale, scale, mid.x, mid.y);
                        }
                    }
                    break;
            }

            view.setImageMatrix(matrix);
            return true;
        }

        private float spacing(MotionEvent event) {
            float x = event.getX(0) - event.getX(1);
            float y = event.getY(0) - event.getY(1);
            return (float) Math.sqrt(x * x + y * y);
        }

        private void midPoint(PointF point, MotionEvent event) {
            float x = event.getX(0) + event.getX(1);
            float y = event.getY(0) + event.getY(1);
            point.set(x / 2, y / 2);
        }
    };

    public void gallery(View v) {
        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
    }

    public void savePhoto(View v) {
        if (image_username.isEmpty() || image_url.isEmpty())
            return;
        Saver.save(getApplicationContext(), image_username, image_url, MainActivity.this);
    }
}
