package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.Optional;

/** General web server metrics. */
public final class MyNamespaceMetrics {
    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION =
            Optional.ofNullable(MyNamespaceMetrics.class.getPackage().getImplementationVersion())
                    .orElse("unknown");

    private final TaggedMetricRegistry registry;

    private MyNamespaceMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static MyNamespaceMetrics of(TaggedMetricRegistry registry) {
        return new MyNamespaceMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** A histogram of the number of bytes written into the response. */
    @CheckReturnValue
    public ResponseSizeBuilderServiceNameStage responseSize() {
        return new ResponseSizeBuilder();
    }

    /** A gauge of the ratio of active workers to the number of workers. */
    public void workerUtilization(Gauge<?> gauge) {
        registry.registerWithReplacement(workerUtilizationMetricName(), gauge);
    }

    public static MetricName workerUtilizationMetricName() {
        return MetricName.builder()
                .safeName("com.palantir.very.long.namespace.worker.utilization")
                .putSafeTags("libraryName", LIBRARY_NAME)
                .putSafeTags("libraryVersion", LIBRARY_VERSION)
                .build();
    }

    @Override
    public String toString() {
        return "MyNamespaceMetrics{registry=" + registry + '}';
    }

    public interface ResponseSizeBuildStage {
        @CheckReturnValue
        Histogram build();
    }

    public interface ResponseSizeBuilderServiceNameStage {
        @CheckReturnValue
        ResponseSizeBuilderEndpointStage serviceName(String serviceName);
    }

    public interface ResponseSizeBuilderEndpointStage {
        @CheckReturnValue
        ResponseSizeBuildStage endpoint(String endpoint);
    }

    private final class ResponseSizeBuilder
            implements ResponseSizeBuilderServiceNameStage,
                    ResponseSizeBuilderEndpointStage,
                    ResponseSizeBuildStage {
        private String serviceName;

        private String endpoint;

        @Override
        public Histogram build() {
            return registry.histogram(
                    MetricName.builder()
                            .safeName("com.palantir.very.long.namespace.response.size")
                            .putSafeTags("service-name", serviceName)
                            .putSafeTags("endpoint", endpoint)
                            .putSafeTags("libraryName", LIBRARY_NAME)
                            .putSafeTags("libraryVersion", LIBRARY_VERSION)
                            .build());
        }

        @Override
        public ResponseSizeBuilder serviceName(String serviceName) {
            Preconditions.checkState(
                    this.serviceName == null,
                    "TagDefinition{name: service-name, values: []} is already set");
            this.serviceName =
                    Preconditions.checkNotNull(
                            serviceName,
                            "TagDefinition{name: service-name, values: []} is required");
            return this;
        }

        @Override
        public ResponseSizeBuilder endpoint(String endpoint) {
            Preconditions.checkState(
                    this.endpoint == null,
                    "TagDefinition{name: endpoint, values: []} is already set");
            this.endpoint =
                    Preconditions.checkNotNull(
                            endpoint, "TagDefinition{name: endpoint, values: []} is required");
            return this;
        }
    }
}
