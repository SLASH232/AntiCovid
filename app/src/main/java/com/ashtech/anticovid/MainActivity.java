package com.ashtech.anticovid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.ashtech.anticovid.LgnSgn.SocialLoginActivity;
import com.ashtech.anticovid.LgnSgn.UserLoginActivity;
import com.ashtech.anticovid.System.onAppkilled;
import com.ashtech.anticovid.Users.SocialMapsActivity;
import com.ashtech.anticovid.Users.SocialTrial;
import com.ashtech.anticovid.Users.UsersMapsActivity;

public class MainActivity extends AppCompatActivity {
    private Button mDriver, mCustomer;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCustomer = (Button) findViewById(R.id.btn_social);



        mCustomer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SocialLoginActivity.class);
                startActivity(intent);
                finish();
                return;
            }
        });
    }
}
