package com.eventitta.infra.media.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

@Getter
@Setter
@ConfigurationProperties(prefix = "media")
public class MediaProperties {

    private Upload upload = new Upload();
    private Cleanup cleanup = new Cleanup();
    private Delivery delivery = new Delivery();
    private Variant variant = new Variant();

    @Getter
    @Setter
    public static class Upload {
        private Category postImage = new Category(5, DataSize.ofMegabytes(5));
        private Category profileImage = new Category(1, DataSize.ofMegabytes(2));
    }

    @Getter
    @Setter
    public static class Category {
        private int maxCount;
        private DataSize maxFileSize;

        public Category() {
        }

        public Category(int maxCount, DataSize maxFileSize) {
            this.maxCount = maxCount;
            this.maxFileSize = maxFileSize;
        }
    }

    @Getter
    @Setter
    public static class Cleanup {
        private Duration tempRetention = Duration.ofDays(7);
        private Duration releasedRetention = Duration.ofDays(30);
    }

    @Getter
    @Setter
    public static class Delivery {
        private String cdnBaseUrl;
    }

    @Getter
    @Setter
    public static class Variant {
        private int postThumbLongEdge = 480;
        private int postDetailLongEdge = 1600;
        private int profileAvatarSize = 256;
        private float webpQuality = 0.85f;
    }
}
