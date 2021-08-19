package org.streaming.transcribe.on.growing.files.streaming;

import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.streaming.transcribe.TranscribeStreamingDemoFile;
import software.amazon.awssdk.services.transcribestreaming.model.AudioStream;

public class AudioStreamPublisher implements Publisher<AudioStream> {

    private final AudioFileManager manager;
    private static Subscription currentSubscription;


    public AudioStreamPublisher(AudioFileManager manager) {
        this.manager = manager;
    }

    @Override
    public void subscribe(Subscriber<? super AudioStream> subscriber) {
        if (this.currentSubscription == null) {
            this.currentSubscription = new SubscriptionImpl(subscriber, (v) -> manager.getNext(), (v) -> manager.isCompleted());
        } else {
            this.currentSubscription.cancel();
            this.currentSubscription = new SubscriptionImpl(subscriber, (v) -> manager.getNext(), (v) -> manager.isCompleted());
        }
        subscriber.onSubscribe(currentSubscription);
    }
}
