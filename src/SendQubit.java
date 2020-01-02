import com.gluonhq.strange.cqc.AppSession;
import com.gluonhq.strange.cqc.CQCSession;
import com.gluonhq.strange.cqc.Protocol;
import com.gluonhq.strange.cqc.ResponseMessage;

import com.gluonhq.strange.gate.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class SendQubit {

    static final short appId = 0;
    static final short CQC_PORT_ALICE = 8001;
    static final short CQC_PORT_BOB = 8004;

    static final short APP_BOB = 8003;
// DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");


    public static void main(String[] args) throws Exception {
         sendQubit();
    }

    public static void sendQubit() throws IOException {
        System.err.println("Send qubit from Alice to Bob CQC Server");
        Thread alice = new Thread() {
            @Override public void run() {
                try {
                    CQCSession s = new CQCSession("Alice", appId);
                    s.connect("localhost", CQC_PORT_ALICE);
timeLog("[ALICE] Creating Qubit ");
                    int qid = s.createQubit();
timeLog("[ALICE] Created Qubit, id = "+qid);
timeLog("[ALICE] dontApply X");
// s.applyGate(new X(qid));
timeLog("[ALICE] Applied X");

// Thread.sleep(1000);

timeLog("[ALICE] Send qubit ");
                    s.sendQubit(qid, CQC_PORT_BOB);
System.err.println("[ALICE] Flushed classical bytes");
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
timeLog("BOB waits for a qubit");
ResponseMessage gotEpr = s.receiveQubit();
                    short qid = gotEpr.getQubitId();
timeLog("[BOB] BOB received a qubit: "+gotEpr+" with id "+qid);
                    boolean qValue = s.measure(qid);
timeLog("[BOB] Bob measures "+qValue);
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
    }

    static void timeLog(String msg) {
        long now = System.currentTimeMillis();
        long millis = now%1000;
        long sec = (now/1000)%60;
        System.err.println("["+sec+"."+millis+"] "+msg);
    }
}
