package controllers.nanonis;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.EmptyStackException;
import java.util.Stack;

public class NanonisClientPool {
    static int[] ports = new int[] { 6501, 6502, 6503, 6504 };
    Stack<NanonisClient> clients = new Stack<>();

    public NanonisClientPool() throws UnknownHostException, IOException {
        super();
        for (int i = 0; i < ports.length; i++) {
            clients.push(new NanonisClient("127.0.0.1", ports[i]));
        }
    }

    public NanonisClient getClient() {
        while (true) {
            NanonisClient client;
            try {
                client = clients.pop();
            } catch (EmptyStackException e) {
                Thread.onSpinWait();
                continue;
            }
            return client;
        }
    }

    public void returnClient(NanonisClient client) {
        clients.push(client);
    }

}
