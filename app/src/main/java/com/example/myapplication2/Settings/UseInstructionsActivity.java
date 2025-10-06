package com.example.myapplication2.Settings;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication2.R;
import com.github.barteksc.pdfviewer.PDFView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class UseInstructionsActivity extends AppCompatActivity{


       PDFView pdfView;
        FloatingActionButton mBackBtn;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_userinstru);

            pdfView = findViewById(R.id.pdfView);
            mBackBtn = findViewById(R.id.buttonbackinstr);

            // Carregar PDF do assets
            pdfView.fromAsset("Instru.pdf")
                    .enableAnnotationRendering(true)
                    .spacing(10)
                    .load();

            mBackBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finish();
                }
            });


        }


}
