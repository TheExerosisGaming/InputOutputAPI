package me.exerosis.io.modules;

import java.io.IOException;
import java.io.InputStream;

public abstract class Collector {
    private InputStream _inputStream;

    public void read(byte[] stream) {
        try {
            _inputStream.read(stream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
