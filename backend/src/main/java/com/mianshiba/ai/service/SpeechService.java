package com.mianshiba.ai.service;

import java.util.function.Consumer;

public interface SpeechService {

    String synthesizeToBase64(String text);

    AsrStreamSession createAsrStreamSession(Consumer<String> onPartial,
                                            Consumer<String> onFinal,
                                            Consumer<Throwable> onError);

    interface AsrStreamSession extends AutoCloseable {
        void start();
        void sendAudio(byte[] audio);
        void stop();
        @Override
        void close();
    }
}
