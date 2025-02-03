package com.rekuta.mashi.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.rekuta.mashi.databinding.ActivitySetupBinding;

public class SetupActivity extends AppCompatActivity {

    private ActivitySetupBinding binding;

    private final String[] perms = new String[] {
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (hasPermissions(perms)) startMain();

        getSupportActionBar().hide();
        binding = ActivitySetupBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        binding.button1.setOnClickListener(view -> ActivityCompat.requestPermissions(this, perms, 1000));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (hasPermissions(perms)) startMain();
    }
    
    public void startMain() {
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        finish();
    }

    public boolean hasPermissions(String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        
        return true;
    }
    
}
