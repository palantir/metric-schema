package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Test data metric. */
public final class ProvidedVersionMetrics {
    private static final String JAVA_VERSION = System.getProperty("java.version", "unknown");

    private static final String LIBRARY_NAME = "specific_libraryVersion";

    private static final String LIBRARY_VERSION = loadLibraryVersion();

    private static final MetricName utilizationMetricName = MetricName.builderWithExpectedTags(3)
            .safeName("provided.version.utilization")
            .putSafeTags("javaVersion", JAVA_VERSION)
            .putSafeTags("libraryName", LIBRARY_NAME)
            .putSafeTags("libraryVersion", LIBRARY_VERSION)
            .build();

    private final TaggedMetricRegistry registry;

    private ProvidedVersionMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    private static String loadLibraryVersion() {
        try (InputStream stream =
                ProvidedVersionMetrics.class.getResourceAsStream("ProvidedVersionMetrics.libraryVersion")) {
            if (stream != null) {
                String version = new String(stream.readAllBytes(), StandardCharsets.UTF_8).trim();
                if (!version.isEmpty()) {
                    return version;
                }
            }
        } catch (IOException | RuntimeException e) {
            // Fall back to the package version
        }
        return Objects.requireNonNullElse(
                ProvidedVersionMetrics.class.getPackage().getImplementationVersion(), "unknown");
    }

    public static ProvidedVersionMetrics of(TaggedMetricRegistry registry) {
        return new ProvidedVersionMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
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
        return "ProvidedVersionMetrics{registry=" + registry + '}';
    }
}
