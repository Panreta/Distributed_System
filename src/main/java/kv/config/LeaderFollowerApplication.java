package kv.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point. Configure via environment variables:
 *   ROLE              = leader | follower
 *   FOLLOWER_URLS     = http://f1:8080,http://f2:8080,...
 *   WRITE_QUORUM_SIZE = 1 | 3 | 5
 *   READ_QUORUM_SIZE  = 1 | 3 | 5
 */
@SpringBootApplication
public class LeaderFollowerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaderFollowerApplication.class, args);
    }
}