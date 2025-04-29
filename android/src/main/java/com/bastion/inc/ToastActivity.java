package com.bastion.inc;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

public class ToastActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);

    String message = getIntent().getStringExtra("toast_message");

    Toast.makeText(this, message, Toast.LENGTH_LONG).show();

    new Handler().postDelayed(this::finish, 2000);
  }
}
