package com.gluonhq.javaqc.ch08.classiclayers;

public class Main {

    public static void main(String[] args) throws InterruptedException {
        System.err.println("Hello, classic layers");
        Bob bob = new Bob();
        bob.startBob();
        Alice alice = new Alice();
        alice.startAlice();
    }
}
