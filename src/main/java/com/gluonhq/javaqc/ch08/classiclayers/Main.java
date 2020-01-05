package com.gluonhq.javaqc.ch08.classiclayers;

public class Main {

    static final short appId = 0;
    static final short CQC_PORT_ALICE = 8001;
    static final short CQC_PORT_BOB = 8004;

    static final short APP_PORT_BOB=8103;

    public static void main(String[] args) throws InterruptedException {
        System.err.println("Hello, classic layers");
        Bob bob = new Bob();
        bob.startBob();
        Alice alice = new Alice();
        alice.startAlice();
    }
}
