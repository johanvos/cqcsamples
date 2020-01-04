package com.gluonhq.javaqc.ch08.classiclayers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;

public class Bob {

    public void startBob() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    ServerSocket serverSocket = new ServerSocket(9753);
                    latch.countDown();
                    Socket socket = serverSocket.accept();
                    InputStream is = new SecureInputStream(socket.getInputStream());
   //                 InputStream is = socket.getInputStream();
                    BufferedReader reader = new BufferedReader( new InputStreamReader(is));
                    String line = reader.readLine();
                    System.err.println("Reading line: "+line);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
        latch.await();
    }

    class SecureInputStream extends InputStream {

        private final InputStream is;

        public SecureInputStream (InputStream is) {
            this.is = is;
        }

        int getNextKey() {
            return 10;
        }
boolean eol = false;

         public int read(byte b[]) throws IOException {
             System.err.println("READ[] asked");
             return is.read(b);
         }

 public int read(byte b[], int off, int length) throws IOException {
     System.err.println("READ[]II asked, off = "+off+", length = "+length);
     int size =  is.read(b, off, length);
     System.err.println("Return size: "+size);
     for (int i = 0; i < size; i++) {
         System.err.println("Byte["+i+"] = "+b[i]);
         b[i] = (byte)(b[i] ^ (byte)getNextKey());
     }
     return size;
 }

 @Override
 public int available () throws IOException {
     int answer = is.available();
     System.err.println("AVAILABLE asked, return "+answer);
             return answer;
 }
        public int read() throws IOException {
//            return is.read();
            System.err.println("READ ASKED... ");
            int av = is.available();
            System.err.println("AV = "+av);
            if (av == 0) return -1;
            int secret = is.read();
            int orig = secret ^ getNextKey();
            System.err.println("did read "+secret+", converted into "+orig);
            if (orig == 10) eol = true;
            return orig;
        }

    }
}
