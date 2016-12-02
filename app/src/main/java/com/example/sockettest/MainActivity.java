package com.example.sockettest;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.FormEncodingBuilder;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import java.io.IOException;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final String LOGIN_URL = "http://kawakp.chinclouds.com:60034/userconsle/login";

    private Intent mServiceIntent;
    private IBackService iBackService;
    private TextView tv;
    private EditText et;
    private Button btn;
    private String s;
    private Button btnlogin;
    private TextView tvcallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
     //   clickLogin();
        initData();


    }

    private void clickLogin() {
        final OkHttpClient okHttpClient = new OkHttpClient();


        btnlogin.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "click");
                Observable.create(new Observable.OnSubscribe<String>() {
                    @Override
                    public void call(final Subscriber<? super String> subscriber) {

                        RequestBody build = new FormEncodingBuilder().add("username", "jnadmin").add("password",
                                "chin2016").build();
                        Request request = new Request.Builder().url(LOGIN_URL).post(build).build();

                        try {
                            okHttpClient.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(Request request, IOException e) {
                                    Log.e(TAG, e.toString());

                                }

                                @Override
                                public void onResponse(Response response) throws IOException {

                                    String header = response.header("set-cookie");
                                    String cookie = header.substring(0, header.indexOf(";"));
                                    Log.d(TAG, cookie);
                                    CacheUtils.putString(MainActivity.this, "cookie", cookie);
                                    subscriber.onCompleted();

                                }
                            });


                        } catch (Exception e) {
                            e.printStackTrace();
                        }


                    }
                }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<String>() {
                            @Override
                            public void onCompleted() {
                                tvcallback.setText("登录成功");
                            }

                            @Override
                            public void onError(Throwable e) {

                            }

                            @Override
                            public void onNext(String s) {


                            }
                        });


            }
        });
    }

    private void initViews() {
        tv = (TextView) findViewById(R.id.tv);
        et = (EditText) findViewById(R.id.editText1);
        btn = (Button) findViewById(R.id.button1);
        btnlogin = (Button) findViewById(R.id.btn_login);
        tvcallback = (TextView) findViewById(R.id.tv_callback);
    }

    private void initData() {

        mServiceIntent = new Intent(this, BackService.class);
        btn.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                String string = et.getText().toString().trim();
                Log.i(TAG, string);
                try {
                    Log.i(TAG, "�Ƿ�Ϊ�գ�" + iBackService);
                    if (iBackService == null) {
                        Toast.makeText(getApplicationContext(),
                                "û�����ӣ������Ƿ������ѶϿ�", Toast.LENGTH_SHORT).show();
                    } else {
                        boolean isSend = iBackService.sendMessage(string);
                        Toast.makeText(MainActivity.this,
                                isSend ? "success" : "fail", Toast.LENGTH_SHORT)
                                .show();
                        et.setText("");
                    }
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(mServiceIntent, conn, BIND_AUTO_CREATE);
        // ��ʼ����
        registerReceiver();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ע��㲥 �����onResume��ע��
        // registerReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // ע���㲥 �����onPause��ע��
        unregisterReceiver(mReceiver);
        // ע������
        unbindService(conn);
    }

    // ע��㲥
    private void registerReceiver() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BackService.HEART_BEAT_ACTION);
        intentFilter.addAction(BackService.MESSAGE_ACTION);
        registerReceiver(mReceiver, intentFilter);
    }

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // ��Ϣ�㲥
            if (action.equals(BackService.MESSAGE_ACTION)) {
                String stringExtra = intent.getStringExtra("message");
                tv.setText(stringExtra);
            } else if (action.equals(BackService.HEART_BEAT_ACTION)) {// �����㲥
                tv.setText("��������");
            }
        }
    };

    private ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            // δ����Ϊ��
            iBackService = null;
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            // ������
            iBackService = IBackService.Stub.asInterface(service);
        }
    };


}
