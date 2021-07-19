package com.mixiao.pdf;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.artifex.mupdfdemo.PDFLoadUtils;
import com.artifex.mupdfdemo.MuPDFReaderView;
import com.artifex.mupdfdemo.PageView;

import java.util.Map;
import java.util.function.Function;

public class MainActivity extends AppCompatActivity {

    private Button button;
    private LinearLayout llPDFShow;
    private MuPDFReaderView muPDFReaderView;
    private PDFLoadUtils pdfLoadUtils;

    private Function function;

    @SuppressLint("HandlerLeak")
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(@NonNull Message msg) {
            initPDF((byte[]) msg.obj);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.button);
        llPDFShow = findViewById(R.id.ll_pdf);
        pdfLoadUtils = new PDFLoadUtils();
        getPDFBytes();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                muPDFReaderView.revokeDraw();
            }
        });
    }

    private void getPDFBytes() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Tools.getPDFBytes(MainActivity.this, new CallBack<byte[]>() {
                    @Override
                    public void onSuccess(byte[] bytes) {
                        Message message = new Message();
                        message.obj = bytes;
                        handler.sendMessage(message);
                    }

                    @Override
                    public void onError(String error) {
                        Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }

    private void initPDF(byte[] bytes) {
        muPDFReaderView = pdfLoadUtils.drawDrawingByte(bytes, this);
        if (muPDFReaderView == null) {
            Toast.makeText(this, "暂无法预览此文件", Toast.LENGTH_SHORT).show();
            return;
        }
        llPDFShow.addView(muPDFReaderView);

        muPDFReaderView.setOnDrawFinishedListener(pdfFinishedListener);
        muPDFReaderView.setOnDrawMarkFinishListener(markFinishedListener);
        muPDFReaderView.setOnMarkClickListener(markClickListener);
    }

    PageView.OnDrawFinishedListener pdfFinishedListener = new PageView.OnDrawFinishedListener() {
        @Override
        public void onFinished() {
            Toast.makeText(MainActivity.this, "PDF文件加载完成", Toast.LENGTH_SHORT).show();
        }
    };


    MuPDFReaderView.OnDrawMarkFinishedListener markFinishedListener = new MuPDFReaderView.OnDrawMarkFinishedListener() {
        @Override
        public void onFinish(Map<String, String> map, String tag) {
            String s = "tag:" + tag + "\n" + "X坐标:" + map.get(MuPDFReaderView.X) + "\n" + "Y坐标:" + map.get(MuPDFReaderView.Y);
            Toast.makeText(MainActivity.this, s, Toast.LENGTH_SHORT).show();
        }
    };

    PageView.OnMarkClickListener markClickListener = new PageView.OnMarkClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(MainActivity.this, v.getTag().toString(), Toast.LENGTH_SHORT).show();
        }
    };
}