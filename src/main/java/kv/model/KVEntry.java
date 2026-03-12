package kv.model;


public class KVEntry extends KVBase {
    private final int version;
    public KVEntry(String key, String value, int version) {
        super(key, value); this.version = version;
    }
    public int getVersion() { return version; }
}