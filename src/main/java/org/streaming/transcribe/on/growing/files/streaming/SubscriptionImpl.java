package org.streaming.transcribe.on.growing.files.streaming;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.transcribestreaming.model.AudioEvent;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class SubscriptionImpl implements Subscription {
    private static final int CHUNK_SIZE_IN_BYTES = 1024 * 1;
    private InputStream currentStream;
    private Function<Void, InputStream> getStream;
    private Function<Void, Boolean> isComplete;
    private final Subscriber<? super AudioStream> subscriber;
    private ExecutorService executor = Executors.newFixedThreadPool(1);

    public SubscriptionImpl(Subscriber<? super AudioStream> subscriber, Function<Void, InputStream> getStream, Function<Void, Boolean> isComplete) {
        this.subscriber = subscriber;
        this.getStream = getStream;
        this.isComplete = isComplete;
    }

    @Override
    public void request(long l) {
        executor.submit(() -> {
            try {
                do {
                    if (currentStream == null) {
                        currentStream = getStream.apply(null);
                    } else {
                        ByteBuffer audioBuffer = getNextEvent();
                        if (audioBuffer.remaining() > 0) {
                            AudioEvent audioEvent = audioEventFromBuffer(audioBuffer);
                            subscriber.onNext(audioEvent);
                        } else {
                            currentStream = null;
                        }
                    }

                } while (!isComplete.apply(null) || currentStream != null);
                subscriber.onComplete();
            } catch (Exception e) {
                subscriber.onError(e);
            }
        });
    }

    private ByteBuffer getNextEvent() {
        ByteBuffer audioBuffer = null;
        byte[] audioBytes = new byte[CHUNK_SIZE_IN_BYTES];

        int len = 0;
        try {
            len = currentStream.read(audioBytes);

            if (len <= 0) {
                audioBuffer = ByteBuffer.allocate(0);
            } else {
                audioBuffer = ByteBuffer.wrap(audioBytes, 0, len);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        return audioBuffer;
    }

    @Override
    public void cancel() {
        executor.shutdown();
    }

    private AudioEvent audioEventFromBuffer(ByteBuffer bb) {
        return AudioEvent.builder()
                .audioChunk(SdkBytes.fromByteBuffer(bb))
                .build();
    }
}
