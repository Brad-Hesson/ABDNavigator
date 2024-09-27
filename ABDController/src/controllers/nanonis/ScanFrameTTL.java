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

    private NanonisClientPool clients;

    public ScanFrameTTL(NanonisClientPool clients) {
        super();

        this.clients = clients;
    }

    public void maybeLoad() throws IOException, NanonisException, ResponseException {
        if (System.currentTimeMillis() < lastLoad + ttlMillis) {
            return;
        }
        NanonisClient client = clients.getClient();
        ScanFrame scanFrame;
        try {
            scanFrame = client.ScanFrameGet();
        } finally {
            clients.returnClient(client);
        }
        centerXM = scanFrame.centerXM();
        centerYM = scanFrame.centerYM();
        widthM = scanFrame.widthM();
        heightM = scanFrame.heightM();
        angleDeg = scanFrame.angleDeg();
        lastLoad = System.currentTimeMillis();
    }

    public void maybeStore() throws IOException, NanonisException, ResponseException {
        NanonisClient client = clients.getClient();
        try {
            client.ScanFrameSet(centerXM, centerYM, widthM, heightM, angleDeg);
        } finally {
            clients.returnClient(client);
        }
    }
}
