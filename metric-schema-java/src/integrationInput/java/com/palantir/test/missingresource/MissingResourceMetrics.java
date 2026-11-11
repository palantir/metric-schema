package com.palantir.test.missingresource;

import com.codahale.metrics.Gauge;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/** Test data metric. */
public final class MissingResourceMetrics {
    private static final String JAVA_VERSION = System.getProperty("java.version", "unknown");

    private static final String LIBRARY_NAME = "specific_libraryVersion";

    private static final String LIBRARY_VERSION = loadLibraryVersion();

    private static final MetricName utilizationMetricName = MetricName.builderWithExpectedTags(3)
            .safeName("missing.resource.utilization")
            .putSafeTags("javaVersion", JAVA_VERSION)
            .putSafeTags("libraryName", LIBRARY_NAME)
            .putSafeTags("libraryVersion", LIBRARY_VERSION)
            .build();

    private final TaggedMetricRegistry registry;

    private MissingResourceMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    private static String loadLibraryVersion() {
        try (InputStream stream = Preconditions.checkNotNull(
                MissingResourceMetrics.class.getResourceAsStream("metric-schema.properties"),
                "Missing generated resource metric-schema.properties")) {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            // Only recorded when a release version is known at build time
            return Objects.requireNonNullElseGet(
                    properties.getProperty("libraryVersion"),
                    () -> Objects.requireNonNullElse(
                            MissingResourceMetrics.class.getPackage().getImplementationVersion(), "unknown"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MissingResourceMetrics of(TaggedMetricRegistry registry) {
        return new MissingResourceMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** Test data. */
    public void utilization(Gauge<? extends Number> gauge) {
        registry.registerWithReplacement(utilizationMetricName(), gauge);
    }

    public static MetricName utilizationMetricName() {
        return utilizationMetricName;
    }

    @Override
    public String toString() {
        return "MissingResourceMetrics{registry=" + registry + '}';
    }
}
