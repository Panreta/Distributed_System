package kv.model;


/** Request body for PUT /kv and internal replication calls. */
public class WriteRequest extends KVBase {
    private String key;
    private String value;
    private int version; // set by leader before propagating

    public WriteRequest() {}
    public WriteRequest(String key, String value, int version) {
        super(key, value); this.version = version;
    }


    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
}