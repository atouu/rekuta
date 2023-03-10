package com.rekuta.mashi;

import android.animation.*;
import android.app.*;
import android.app.Activity;
import android.content.*;
import android.content.Intent;
import android.content.pm.*;
import android.content.res.*;
import android.os.*;
import android.text.*;
import android.text.style.*;
import android.util.*;
import android.view.*;
import android.view.View;
import android.view.View.*;
import android.view.animation.*;
import android.webkit.*;
import android.widget.*;
import android.widget.Button;

public class SetupActivity extends Activity {
	   
	private Button button1;
	
	private Intent main = new Intent();
	private String[] perms = new String[] {
		android.Manifest.permission.READ_EXTERNAL_STORAGE,
		android.Manifest.permission.RECORD_AUDIO,
		android.Manifest.permission.WRITE_EXTERNAL_STORAGE
	};
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		setContentView(R.layout.setup);
		initialize(_savedInstanceState);
		_checkPermissions();
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
		_checkPermissions();
	}
	
	
	public void _checkPermissions() {
		if (Build.VERSION.SDK_INT >= 23) {
			if (hasPermissions(perms)) {
				main.setClass(getApplicationContext(), MainActivity.class);
				startActivity(main);
				finish();
			}
		}
		else {
			main.setClass(getApplicationContext(), MainActivity.class);
			startActivity(main);
			finish();
		}
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