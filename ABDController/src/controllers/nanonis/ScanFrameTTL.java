package controllers.nanonis;

import java.io.IOException;

import controllers.nanonis.records.ScanFrame;

public class ScanFrameTTL {
    private long ttlMillis = 1000;
    private long writeDelayMillis = 100;

    private long lastLoad = 0;
    private long lastModify = 0;
    public float centerXM;
    public float centerYM;
    public float widthM;
    public float heightM;
    public float angleDeg;

    private NanonisClient client;

    public ScanFrameTTL(NanonisClient client) {
        super();

        this.client = client;
    }

    public void maybeLoad() throws IOException, NanonisException, ResponseException {
        if (System.currentTimeMillis() < lastLoad + ttlMillis) {
            return;
        }
        ScanFrame scanFrame = client.ScanFrameGet();
        centerXM = scanFrame.centerXM();
        centerYM = scanFrame.centerYM();
        widthM = scanFrame.widthM();
        heightM = scanFrame.heightM();
        angleDeg = scanFrame.angleDeg();
        lastLoad = System.currentTimeMillis();
    }

    public void maybeStore() throws IOException, NanonisException, ResponseException {
        client.ScanFrameSet(centerXM, centerYM, widthM, heightM, angleDeg);
        // boolean threadRunning = lastModify != 0;
        // lastModify = System.currentTimeMillis();
        // System.out.println("[TTL] updating value");
        // if (!threadRunning) {
        //     new Thread() {
        //         public void run() {
        //             while (System.currentTimeMillis() < lastModify + writeDelayMillis) {
        //                 Thread.onSpinWait();
        //             }
        //             System.out.println("[TTL] Executing store");
        //             lastModify = 0;
        //             try {
        //                 client.ScanFrameSet(centerXM, centerYM, widthM, heightM, angleDeg);
        //             } catch (IOException | NanonisException | ResponseException e) {
        //                 e.printStackTrace();
        //             }
        //         }
        //     }.start();
        // }
    }
}
