package com.gluonhq.javaqc.ch08.classiclayers;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

public class Alice {

    public void startAlice() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    System.err.println("Start alice");
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(InetAddress.getLocalHost(), 9753), 50000);
                    System.err.println("connected alice");

                    OutputStream os = new SecureOutputStream(socket.getOutputStream());
                    System.err.println("got os for alice: "+os);

                    PrintStream printStream = new PrintStream(os);
                    printStream.print("Hello, world\n");
                  //  printStream.print("Hello, world 2\n");
                    System.err.println("Wrote hw");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        t.start();
    }


    class SecureOutputStream extends  OutputStream {
        private final OutputStream os;

        int getNextKey() {
            return 10;
        }

        public SecureOutputStream(OutputStream os) {
            this.os = os;
        }

        public void write(int b) throws IOException {
            int c = b ^ getNextKey();
            System.err.println("Had to write "+b+", will write "+c);
            this.os.write(c);
        }
    }
}
