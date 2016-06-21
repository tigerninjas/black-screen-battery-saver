package busu.com.blackscreenbatterysaver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.squareup.seismic.ShakeDetector;

import java.util.HashMap;

/**
 * Created by adibusu on 5/14/16.
 */
public class StarterActivity extends AppCompatActivity {

    public final static String TAG = "BSBS";

    private Preferences mPrefs;

    private Button mBtnStartStop;
    private RadioGroup mRgPos, mRgPer, mRgShakeSens;
    private TextView mStatus;
    private CheckBox mChkStartOnSh, mChkStopOnSh;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && TheService.BROADCAST.equals(intent.getAction())) {
                serviceStatusChanged(
                        (TheService.State) intent.getSerializableExtra(TheService.BROADCAST_CURRENT_STATE),
                        (TheService.State) intent.getSerializableExtra(TheService.BROADCAST_OLD_STATE));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mPrefs = new Preferences(this);
        initMapIds();

        setContentView(R.layout.starter);

        CheckBox cbClose = (CheckBox) findViewById(R.id.sChkClose);
        cbClose.setChecked(mPrefs.hasToCloseAfterButtonPressed());
        cbClose.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mPrefs.setHasToCloseAfterButtonPressed(isChecked);
            }
        });

        mBtnStartStop = (Button) findViewById(R.id.sBtnStartStop);
        mBtnStartStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (TheService.state == TheService.State.ACTIVE) {
                    startTheService(NotificationsHelper.ACTION_STOP);
                } else {
                    savePrefsFromComponents();
                    checkDrawOverlayPermission();
                }
            }
        });

        mRgPer = (RadioGroup) findViewById(R.id.sRgPercentage);
        mRgPer.check(mapIds.get(mPrefs.getHoleHeightPercentage()));

        mRgPos = (RadioGroup) findViewById(R.id.sRgPosition);
        mRgPos.check(mapIds.get(mPrefs.getHolePosition()));

        mRgShakeSens = (RadioGroup) findViewById(R.id.sRgShkSens);
        mRgShakeSens.check(mapIds.get(mPrefs.getShakeSensitivity()));

        mChkStartOnSh = (CheckBox) findViewById(R.id.sChkShakeStart);
        mChkStartOnSh.setChecked(mPrefs.hasToStartOnShake());

        mChkStopOnSh = (CheckBox) findViewById(R.id.sChkShakeStop);
        mChkStopOnSh.setChecked(mPrefs.hasToStopOnShake());

        Button btnApply = (Button) findViewById(R.id.sBtnApply);
        btnApply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                savePrefsFromComponents();
                if (TheService.state != TheService.State.STOPPED) {
                    startTheService(TheService.ACTION_READPREFS);
                }
            }
        });

        mStatus = (TextView) findViewById(R.id.sStatus);

        serviceStatusChanged(TheService.state, null);
    }

    private void savePrefsFromComponents() {
        mPrefs.setHolePosition(mapIds.get(mRgPos.getCheckedRadioButtonId()));
        mPrefs.setHoleHeightPercentage(mapIds.get(mRgPer.getCheckedRadioButtonId()));
        mPrefs.setShakeSensitivity(mapIds.get(mRgShakeSens.getCheckedRadioButtonId()));
        mPrefs.setStartOnShake(mChkStartOnSh.isChecked());
        mPrefs.setStopOnShake(mChkStopOnSh.isChecked());
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, new IntentFilter(TheService.BROADCAST));
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    public final static int REQUEST_CODE = 1;

    public void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(StarterActivity.this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE);
        } else {
            startTheService();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(StarterActivity.this)) {
                startTheService();
            } else {
                Snackbar.make(mBtnStartStop, R.string.pleaseEnableOverlay, Snackbar.LENGTH_LONG).show();
            }
        }
    }

    private void startTheService() {
        startTheService(NotificationsHelper.ACTION_START);
    }

    private void startTheService(String action) {
        startService(new Intent(StarterActivity.this, TheService.class).setAction(action));
        if (mPrefs.hasToCloseAfterButtonPressed()) {
            finish();
        }
    }

    private void serviceStatusChanged(TheService.State currentState, TheService.State oldState) {
        final boolean isStarted = (TheService.State.ACTIVE == currentState);
        //
        mBtnStartStop.setText(isStarted ? R.string.btn_stop : R.string.btn_start);
        mStatus.setText(isStarted ? R.string.status_started : R.string.status_stopped);
        mStatus.setTextColor(isStarted ? Color.GREEN : Color.RED);
    }

    private HashMap<Integer, Integer> mapIds;

    private void initMapIds() {
        mapIds = new HashMap<>((3 + 3 + 2) * 2 + 1);
        mapIds.put(R.id.sRbPerHalf, Preferences.HOLE_HEIGHT_PERCENTAGE_1P2);
        mapIds.put(R.id.sRbPerThird, Preferences.HOLE_HEIGHT_PERCENTAGE_1P3);
        mapIds.put(Preferences.HOLE_HEIGHT_PERCENTAGE_1P3, R.id.sRbPerThird);
        mapIds.put(Preferences.HOLE_HEIGHT_PERCENTAGE_1P2, R.id.sRbPerHalf);
        mapIds.put(R.id.sRbPosBottom, ViewPortView.BOTTOM);
        mapIds.put(ViewPortView.BOTTOM, R.id.sRbPosBottom);
        mapIds.put(R.id.sRbPosCenter, ViewPortView.CENTER);
        mapIds.put(ViewPortView.CENTER, R.id.sRbPosCenter);
        mapIds.put(R.id.sRbPosTop, ViewPortView.TOP);
        mapIds.put(ViewPortView.TOP, R.id.sRbPosTop);
        mapIds.put(R.id.sRbSenLow, ShakeDetector.SENSITIVITY_LIGHT);
        mapIds.put(R.id.sRbSenMed, ShakeDetector.SENSITIVITY_MEDIUM);
        mapIds.put(R.id.sRbSenHigh, ShakeDetector.SENSITIVITY_HARD);
        mapIds.put(ShakeDetector.SENSITIVITY_LIGHT, R.id.sRbSenLow);
        mapIds.put(ShakeDetector.SENSITIVITY_MEDIUM, R.id.sRbSenMed);
        mapIds.put(ShakeDetector.SENSITIVITY_HARD, R.id.sRbSenHigh);

    }


}