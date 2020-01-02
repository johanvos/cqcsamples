import com.gluonhq.strange.cqc.AppSession;
import com.gluonhq.strange.cqc.CQCSession;
import com.gluonhq.strange.cqc.Protocol;
import com.gluonhq.strange.cqc.ResponseMessage;

import com.gluonhq.strange.gate.*;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class BB84 {

    static final short appId = 0;
    static final short CQC_PORT_ALICE = 8001;
    static final short CQC_PORT_BOB = 8004;

    static final short APP_BOB = 8003;
// DateFormat formatter = new SimpleDateFormat("HH:mm:ss.SSS");


    public static void main(String[] args) throws Exception {
         bb84();
    }

    public static void sayHello() throws IOException {
        System.err.println("Sending HELLO to CQC Server");
        CQCSession s = new CQCSession("Alice", appId);
        s.connect("localhost", CQC_PORT_ALICE);
        s.sendHello();
        ResponseMessage message = s.readMessage();
        byte type = message.getType();
        if (type == Protocol.CQC_TP_HELLO) {
            System.err.println("Respose from CQC Server = HELLO");
        } else {
            System.err.println("Something went wrong, no HELLO from CQC Server");
        }
    }

    public static void bb84() throws IOException {
        System.err.println("Teleport from Alice to Bob CQC Server");
        Thread alice = new Thread() {
            @Override public void run() {
                try {
                    CQCSession s = new CQCSession("Alice", appId);
                    s.connect("localhost", CQC_PORT_ALICE);
Thread.sleep(1000);
timeLog("[ALICE] Creating Qubit ");
                    int qid = s.createQubit();
timeLog("[ALICE] Created Qubit, id = "+qid);

Thread.sleep(1000);

timeLog("[ALICE] Creating EPR ");
                    ResponseMessage epr = s.createEPR("BOB", CQC_PORT_BOB);
                    int qAId = epr.getQubitId();
timeLog("[ALICE] EPR created, qaid = "+qAId);
                    short eprPort = epr.getEprOtherPort();
System.err.println("[ALICE] EPRport = "+eprPort);
s.applyGate(new X(qid));
System.err.println("[ALICE] Applying CNot to "+qid+" and "+qAId);

Thread.sleep(1000);

                    s.applyGate(new Cnot(qid, qAId));
System.err.println("[ALICE] Applying H to "+qid);

Thread.sleep(1000);

                    s.applyGate(new Hadamard(qid));
System.err.println("[ALICE] measure q0 with id "+qid);

Thread.sleep(1000);

                    boolean qValue = s.measure(qid);
System.err.println("[ALICE] measured q0 (with id "+qid+"): "+qValue);
                    boolean qAValue = s.measure(qAId);
System.err.println("[ALICE] measued q1 (id = "+qAId+"): "+qAValue);
                    AppSession a = new AppSession();
                    OutputStream os = a.connect("localhost", APP_BOB);
                    DataOutputStream dos = new DataOutputStream(os);
                    dos.writeByte((byte)(qValue ? 1 :0));
                    dos.writeByte((byte)(qAValue ? 1 :0));
                    dos.flush();
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
                    AppSession appSession = new AppSession(APP_BOB);
                    CQCSession s = new CQCSession("Bob", appId);
                    s.connect("localhost", CQC_PORT_BOB);
timeLog("BOB waits for an EPR");
ResponseMessage gotEpr = s.receiveEPR();
                    short eprId = gotEpr.getQubitId();
timeLog("[BOB] BOB received an EPR: "+gotEpr+" with id "+eprId);
                    byte b0 = appSession.readByte();
System.err.println("BOB got b0: "+b0);
                    byte b1 = appSession.readByte();
System.err.println("BOB got b1: "+b1);
                    if (b0 == 0x1) {
System.err.println("BOB apply X");
                        s.applyGate(new X(eprId));
                    }
                    if (b1 == 0x1) {
System.err.println("BOB apply Z");
                        s.applyGate(new Z(eprId));
                    }
                    boolean qValue = s.measure(eprId);
System.err.println("BOB got value: "+qValue);
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
