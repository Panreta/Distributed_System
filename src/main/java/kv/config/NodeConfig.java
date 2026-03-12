package kv.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * All N/R/W settings come from environment variables so you can
 * switch configurations without rebuilding the image.
 */
@Configuration
public class NodeConfig {

    @Value("${node.role:leader}")
    private String role;

    @Value("${node.follower-urls}")
    private String followerUrlsRaw;

    @Value("${node.write-quorum}")
    private int writeQuorum;

    @Value("${node.read-quorum}")
    private int readQuorum;

    @Value("${node.write-delay-ms}")
    private long writeDelayMs;

    @Value("${node.read-delay-ms}")
    private long readDelayMs;

    public boolean isLeader() { return "leader".equalsIgnoreCase(role); }
    public String getRole() { return role; }

    public List<String> getFollowerUrls() {
        if (followerUrlsRaw == null || followerUrlsRaw.isBlank()) return Collections.emptyList();
        return Arrays.stream(followerUrlsRaw.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
    }

    public int getWriteQuorum() { return writeQuorum; }
    public int getReadQuorum() { return readQuorum; }
    public long getWriteDelayMs() { return writeDelayMs; }
    public long getReadDelayMs() { return readDelayMs; }
}