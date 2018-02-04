package com.knafayim.shush;

import android.app.ActionBar;
import android.content.Intent;
import android.media.AudioManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.knafayim.shush.geofence.GeofenceService;
import com.xw.repo.BubbleSeekBar;

public class VolumeSettingsActivity extends AppCompatActivity {

    RadioGroup enterRadioGroup;
    RadioGroup exitRadioGroup;
    RadioButton silentButton;
    RadioButton vibrateButton;
    RadioButton revertButton;
    RadioButton nothingButton;
    int entryInt;
    int exitInt;
    private BubbleSeekBar bubbleSeekBar;
    private int radius;
    private SwitchCompat startAfterBoot;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_volume_settings);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);
        enterRadioGroup = (RadioGroup) findViewById(R.id.enterRadioGroup);
        exitRadioGroup = (RadioGroup) findViewById(R.id.exitRadioGroup);
        vibrateButton = (RadioButton) findViewById(R.id.vibrateRadioButton);
        silentButton = (RadioButton) findViewById(R.id.silentRadioButton);
        revertButton = (RadioButton) findViewById(R.id.revertRadioButton);
        nothingButton = (RadioButton) findViewById(R.id.nothingRadioButton);
        startAfterBoot = (SwitchCompat) findViewById(R.id.start_after_boot);
        startAfterBoot.setChecked(AppGlobals.shouldStartAfterBoot());
        if (AppGlobals.shouldStartAfterBoot()) {
            startAfterBoot.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
        } else {
            startAfterBoot.setTextColor(getResources().getColor(android.R.color.darker_gray));

        }
        startAfterBoot.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                AppGlobals.startAfterBoot(b);
                if (b) {
                    startAfterBoot.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                } else {
                    startAfterBoot.setTextColor(getResources().getColor(android.R.color.darker_gray));

                }
            }
        });
        nothingButton.setId(R.id.nothing);
        bubbleSeekBar = (BubbleSeekBar) findViewById(R.id.radius);
        bubbleSeekBar.setProgress(AppGlobals.getFenceRadius());
        radius = AppGlobals.getFenceRadius();
        Log.i("TAG", "exit mode "+AppGlobals.getExitMode());
        bubbleSeekBar.setOnProgressChangedListener(new BubbleSeekBar.OnProgressChangedListener() {
            @Override
            public void onProgressChanged(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {
                radius = progress;
            }

            @Override
            public void getProgressOnActionUp(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }

            @Override
            public void getProgressOnFinally(BubbleSeekBar bubbleSeekBar, int progress, float progressFloat) {

            }
        });

        entryInt = AppGlobals.getEnterMode();
        exitInt = AppGlobals.getExitMode();

        Log.i("TAG", "entry "  + entryInt);

        if(entryInt == -1) {
            ((RadioButton)enterRadioGroup.getChildAt(0)).setChecked(true);
            ((RadioButton)exitRadioGroup.getChildAt(0)).setChecked(true);
        } else {
            switch(entryInt) {
                case 0:
                    ((RadioButton)enterRadioGroup.getChildAt(1)).setChecked(true);
                    break;
                case 1:
                    ((RadioButton)enterRadioGroup.getChildAt(0)).setChecked(true);
                    break;
            }
            switch(exitInt) {
                case 0:
                    ((RadioButton)exitRadioGroup.getChildAt(0)).setChecked(true);
                    break;
                case 1:
                    ((RadioButton)exitRadioGroup.getChildAt(1)).setChecked(true);
                    break;
            }
        }
    }

    public void saveSettings(View view){
        switch(enterRadioGroup.getCheckedRadioButtonId()){
            case R.id.vibrateRadioButton:
                entryInt = 1;
                break;
            case R.id.silentRadioButton:
                entryInt = 0;
                break;
        }

        switch (exitRadioGroup.getCheckedRadioButtonId()){
            case R.id.revertRadioButton:
                exitInt = 0;
                break;
            case R.id.nothing:
                exitInt = 1;
                break;
        }
        Log.i("TAG", "entry " + entryInt);
        Log.i("TAG", "exit int "+ exitInt);
        AppGlobals.setFenceRadius(radius);
        Log.i("entry", "" + entryInt);

        if(entryInt != -1 || exitInt != -1) {
            if (AppGlobals.isServiceActive()) {
                AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
                audioManager.setRingerMode(AppGlobals.getPreviousRingerState());

            }
            AppGlobals.setEnterMode(entryInt);
            AppGlobals.setExitMode(exitInt);
            stopService(new Intent(getApplicationContext(), GeofenceService.class));
            new android.os.Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (AppGlobals.isServiceActive()) {
                        startService(new Intent(getApplicationContext(), GeofenceService.class));
                    }
                }
            }, 1000);
            finish();
        } else {
            Toast.makeText(getApplicationContext(), "Both entry/exit options have to be selected", Toast.LENGTH_LONG).show();
        }
    }
}
