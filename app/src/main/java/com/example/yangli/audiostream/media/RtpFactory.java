
package com.example.yangli.audiostream.media;



import java.net.DatagramSocket;
import java.net.InetAddress;

public class    RtpFactory {
    public static AudioStreamer[] getSystemSupportedAudioStreamer() {
        return new AudioStreamer[] {
                new AudioStreamerImpl(0), new AudioStreamerImpl(8)};
    }

    // returns null if codecId is not supported by the system
    public static AudioStreamer createAudioStreamer(int id, InetAddress localHost, int codecId) {
        return create(codecId);
    }

    public static AudioStreamer createAudioStreamer(
            int codecId, int remoteSampleRate, DatagramSocket socket) {
        AudioStreamerImpl s = create(codecId);
        if (s == null) return null;
        s.set(remoteSampleRate);
        return s;
    }

    private static AudioStreamerImpl create(int codecId) {
        for (AudioStreamer s : getSystemSupportedAudioStreamer()) {
            if (s.getCodecId() == codecId) {
                return new AudioStreamerImpl(codecId);
            }
        }
        return null;
    }

    static Encoder createEncoder(int id) {
        switch (id) {
        case 8:
            return new G711ACodec();
        default:
            return new G711UCodec();
        }
    }

    static Decoder createDecoder(int id) {
        switch (id) {
        case 8:
            return new G711ACodec();
        default:
            return new G711UCodec();
        }
    }
}
