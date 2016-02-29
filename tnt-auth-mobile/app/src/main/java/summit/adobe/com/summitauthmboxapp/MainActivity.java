package summit.adobe.com.summitauthmboxapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

  private static final String MBOX_NAME = "summit-auth-mobile";
  private static final long DELAY = 5;

  private VelocityTracker mVelocityTracker = null;
  private EditText mAuthenticatedId;
  private ImageView mImageView;
  private Button submitButton;
  private volatile String authenticatedId;
  private TNTRequestService tntRequestService;
  private ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    mImageView = (ImageView) findViewById(R.id.imageView);

    mAuthenticatedId = (EditText) findViewById(R.id.authenticateId);

    submitButton = (Button) findViewById(R.id.submit);

    submitButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String authenticatedIdValue = mAuthenticatedId.getText().toString();
        if (StringUtils.isBlank(authenticatedIdValue)) {
          Toast toast = Toast.makeText(MainActivity.this, "Authenticated Id cannot be empty", Toast.LENGTH_SHORT);
          toast.show();
          return;
        }

        if (!StringUtils.equals(authenticatedId, authenticatedIdValue)) {
          authenticatedId = authenticatedIdValue;
          tntRequestService = new TNTRequestService(authenticatedId);
          if (executorService != null) {
            executorService.shutdown();
          }
          executorService = Executors.newSingleThreadScheduledExecutor();

          executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
              if (StringUtils.isBlank(authenticatedId)) {
                return;
              }

              Map<String, String> profileParameters = new HashMap<>();
              if (mVelocityTracker != null) {
                float xVelocity = mVelocityTracker.getXVelocity();
                float yVelocity = mVelocityTracker.getYVelocity();
                double velocity = Math.sqrt(xVelocity * xVelocity + yVelocity * yVelocity);

                profileParameters.put("velocity", Double.toString(velocity));
              } else {
                profileParameters.put("velocity", "0");
              }

              setOffer(profileParameters);

            }
          }, DELAY, DELAY, TimeUnit.SECONDS);
        }
      }
    });

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

  private void setOffer(Map<String, String> profileParameters) {
    try {
      String content = tntRequestService.getContent(MBOX_NAME, profileParameters);
      displayImageFromUrl(content, mImageView);
    } catch (final Exception e) {
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast toast = Toast.makeText(MainActivity.this, "An exception occurred. Message: " + e.getMessage(),
            Toast.LENGTH_SHORT);
          toast.show();
        }
      });
    }
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
