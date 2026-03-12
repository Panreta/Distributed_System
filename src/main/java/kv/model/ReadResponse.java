package kv.model;

public class ReadResponse extends KVBase {
    private int version;
    public ReadResponse() {}
    public ReadResponse(String key, String value, int version) {
        super(key, value); this.version = version;
    }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}