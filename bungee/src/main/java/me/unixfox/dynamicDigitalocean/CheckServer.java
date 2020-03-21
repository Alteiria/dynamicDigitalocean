package me.unixfox.dynamicDigitalocean;

import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.common.util.concurrent.MoreExecutors;

import io.netty.util.internal.shaded.org.jctools.queues.MessagePassingQueue.Consumer;
import net.md_5.bungee.api.Callback;
import net.md_5.bungee.api.ServerPing;
import net.md_5.bungee.api.config.ServerInfo;

public class CheckServer {

    private ServerInfo server;

    public CheckServer(ServerInfo server) {
        this.server = server;
    }

    public boolean isOnline() {
        Future<ServerPing> serverPingFuture = MoreExecutors.newDirectExecutorService()
                .submit(createCallable(server::ping));
        try {
            serverPingFuture.get();
            return true;
        } catch (InterruptedException e) {
            // WTF, this should not happen
        } catch (ExecutionException e) {
            // => happens if the callbackConsumer calls back with a non-null error.
        }
        return false;
    }

    public static <V> Callable<V> createCallable(Consumer<Callback<V>> callbackConsumer) {
        return new Callable<V>() {
            private final CountDownLatch countDownLatch = new CountDownLatch(1);

            private V result;
            private Throwable error;

            public void callbackDone(V result, Throwable error) {
                this.result = result;
                this.error = error;
                countDownLatch.countDown();
            }

            @Override
            public V call() throws Exception {
                callbackConsumer.accept(this::callbackDone);
                countDownLatch.await();
                if (this.error != null) {
                    throw new RuntimeException(this.error);
                } else {
                    return this.result;
                }
            }
        };
    }
}