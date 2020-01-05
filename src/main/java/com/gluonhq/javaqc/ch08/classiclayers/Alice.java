package com.gluonhq.javaqc.ch08.classiclayers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Random;

import com.gluonhq.strange.cqc.CQCSession;
import com.gluonhq.strange.gate.*;

public class Alice {

    private boolean classicOpen = false; // true if we already listen to classical channel
    Socket classicSocket;
    InputStream classicInputStream;
    OutputStream classicOutputStream;
    private byte[] key;
    int keyCnt = 0;
    private CQCSession cqcSession;

    private byte[] getKey(int size) throws IOException {
        int bitSize = 8 * size;
        BitSet bitResult = new BitSet(bitSize);
        int covered = 0;
        while (covered < bitSize) {
            System.err.println("[GETKEY] ALICE NEED KEY OF SIZE "+bitSize+" and has "+covered);
            boolean[] valid = getKeyImpl((bitSize-covered) * 2);
            System.err.println("[GETKEY] ADDED bits: "+valid.length);
            int addSize = Math.min(bitSize - covered,valid.length);
            for (int i = 0; i < addSize; i++) {
                System.err.println("BIT "+i+": "+valid[i]);
                bitResult.set(covered++,valid[i]);
            }
        }
        byte[] result = bitResult.toByteArray();
        return result;
    }

    public void startAlice() {
        Thread t = new Thread() {
            @Override
            public void run() {
                try {
                    cqcSession = new CQCSession("Alice", Main.appId);
                    cqcSession.connect("localhost", Main.CQC_PORT_ALICE, Main.APP_PORT_ALICE);
                    key = getKey(20);
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
            return key[keyCnt++];
//            return 10;
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

    private boolean[] getKeyImpl(final int size) throws IOException {
        System.err.println("Send qubit from Alice to Bob CQC Server");
        final Random random = new Random();
        final boolean[] key = new boolean[size];
        final boolean[] base = new boolean[size];
        boolean[] validBuffer = new boolean[size];
        boolean[] validKey = new boolean[size];

        try {

            for (int i = 0; i < size; i++) {
                key[i] = random.nextBoolean();
                base[i] = random.nextBoolean();
                timeLog("[ALICE] Creating Qubit " + i + " in base " + base[i] + " with val " + key[i]);
                int qid = cqcSession.createQubit();
                if (key[i]) cqcSession.applyGate(new X(qid));
                if (base[i]) cqcSession.applyGate(new Hadamard(qid));
                timeLog("[ALICE] Send qubit ");
                cqcSession.sendQubit(qid, Main.CQC_PORT_BOB);
            }
            System.err.println("Done sending qubits, lets evaluate now");
            Thread.sleep(2000);
            if (!classicOpen) {
                classicSocket = new Socket(InetAddress.getLocalHost(), Main.APP_PORT_BOB);
                classicOutputStream = classicSocket.getOutputStream();
                classicInputStream = classicSocket.getInputStream();
                classicOpen = true;
            }
            for (int i = 0; i < size; i++) {
                classicOutputStream.write(base[i] ? 0x1 : 0x0);
            }
            classicOutputStream.flush();
            int validSize = 0;
            for (int i = 0; i < size; i++) {
                boolean bb = classicInputStream.read() == 1? true : false;
                System.err.println("ALICE: "+i+": AliceBase = "+base[i]+" and Bobbase = "+bb);

                if (!Boolean.logicalXor(bb, base[i])) {
                    System.err.println("ALICEBIT "+validSize+": "+key[i]);

                    validBuffer[validSize] = key[i];
                    validSize++;
                }
            }
            validKey = Arrays.copyOf(validBuffer, validSize);

            System.err.println("[ALICE] Flushed classical bytes");
            return validKey;
        } catch (Throwable t) {
            t.printStackTrace();

            return null;
        }
    }

     static void timeLog(String msg) {
        long now = System.currentTimeMillis();
        long millis = now%1000;
        long sec = (now/1000)%60;
        System.err.println("["+sec+"."+millis+"] "+msg);
    }

}
