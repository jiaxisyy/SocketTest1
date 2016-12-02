package com.example.sockettest;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Arrays;

public class BackService extends Service {
    private static final String TAG = "BackService";
    /**
     * �������ʱ��
     */
    private static final long HEART_BEAT_RATE = 3 * 1000;
    /**
     * ����IP��ַ
     */
    private static final String HOST = "58.250.204.174";
    /**
     * �˿ں�
     */
    public static final int PORT = 60032;
    /**
     * ��Ϣ�㲥
     */
    public static final String MESSAGE_ACTION = "org.feng.message_ACTION";
    /**
     * �����㲥
     */
    public static final String HEART_BEAT_ACTION = "org.feng.heart_beat_ACTION";

    private long sendTime = 0L;

    /**
     * ������ �����ö����ͬʱ���������������л���
     */
    private WeakReference<Socket> mSocket;

    private ReadThread mReadThread;

    private IBackService.Stub iBackService = new IBackService.Stub() {
        @Override
        public boolean sendMessage(String message) throws RemoteException {
            return sendMsg(message);
        }
    };


    @Override
    public IBinder onBind(Intent arg0) {
        return (IBinder) iBackService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        new InitSocketThread().start();
    }

    public static String GetInetAddress(String host) {
        String IPAddress = "";
        InetAddress ReturnStr1 = null;
        try {
            ReturnStr1 = java.net.InetAddress.getByName(host);
            IPAddress = ReturnStr1.getHostAddress();
        } catch (UnknownHostException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return IPAddress;
        }
        return IPAddress;
    }

    // ����������
    private Handler mHandler = new Handler();
    private Runnable heartBeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (System.currentTimeMillis() - sendTime >= HEART_BEAT_RATE) {
                boolean isSuccess = sendMsg("");// �ͷ���һ��\r\n��ȥ, �������ʧ�ܣ������³�ʼ��һ��socket
                if (!isSuccess) {
                    mHandler.removeCallbacks(heartBeatRunnable);
                    mReadThread.release();
                    releaseLastSocket(mSocket);
                    new InitSocketThread().start();
                }
            }
            mHandler.postDelayed(this, HEART_BEAT_RATE);
        }
    };

    public boolean sendMsg(String msg) {
        if (null == mSocket || null == mSocket.get()) {
            return false;
        }
        Socket soc = mSocket.get();
        try {
            if (!soc.isClosed() && !soc.isOutputShutdown()) {
                OutputStream os = soc.getOutputStream();
                String message = msg + "\r\n";
                os.write(message.getBytes());
                os.flush();
                sendTime = System.currentTimeMillis();// ÿ�η��ͳɹ����ݣ��͸�һ�����ɹ����͵�ʱ�䣬��ʡ�������ʱ��
                Log.i(TAG, "���ͳɹ���ʱ�䣺" + sendTime);
            } else {
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    // ��ʼ��socket
    private void initSocket() throws UnknownHostException, IOException {


        Socket socket = new Socket(HOST, PORT);
        mSocket = new WeakReference<Socket>(socket);
        mReadThread = new ReadThread(socket);
        mReadThread.start();
        mHandler.postDelayed(heartBeatRunnable, HEART_BEAT_RATE);// ��ʼ���ɹ��󣬾�׼������������
    }

    // �ͷ�socket
    private void releaseLastSocket(WeakReference<Socket> mSocket) {
        try {
            if (null != mSocket) {
                Socket sk = mSocket.get();
                if (!sk.isClosed()) {
                    sk.close();
                }
                sk = null;
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    class InitSocketThread extends Thread {
        @Override
        public void run() {
            super.run();
            try {
                initSocket();
            } catch (UnknownHostException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public class ReadThread extends Thread {
        private WeakReference<Socket> mWeakSocket;
        private boolean isStart = true;

        public ReadThread(Socket socket) {
            mWeakSocket = new WeakReference<Socket>(socket);
        }

        public void release() {
            isStart = false;
            releaseLastSocket(mWeakSocket);
        }

        @SuppressLint("NewApi")
        @Override
        public void run() {
            super.run();
            Socket socket = mWeakSocket.get();
            if (null != socket) {
                try {
                    InputStream is = socket.getInputStream();
                    byte[] buffer = new byte[1024 * 4];
                    int length = 0;
                    while (!socket.isClosed() && !socket.isInputShutdown()
                            && isStart && ((length = is.read(buffer)) != -1)) {
                        if (length > 0) {
                            String message = new String(Arrays.copyOf(buffer,
                                    length)).trim();
                            Log.i(TAG, "�յ�����������������Ϣ��" + message);
                            // �յ���������������Ϣ����ͨ��Broadcast���ͳ�ȥ
                            if (message.equals("ok")) {// ���������ظ�
                                Intent intent = new Intent(HEART_BEAT_ACTION);
                                sendBroadcast(intent);
                            } else {
                                // ������Ϣ�ظ�
                                Intent intent = new Intent(MESSAGE_ACTION);
                                intent.putExtra("message", message);
                                sendBroadcast(intent);
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
