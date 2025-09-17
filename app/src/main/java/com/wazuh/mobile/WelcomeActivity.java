package com.wazuh.mobile;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.wazuh.mobile.LoginActivity;
import com.wazuh.mobile.R;
import com.wazuh.mobile.RegisterActivity;


public class WelcomeActivity extends AppCompatActivity {

    private MaterialButton btnWelcomeLogin, btnWelcomeRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);

        btnWelcomeLogin = findViewById(R.id.btnWelcomeLogin);
        btnWelcomeRegister = findViewById(R.id.btnWelcomeRegister);

        btnWelcomeLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Mengarahkan ke LoginActivity
                Intent intent = new Intent(WelcomeActivity.this, LoginActivity.class);
                startActivity(intent);
            }
        });

        btnWelcomeRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                 Mengarahkan ke RegisterActivity
                Intent intent = new Intent(WelcomeActivity.this, RegisterActivity.class);
                startActivity(intent);
            }
        });
    }
}