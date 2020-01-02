import com.gluonhq.strange.cqc.AppSession;
import com.gluonhq.strange.cqc.CQCSession;
import com.gluonhq.strange.cqc.Protocol;
import com.gluonhq.strange.cqc.ResponseMessage;

import com.gluonhq.strange.gate.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.CountDownLatch;

public class BB84 {

    static final short appId = 0;
    static final short CQC_PORT_ALICE = 8001;
    static final short CQC_PORT_BOB = 8004;

    static final short APP_BOB = 8003;

    static final int SIZE = 16;
    static final CountDownLatch latch = new CountDownLatch(2);

    public static void main(String[] args) throws Exception {
         sendQubits();
    }

    public static void sendQubits() throws IOException {
        System.err.println("Send qubit from Alice to Bob CQC Server");
                    boolean[] base = new boolean[SIZE];
                    boolean[] val = new boolean[SIZE];
                    boolean[] bobbase = new boolean[SIZE];
                    boolean[] bobval = new boolean[SIZE];
        Thread alice = new Thread() {
            @Override public void run() {
                try {
                    CQCSession s = new CQCSession("Alice", appId);
                    s.connect("localhost", CQC_PORT_ALICE);
                    for (int i = 0; i < SIZE; i++) {
                        base[i] = Math.random() < .5;
                        val[i] = Math.random() < .5;
                        int qid = s.createQubit();
                        if (base[i]) {
                            s.applyGate(new Hadamard(qid));
                        }
                        if (val[i]) {
                            s.applyGate(new X(qid));
                        }
timeLog("[ALICE] Send qubit ");
                        s.sendQubit(qid, CQC_PORT_BOB);
System.err.println("[ALICE] Flushed classical bytes");
                    }
latch.countDown();
                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        Thread bob = new Thread() {
            @Override public void run() {
                try {
                    CQCSession s = new CQCSession("Bob", appId);
                    s.connect("localhost", CQC_PORT_BOB);
                    for (int i = 0; i < SIZE; i++) {
timeLog("BOB waits for a qubit");
                        ResponseMessage msg = s.receiveQubit();
                        short qid = msg.getQubitId();
                        bobbase[i] = Math.random() < .5;
                        if (bobbase[i]) {
                            s.applyGate(new Hadamard(qid));
                        }
timeLog("[BOB] BOB received a qubit with id "+qid);
                        boolean qValue = s.measure(qid);
timeLog("[BOB] Bob measures "+qValue);
                        bobval[i] = qValue;
                    }
latch.countDown();
                    // s.releaseQubit(qid);
// timeLog("[BOB] Bob released "+qid);

                }
                catch (Throwable t) {
                    t.printStackTrace();
                }
            }
        };
        bob.start();
        alice.start();
try {
latch.await();
} catch (InterruptedException e) {
e.printStackTrace();
}
for (int i = 0; i < SIZE; i++) {
System.err.println("A base = "+base[i]+" and aval = "+val[i]+";  B base = "+bobbase[i]+" and bval = "+bobval[i]);
}
    }

    static void timeLog(String msg) {
        long now = System.currentTimeMillis();
        long millis = now%1000;
        long sec = (now/1000)%60;
        System.err.println("["+sec+"."+millis+"] "+msg);
    }
}
