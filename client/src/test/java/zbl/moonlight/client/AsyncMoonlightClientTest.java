package zbl.moonlight.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import zbl.moonlight.core.common.G;
import zbl.moonlight.core.executor.Executor;
import zbl.moonlight.server.MoonlightServer;
import zbl.moonlight.socket.client.ServerNode;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static zbl.moonlight.server.engine.result.Result.Error.INVALID_ARGUMENT;

class AsyncMoonlightClientTest {
    public static final String HOST = "127.0.0.1";
    public static final int PORT = 7820;
    public static final String KV_STORE = "kv_store";

    @BeforeEach
    public void setUp() throws IOException, InterruptedException {
        MoonlightServer.main(new String[0]);
        TimeUnit.SECONDS.sleep(3);
    }

    @Test
    void test_001_asyncKvGet() throws IOException {
        AsyncMoonlightClient client = new AsyncMoonlightClient();
        Executor.start(client);
        SelectionKey selectionKey = client.connect(new ServerNode(HOST, PORT));

        List<byte[]> keys = List.of(G.I.toBytes("key"));

        MoonlightFuture future = client.asyncKvGet(selectionKey, KV_STORE, keys);
        byte[] result = future.get();

        assert result[0] == INVALID_ARGUMENT;
    }
}