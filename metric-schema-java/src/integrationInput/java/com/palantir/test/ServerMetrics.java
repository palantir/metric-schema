package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.Safe;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Properties;

/** General web server metrics. */
public final class ServerMetrics {
    private static final String JAVA_VERSION = System.getProperty("java.version", "unknown");

    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION = loadLibraryVersion();

    private static final MetricName workerUtilizationMetricName = MetricName.builderWithExpectedTags(3)
            .safeName("server.worker.utilization")
            .putSafeTags("javaVersion", JAVA_VERSION)
            .putSafeTags("libraryName", LIBRARY_NAME)
            .putSafeTags("libraryVersion", LIBRARY_VERSION)
            .build();

    private final TaggedMetricRegistry registry;

    private ServerMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    private static String loadLibraryVersion() {
        try (InputStream stream = ServerMetrics.class.getResourceAsStream("ServerMetrics.properties")) {
            if (stream == null) {
                // Only written when a release version is known at build time
                return Objects.requireNonNullElse(
                        ServerMetrics.class.getPackage().getImplementationVersion(), "unknown");
            }
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            return Preconditions.checkNotNull(
                    properties.getProperty("libraryVersion"), "Library version resource is missing libraryVersion");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static ServerMetrics of(TaggedMetricRegistry registry) {
        return new ServerMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** A histogram of the number of bytes written into the response. */
    @CheckReturnValue
    public ResponseSizeBuilderServiceNameStage responseSize() {
        return new ResponseSizeBuilder();
    }

    /** A gauge of the ratio of active workers to the number of workers. */
    public void workerUtilization(Gauge<? extends Number> gauge) {
        registry.registerWithReplacement(workerUtilizationMetricName(), gauge);
    }

    public static MetricName workerUtilizationMetricName() {
        return workerUtilizationMetricName;
    }

    @Override
    public String toString() {
        return "ServerMetrics{registry=" + registry + '}';
    }

    public interface ResponseSizeBuildStage {
        @CheckReturnValue
        Histogram build();

        @CheckReturnValue
        MetricName buildMetricName();
    }

    public interface ResponseSizeBuilderServiceNameStage {
        @CheckReturnValue
        ResponseSizeBuilderEndpointStage serviceName(@Safe String serviceName);
    }

    public interface ResponseSizeBuilderEndpointStage {
        @CheckReturnValue
        ResponseSizeBuildStage endpoint(@Safe String endpoint);
    }

    private final class ResponseSizeBuilder
            implements ResponseSizeBuilderServiceNameStage, ResponseSizeBuilderEndpointStage, ResponseSizeBuildStage {
        private String serviceName;

        private String endpoint;

        @Override
        public ResponseSizeBuilder serviceName(@Safe String serviceName) {
            Preconditions.checkState(this.serviceName == null, "service-name is already set");
            this.serviceName = Preconditions.checkNotNull(serviceName, "service-name is required");
            return this;
        }

        @Override
        public ResponseSizeBuilder endpoint(@Safe String endpoint) {
            Preconditions.checkState(this.endpoint == null, "endpoint is already set");
            this.endpoint = Preconditions.checkNotNull(endpoint, "endpoint is required");
            return this;
        }

        @Override
        public Histogram build() {
            return registry.histogram(buildMetricName());
        }

        @Override
        public MetricName buildMetricName() {
            return MetricName.builderWithExpectedTags(5)
                    .safeName("server.response.size")
                    .putSafeTags("endpoint", endpoint)
                    .putSafeTags("javaVersion", JAVA_VERSION)
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .putSafeTags("service-name", serviceName)
                    .build();
        }
    }
}
