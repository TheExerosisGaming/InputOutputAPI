package me.exerosis.io.modules;

public abstract class Emitter {
    protected abstract byte[] getStream();

    public void writeTo(Collector collector) {

    }
}
