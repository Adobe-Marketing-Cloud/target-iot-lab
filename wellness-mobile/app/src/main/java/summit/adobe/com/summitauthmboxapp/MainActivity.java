package summit.adobe.com.summitauthmboxapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

  private static final String MBOX_NAME = "wellnessMobile";
  private static final long DELAY = 5;
  private static final long DELTA_TIME_MILLIS = 100;
  private static final int WINDOW_SIZE = (int) (TimeUnit.SECONDS.toMillis(5) / DELTA_TIME_MILLIS);


  private long lastUpdate = 0;
  private float lastX, lastY, lastZ;
  private DescriptiveStatistics descriptiveStatistics;
  private SensorManager mSensorManager;
  private Sensor mSenAccelerometer;
  private VelocityTracker mVelocityTracker = null;
  private EditText mProfileId;
  private EditText mUserName;
  private TextView debugArea;
  private ScrollView scrollView;
  private ImageView mImageView;
  private Button mSubmitButton;
  private Button mStopButton;
  private volatile String profileId;
  private volatile String userName;
  private TNTRequestService tntRequestService;
  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    descriptiveStatistics = new DescriptiveStatistics(WINDOW_SIZE);

    debugArea = (TextView) findViewById(R.id.debug);

    mImageView = (ImageView) findViewById(R.id.imageView);

    mUserName = (EditText) findViewById(R.id.userName);

    mProfileId = (EditText) findViewById(R.id.profileId);

    mSubmitButton = (Button) findViewById(R.id.submit);

    scrollView = (ScrollView) findViewById(R.id.scrollView);

    mSubmitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String thirdPartyId = mProfileId.getText().toString();
        final String name = mUserName.getText().toString();
        if (StringUtils.isBlank(thirdPartyId) || StringUtils.isBlank(name)) {
          Toast toast = Toast.makeText(MainActivity.this, "Profile UserId and User Name cannot be empty",
            Toast.LENGTH_SHORT);
          toast.show();
          return;
        }

        userName = name;

        if (!StringUtils.equals(profileId, thirdPartyId)) {
          profileId = thirdPartyId;
          tntRequestService = new TNTRequestService(profileId, MainActivity.this);
        }

        if (executorService != null) {
          executorService.shutdown();
        }
        executorService = null;
        executorService = Executors.newSingleThreadScheduledExecutor();

        executorService.scheduleAtFixedRate(new Runnable() {
          @Override
          public void run() {
            if (StringUtils.isBlank(profileId)) {
              return;
            }

            Map<String, String> profileParameters = new HashMap<>();
            if (mVelocityTracker != null) {
              float xVelocity = mVelocityTracker.getXVelocity();
              float yVelocity = mVelocityTracker.getYVelocity();
              double velocity = Math.sqrt(xVelocity * xVelocity + yVelocity * yVelocity);

              if (velocity < 50) {
                profileParameters.put("activeState", "couchpotato");
              } else if (velocity < 900) {
                profileParameters.put("activeState", "fitnessfreak");
              } else {
                profileParameters.put("activeState", "marathoner");
              }
            } else {
              double mean = descriptiveStatistics.getMean();
              if (mean < 500) {
                profileParameters.put("activeState", "couchpotato");
              } else if (mean < 1000) {
                profileParameters.put("activeState", "fitnessfreak");
              } else {
                profileParameters.put("activeState", "marathoner");
              }
            }

            Map<String, String> mboxParameters = new HashMap<>();
            mboxParameters.put("name", userName);

            setOffer(mboxParameters, profileParameters);

          }
        }, DELAY, DELAY, TimeUnit.SECONDS);
      }
    });

    mStopButton = (Button) findViewById(R.id.stopButton);
    mStopButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if (executorService != null) {
          executorService.shutdown();
        }
        executorService = null;
      }
    });

    mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
    mSenAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    mSensorManager.registerListener(this, mSenAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();

    switch (action) {
      case MotionEvent.ACTION_DOWN:
        if (mVelocityTracker == null) {
          mVelocityTracker = VelocityTracker.obtain();
        } else {
          mVelocityTracker.clear();
        }
        mVelocityTracker.addMovement(event);
        break;
      case MotionEvent.ACTION_MOVE:
        mVelocityTracker.addMovement(event);
        mVelocityTracker.computeCurrentVelocity(1000);
        break;
      case MotionEvent.ACTION_UP:
        mVelocityTracker = null;
        break;
      case MotionEvent.ACTION_CANCEL:
        mVelocityTracker.recycle();
        break;
    }
    return true;
  }

  @Override
  public void onSensorChanged(SensorEvent sensorEvent) {
    Sensor mySensor = sensorEvent.sensor;

    if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
      float x = sensorEvent.values[0];
      float y = sensorEvent.values[1];
      float z = sensorEvent.values[2];

      long curTime = System.currentTimeMillis();

      if ((curTime - lastUpdate) > DELTA_TIME_MILLIS) {
        long diffTime = (curTime - lastUpdate);
        lastUpdate = curTime;

        float speed = Math.abs(x + y + z - lastX - lastY - lastZ) / diffTime * 10000;
        descriptiveStatistics.addValue(speed);

        lastX = x;
        lastY = y;
        lastZ = z;
      }
    }
  }

  @Override
  public void onAccuracyChanged(Sensor sensor, int accuracy) {

  }

  private void setOffer(Map<String, String> mboxParameters, Map<String, String> profileParameters) {
    try {
      String content = tntRequestService.getContent(MBOX_NAME, mboxParameters, profileParameters);
      if (StringUtils.isBlank(content)) {
        return;
      }

      displayImageFromUrl(content, mImageView);
      tntRequestService.updateProfile(profileParameters);
    } catch (final Exception e) {
      debug("An exception occurred. Message: " + e.getMessage());
    }
  }

  void debug(final String message) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        debugArea.append(new Date().toString() + ": \n");
        debugArea.append(message);
        debugArea.append("\n");
        debugArea.append("\n");
        scrollView.fullScroll(ScrollView.FOCUS_DOWN);
      }
    });
  }

  private void displayImageFromUrl(String urlValue, final ImageView imageView) throws IOException {
    URL url = new URL(urlValue);
    final Bitmap bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());

    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        imageView.setImageBitmap(bmp);
      }
    });
  }

}
