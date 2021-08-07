package com.example.androidcanvasbasics;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.LinearLayout;

public class MainActivity extends AppCompatActivity {

    LinearLayout linearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 有一说一，这个demo的用法，真的跟qt很像
        linearLayout = findViewById(R.id.linearLayout);
        MyView myView = new MyView(this);
        linearLayout.addView(myView); // 添加自定义的布局
    }
}