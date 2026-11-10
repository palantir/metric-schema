package com.palantir.test.missingversionkey;

import com.codahale.metrics.Gauge;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Objects;
import java.util.Properties;

/** Test data metric. */
public final class MissingVersionKeyMetrics {
    private static final String JAVA_VERSION = System.getProperty("java.version", "unknown");

    private static final String LIBRARY_NAME = "specific_libraryVersion";

    private static final String LIBRARY_VERSION = loadLibraryVersion();

    private static final MetricName utilizationMetricName = MetricName.builderWithExpectedTags(3)
            .safeName("missing.version.key.utilization")
            .putSafeTags("javaVersion", JAVA_VERSION)
            .putSafeTags("libraryName", LIBRARY_NAME)
            .putSafeTags("libraryVersion", LIBRARY_VERSION)
            .build();

    private final TaggedMetricRegistry registry;

    private MissingVersionKeyMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    private static String loadLibraryVersion() {
        try (InputStream stream = MissingVersionKeyMetrics.class.getResourceAsStream("metric-schema.properties")) {
            if (stream == null) {
                // Only written when a release version is known at build time
                return Objects.requireNonNullElse(
                        MissingVersionKeyMetrics.class.getPackage().getImplementationVersion(), "unknown");
            }
            Properties properties = new Properties();
            properties.load(stream);
            return Preconditions.checkNotNull(
                    properties.getProperty("libraryVersion"), "Library version resource is missing libraryVersion");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static MissingVersionKeyMetrics of(TaggedMetricRegistry registry) {
        return new MissingVersionKeyMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
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
        return "MissingVersionKeyMetrics{registry=" + registry + '}';
    }
}
