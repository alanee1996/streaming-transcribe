package org.streaming.transcribe;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.transcribestreaming.TranscribeStreamingAsyncClient;
import software.amazon.awssdk.services.transcribestreaming.model.*;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

public class TranscribeStreamingDemoApp {

    private static final Region REGION = Region.US_EAST_1;
    private static TranscribeStreamingAsyncClient client;
    private static StartStreamTranscriptionResponseHandler responseHandler = getResponseHandler();
    private static AudioStreamPublisher publisher;

    public static void main(String args[]) throws URISyntaxException, ExecutionException, InterruptedException, LineUnavailableException {
        try {
            client = TranscribeStreamingAsyncClient.builder()
                    .credentialsProvider(getCredentials())
                    .region(REGION)
                    .build();
            publisher = new AudioStreamPublisher(getStreamFromMic());
            CompletableFuture<Void> result = client.startStreamTranscription(getRequest(16_000),
                    publisher,
                    responseHandler);
            result.get();
            client.close();
        } catch (Exception ex) {
            throw ex;
        }
    }

    private static void close() {
        try {
            if (publisher != null) {
                publisher.inputStream.close();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static InputStream getStreamFromMic() throws LineUnavailableException {

        // Signed PCM AudioFormat with 16kHz, 16 bit sample size, mono
        int sampleRate = 16000;
        AudioFormat format = new AudioFormat(sampleRate, 16, 1, true, false);
//        AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED,sampleRate,16,1,5,2,false);
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.out.println("Line not supported");
            System.exit(0);
        }

        TargetDataLine line = (TargetDataLine) AudioSystem.getLine(info);
        line.open(format);
        line.start();

        InputStream audioStream = new AudioInputStream(line);
        return audioStream;
    }

    private static AwsCredentialsProvider getCredentials() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(
                Settings.ACCESSKEYID,
                Settings.SECRET_ACCESSKEY);
        return StaticCredentialsProvider.create(awsCreds);
//        return DefaultCredentialsProvider.create();
    }

    private static StartStreamTranscriptionRequest getRequest(Integer mediaSampleRateHertz) {
        return StartStreamTranscriptionRequest.builder()
                .languageCode(LanguageCode.EN_US.toString())
                .mediaEncoding(MediaEncoding.PCM)
                .vocabularyFilterName("badwordSimple")
                .vocabularyFilterMethod(VocabularyFilterMethod.MASK)
                .mediaSampleRateHertz(mediaSampleRateHertz)
                .build();
    }

    private static StartStreamTranscriptionResponseHandler getResponseHandler() {
        return StartStreamTranscriptionResponseHandler.builder()
                .onResponse(r -> {
                    System.out.println("Received Initial response");
                })
                .onError(e -> {
                    System.out.println(e.getMessage());
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    System.out.println("Error Occurred: " + sw.toString());
                })
                .onComplete(() -> {
                    System.out.println("=== All records stream successfully ===");
                }).subscriber(event -> {
                    List<Result> results = ((TranscriptEvent) event).transcript().results();
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
                    LocalDateTime now = LocalDateTime.now();
                    if (results.size() > 0) {
                        if (results.size() > 1) {
                            System.out.println("--------Greater than 2 - actual = " + results.size() + "---------");
                        }
                        if (!results.get(0).alternatives().get(0).transcript().isEmpty()) {
                            print(results);
                            String transcript = results.get(0).alternatives().get(0).transcript();
                            if (transcript.toLowerCase(Locale.ROOT).contains("complete") && responseHandler != null) {
                                responseHandler.complete();
                                close();
                            }
                            System.out.println(transcript);
                        }
                    }
                }).build();

    }

    private static void print(List<Result> results) {
        try {
            System.out.println("-----------------------------------------------------------");
//            CompletableFuture.runAsync(() -> {
            results.stream().forEach(result -> System.out.println(result.toString()));
//            });
            System.out.println("+++++++++++++++++++++++++++++++++++++++++++++++++++++++++++");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private InputStream getStreamFromFile(String audioFileName) {
        try {
            File inputFile = new File(getClass().getClassLoader().getResource(audioFileName).getFile());
            InputStream audioStream = new FileInputStream(inputFile);
            return audioStream;
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private static class AudioStreamPublisher implements Publisher<AudioStream> {
        private final InputStream inputStream;
        private static Subscription currentSubscription;


        private AudioStreamPublisher(InputStream inputStream) {
            this.inputStream = inputStream;
        }

        @Override
        public void subscribe(Subscriber<? super AudioStream> s) {

            if (this.currentSubscription == null) {
                this.currentSubscription = new SubscriptionImpl(s, inputStream);
            } else {
                this.currentSubscription.cancel();
                this.currentSubscription = new SubscriptionImpl(s, inputStream);
            }
            s.onSubscribe(currentSubscription);
        }
    }

    public static class SubscriptionImpl implements Subscription {
        private static final int CHUNK_SIZE_IN_BYTES = 1024 * 1;
        private final Subscriber<? super AudioStream> subscriber;
        private final InputStream inputStream;
        private ExecutorService executor = Executors.newFixedThreadPool(1);
        private AtomicLong demand = new AtomicLong(0);

        SubscriptionImpl(Subscriber<? super AudioStream> s, InputStream inputStream) {
            this.subscriber = s;
            this.inputStream = inputStream;
        }

        @Override
        public void request(long n) {
            if (n <= 0) {
                subscriber.onError(new IllegalArgumentException("Demand must be positive"));
            }

            demand.getAndAdd(n);

            executor.submit(() -> {
                try {
                    do {
                        ByteBuffer audioBuffer = getNextEvent();
                        if (audioBuffer.remaining() > 0) {
                            AudioEvent audioEvent = audioEventFromBuffer(audioBuffer);
                            subscriber.onNext(audioEvent);
                        } else {
                            subscriber.onComplete();
                            break;
                        }
                    } while (demand.decrementAndGet() > 0);
                } catch (Exception e) {
                    subscriber.onError(e);
                }
            });
        }

        @Override
        public void cancel() {
            executor.shutdown();
        }

        private ByteBuffer getNextEvent() {
            ByteBuffer audioBuffer = null;
            byte[] audioBytes = new byte[CHUNK_SIZE_IN_BYTES];

            int len = 0;
            try {
                len = inputStream.read(audioBytes);

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

        private AudioEvent audioEventFromBuffer(ByteBuffer bb) {
            return AudioEvent.builder()
                    .audioChunk(SdkBytes.fromByteBuffer(bb))
                    .build();
        }
    }
}
