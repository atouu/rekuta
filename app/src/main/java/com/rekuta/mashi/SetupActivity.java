package com.rekuta.mashi;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class SetupActivity extends Activity {
          
    private Button button1;
    
    private String[] perms = new String[] {
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    
    @Override
    protected void onCreate(Bundle _savedInstanceState) {
        super.onCreate(_savedInstanceState);
        checkPermissions();
        getActionBar().hide();
        setContentView(R.layout.setup);
        initialize(_savedInstanceState);
    }
    
    private void initialize(Bundle _savedInstanceState) {   
        button1 = findViewById(R.id.button1);
        
        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View _view) {
                if (Build.VERSION.SDK_INT >= 23) {
                    requestPermissions(perms, 1000);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        checkPermissions();
    }
    
    public void checkPermissions() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (hasPermissions(perms)) {
                startMain();
            }
        }
        else {
            startMain();
        }
    }
    
    public void startMain() {
        Intent main = new Intent(this, MainActivity.class);
        startActivity(main);
        finish();
    }

    public boolean hasPermissions(String... permissions) {
        if (permissions != null) {
            for (String permission : permissions) {
                if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
}
