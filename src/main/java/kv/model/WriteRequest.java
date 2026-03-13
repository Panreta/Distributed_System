package kv.model;

/** Request body for PUT /kv and internal replication calls. */
public class WriteRequest {
    private String key;
    private int version;

    public WriteRequest() {}
    public WriteRequest(String key, int version) { this.key = key; this.version = version; }

    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}