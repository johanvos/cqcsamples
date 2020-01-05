package com.gluonhq.javaqc.ch08.classiclayers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;
import java.util.concurrent.CountDownLatch;

import com.gluonhq.strange.cqc.CQCSession;
import com.gluonhq.strange.gate.*;

public class Bob {

    private boolean classicOpen = false; // true if we already listen to classical channel
    ServerSocket serverSocket;
    Socket socket;
    InputStream classicInputStream;
    private byte[] key;
    int keyCnt = 0;
    private CQCSession cqcSession;

    public void startBob() throws InterruptedException {
        final CountDownLatch latch = new CountDownLatch(1);
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    cqcSession = new CQCSession("Bob", Main.appId);
                    cqcSession.connect("localhost", Main.CQC_PORT_BOB, Main.APP_PORT_BOB);
                    ServerSocket serverSocket = new ServerSocket(9753);
                    latch.countDown();
                    key = getKey(20);
                    Socket socket = serverSocket.accept();
                    InputStream is = new SecureInputStream(socket.getInputStream());
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    String line = reader.readLine();
                    System.err.println("Reading line: " + line);
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

        public SecureInputStream(InputStream is) {
            this.is = is;
        }

        int getNextKey() {
            return key[keyCnt++];
//            return 10;
        }

        @Override
        public int read(byte b[], int off, int length) throws IOException {
            int size = is.read(b, off, length);
            for (int i = 0; i < size; i++) {
                b[i] = (byte) (b[i] ^ (byte) getNextKey());
            }
            return size;
        }

        @Override
        public int available() throws IOException {
            return is.available();
        }

        public int read() throws IOException {
            if (is.available() == 0) return -1;
            int secret = is.read();
            int orig = secret ^ getNextKey();
            return orig;
        }
    }

    private byte[] getKey(int size) throws IOException {
        int bitSize = 8 * size;
        BitSet bitResult = new BitSet(bitSize);
        int covered = 0;
        while (covered < bitSize) {
            System.err.println("[GETKEY] BOB NEED KEY OF SIZE "+bitSize+" and has "+covered);
            boolean[] valid = receiveKey((bitSize-covered) * 2);
            System.err.println("[GETKEY] BOB ADDED bits: "+valid.length);
            int addSize = Math.min(bitSize - covered,valid.length);
            for (int i = 0; i < addSize; i++) {
                System.err.println("BOBBIT "+i+": "+valid[i]);
                bitResult.set(covered++,valid[i]);
            }
        }
        byte[] result = bitResult.toByteArray();
        return result;
    }


    private boolean[] receiveKey(final int size) {
        final Random random = new Random();
        final boolean[] key = new boolean[size];
        final boolean[] base = new boolean[size];
        boolean[] validBuffer = new boolean[size];
        boolean[] validKey = null;
        int validSize = 0;

        try {

            for (int i = 0; i < size; i++) {
                timeLog("BOB waits for a qubit");
                int qid = cqcSession.receiveQubit();
                timeLog("[BOB] BOB received a qubit with id " + qid);
                base[i] = random.nextBoolean();
                if (base[i]) {
                    cqcSession.applyGate(new Hadamard(qid));
                }
                boolean qValue = cqcSession.measure(qid);
                key[i] = qValue;
                timeLog("[BOB] Key " + i + ": Bob measures " + qValue + " in base " + base[i]);
            }
            if (!classicOpen) {
                serverSocket = new ServerSocket(Main.APP_PORT_BOB);
                socket = serverSocket.accept();
                classicInputStream = socket.getInputStream();
                classicOpen = true;
            }
            boolean[] aliceBase = new boolean[size];
            for (int i = 0; i < size; i++) {
                aliceBase[i] = classicInputStream.read() == 1;
            }
            OutputStream os = socket.getOutputStream();
            for (int i = 0; i < size; i++) {
                os.write(base[i] ? 0x1 : 0x0);
                System.err.println("BOB: "+i+": AliceBase = "+aliceBase[i]+" and Bobbase = "+base[i]);
                if (!Boolean.logicalXor(aliceBase[i], base[i])) {
                    System.err.println("BOBBIT "+validSize+": "+key[i]);
                    validBuffer[validSize] = key[i];
                    validSize++;
                }

            }

            validKey = Arrays.copyOf(validBuffer, validSize);
            os.flush();

        } catch (Throwable t) {
            t.printStackTrace();
        }
        return validKey;
    }

    static void timeLog(String msg) {
        long now = System.currentTimeMillis();
        long millis = now % 1000;
        long sec = (now / 1000) % 60;
        System.err.println("[" + sec + "." + millis + "] " + msg);
    }
}
