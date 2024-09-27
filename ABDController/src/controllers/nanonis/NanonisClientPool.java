package controllers.nanonis;

import java.io.IOException;
import java.net.UnknownHostException;
import java.util.EmptyStackException;
import java.util.Stack;

public class NanonisClientPool {
    static int[] ports = new int[] { 6501, 6502, 6503, 6504 };
    Stack<NanonisClient> clients = new Stack<>();

    public NanonisClientPool(String host) throws UnknownHostException, IOException {
        super();
        for (int i = 0; i < ports.length; i++) {
            clients.push(new NanonisClient(host, ports[i], this));
        }
    }

    public void close() throws IOException {
        for (int i = 0; i < ports.length; i++) {
            NanonisClient client = getClient();
            client.shutdown();
            Thread.onSpinWait();
        }
    }

    public NanonisClient getClient() {
        NanonisClient client;
        while (true) {
            try {
                client = clients.pop();
            } catch (EmptyStackException e) {
                Thread.onSpinWait();
                continue;
            }
            break;
        }
        return client;
    }

    public void returnClient(NanonisClient client) {
        clients.push(client);
    }

}
