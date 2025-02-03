package com.rekuta.mashi.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.rekuta.mashi.BuildConfig;
import com.rekuta.mashi.R;
import com.rekuta.mashi.databinding.ActivityAboutBinding;

import java.util.Locale;

import de.psdev.licensesdialog.LicensesDialog;

public class AboutActivity extends AppCompatActivity {

    private ActivityAboutBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAboutBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        initialize();
    }

    public void initialize() {
        binding.version.setText(String.format(Locale.ENGLISH, "%s (%d)", BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE));

        binding.projectGithub.setOnClickListener(view -> openURL(R.string.project_github_url));
        binding.authorWebsite.setOnClickListener(view -> openURL(R.string.developer_website_url));
        binding.authorGithub.setOnClickListener(view -> openURL(R.string.developer_github_url));
        binding.authorBluesky.setOnClickListener(view -> openURL(R.string.developer_bluesky_url));

        binding.projectLicenses.setOnClickListener(view -> {
            LicensesDialog.Builder licensesDialog = new LicensesDialog.Builder(this);
            licensesDialog.setNotices(R.raw.notices);
            licensesDialog.setIncludeOwnLicense(true);
            licensesDialog.build().show();
        });
    }

    public void openURL(int urlRes) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setData(Uri.parse(getString(urlRes)));
        startActivity(intent);
    }
}