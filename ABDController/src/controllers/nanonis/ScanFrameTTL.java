package controllers.nanonis;

import java.io.IOException;

import controllers.nanonis.records.ScanFrame;

public class ScanFrameTTL {
    private long ttlMillis = 1000;

    private long lastLoad = 0;
    public float centerXM;
    public float centerYM;
    public float widthM;
    public float heightM;
    public float angleDeg;

    private NanonisClientPool pool;

    public ScanFrameTTL(NanonisClientPool pool) {
        super();

        this.pool = pool;
    }

    public void maybeLoad() throws IOException, NanonisException, ResponseException {
        if (System.currentTimeMillis() < lastLoad + ttlMillis) {
            return;
        }
        ScanFrame scanFrame;
        try (NanonisClient client = pool.getClient()) {
            scanFrame = client.ScanFrameGet();
        }
        centerXM = scanFrame.centerXM();
        centerYM = scanFrame.centerYM();
        widthM = scanFrame.widthM();
        heightM = scanFrame.heightM();
        angleDeg = scanFrame.angleDeg();
        lastLoad = System.currentTimeMillis();
    }

    public void maybeStore() throws IOException, NanonisException, ResponseException {
        try (NanonisClient client = pool.getClient()) {
            client.ScanFrameSet(centerXM, centerYM, widthM, heightM, angleDeg);
        }
    }
}
