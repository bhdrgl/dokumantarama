package com.ziraat.dokumantarama;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by bhdrgl on 16/11/2017.
 */

public class ServerSocketThread extends Thread {

    private static final String TAG = "Connection";
    private final int SOCKET_PORT = 38300;
    private final int TIMEOUT = 10 * 1000;

    private ServerSocket server = null;
    private PrintWriter socketOut;
    private OutputStream os;
    private SocketListener listener;
    private BufferedReader in;



    public ServerSocketThread(SocketListener listener) {
        super();
        this.listener = listener;
    }

    public PrintWriter getPrintWriter() {
        return socketOut;
    }

    public void sendPhoto(final byte[] encoded) {

        Runnable sendData = new Thread() {
            public void run() {
                try {
                    os.write(encoded,0,encoded.length);
                    os.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        };

        new Thread(sendData).start();
    }

    public void sendPhoto(final String encoded) {

        Runnable sendData = new Thread() {
            public void run() {

                    socketOut.print(encoded);
                    socketOut.flush();


            }
        };

        new Thread(sendData).start();
    }

    public void write(final String data) {
        Runnable sendData = new Thread(new Runnable() {
            @Override
            public void run() {
                if (socketOut != null) {
                    socketOut.write(data);
                    socketOut.print("|");
                    socketOut.print("\\r\\n");
                    Log.d("SocketThreadLog", data);
                    socketOut.flush();
                }

            }
        });
        new Thread(sendData).start();
    }

    @Override
    public void run() {
        super.run();

        Socket client;
        try {
            server = new ServerSocket(SOCKET_PORT);
            server.setSoTimeout(TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG,e.getMessage());
        }
        while (true) {
            try {
                client = server.accept();
                if (client != null) {

                    Log.d(TAG, "Socket Connection Established");
                    in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    String line = null;
                    os = client.getOutputStream();
                    socketOut = new PrintWriter(client.getOutputStream(), false);
                    String incoming = "";
                    int character;
                    while ((character = in.read()) != -1) {
                        incoming = incoming.concat(String.valueOf((char) character));
                        if(incoming.equals("-")){
                            incoming = "";
                        }
                        if (incoming.contains("{EOF}"))
                            if (incoming != null && incoming.length() > 0) {
                                Log.d(TAG, "Socket Data = " + incoming);
                                String[] serverData = incoming.split("\\|");
                                listener.sendSocketCommand(Integer.valueOf(serverData[0]));
                                incoming = "";
                            }
                    }


                    if (client != null) {
                        Log.d(TAG, "Connection Successful");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG,e.getMessage());
            }
        }

    }
}
