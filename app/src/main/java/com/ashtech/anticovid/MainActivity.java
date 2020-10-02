package com.ashtech.anticovid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ashtech.anticovid.LgnSgn.SocialLoginActivity;

public class MainActivity extends AppCompatActivity {
    private Button  mSocial;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mSocial = (Button) findViewById(R.id.btn_social);



        mSocial.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SocialLoginActivity.class);
                startActivity(intent);
                finish();

            }
        });
    }
}
