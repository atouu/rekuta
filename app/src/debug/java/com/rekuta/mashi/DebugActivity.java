package com.rekuta.mashi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;

public class DebugActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();

        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        dialog.setTitle("An error occurred");
        dialog.setMessage(intent.getStringExtra("error"));
        dialog.setPositiveButton("Exit", (dialog1, which) -> finish());
        dialog.create();
        dialog.show();
    }
}
