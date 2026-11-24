package com.limbo.emu.main;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class WildfireHomeActivity extends Activity {
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(28, 28, 28, 28);

        TextView title = new TextView(this);
        title.setText("Wildfire Lab");
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("One app. Choose the system architecture you want to emulate.");
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams subtitleParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        subtitleParams.setMargins(0, 12, 0, 28);
        root.addView(subtitle, subtitleParams);

        Button pc = new Button(this);
        pc.setText("PC systems · x86 / x86_64");
        pc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(WildfireHomeActivity.this, LimboEmuActivity.class));
            }
        });
        root.addView(pc, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        Button phone = new Button(this);
        phone.setText("Phone / ARM systems · AArch64");
        phone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivity(new Intent(WildfireHomeActivity.this, com.limbo.emu.main.arm.LimboEmuActivity.class));
            }
        });
        LinearLayout.LayoutParams phoneParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        phoneParams.setMargins(0, 12, 0, 0);
        root.addView(phone, phoneParams);

        setContentView(root);
    }
}
