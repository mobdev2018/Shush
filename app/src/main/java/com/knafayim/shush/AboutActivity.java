package com.knafayim.shush;

import android.app.ActionBar;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class AboutActivity extends AppCompatActivity {

    TextView aboutTextView;
    TextView versionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.abs_layout);

        aboutTextView = (TextView) findViewById(R.id.aboutTextView);
        aboutTextView.setText(Html.fromHtml("<p>For more information contact <a href =\"mailto:Raffi@baltimoretherapycenter.com\">Raffi@baltimoretherapycenter.com</a>.</p>"));
        aboutTextView.setMovementMethod(LinkMovementMethod.getInstance());

        PackageManager manager = this.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(
                    getPackageName(), 0);
            versionTextView = (TextView) findViewById(R.id.txtVersion);
            versionTextView.setText("Version" + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            Toast.makeText(getApplicationContext(), "Version unknown", Toast.LENGTH_LONG).show();
        }


    }

    public void knafayimWebsite(View view){
        String url = "https://www.baltimoretherapycenter.com/";
        Intent i = new Intent(Intent.ACTION_VIEW);
        i.setData(Uri.parse(url));
        startActivity(i);
    }
}
