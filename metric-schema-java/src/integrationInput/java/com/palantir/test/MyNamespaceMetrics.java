package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;

/** General web server metrics. */
public final class MyNamespaceMetrics {
    private final TaggedMetricRegistry registry;

    private MyNamespaceMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static MyNamespaceMetrics of(TaggedMetricRegistry registry) {
        return new MyNamespaceMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** A histogram of the number of bytes written into the response. */
    public ResponseSizeBuilderServiceNameStage responseSize() {
        return new ResponseSizeBuilder();
    }

    /** A gauge of the ratio of active workers to the number of workers. */
    public void workerUtilization(Gauge<?> gauge) {
        registry.gauge(
                MetricName.builder()
                        .safeName("com.palantir.very.long.namespace.worker.utilization")
                        .build(),
                gauge);
    }

    @Override
    public String toString() {
        return "MyNamespaceMetrics{registry=" + registry + '}';
    }

    public interface ResponseSizeBuildStage {
        Histogram build();
    }

    public interface ResponseSizeBuilderServiceNameStage {
        ResponseSizeBuilderEndpointStage serviceName(String serviceName);
    }

    public interface ResponseSizeBuilderEndpointStage {
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
                            .build());
        }

        @Override
        public ResponseSizeBuilder serviceName(String serviceName) {
            this.serviceName = Preconditions.checkNotNull(serviceName, "service-name is required");
            return this;
        }

        @Override
        public ResponseSizeBuilder endpoint(String endpoint) {
            this.endpoint = Preconditions.checkNotNull(endpoint, "endpoint is required");
            return this;
        }
    }
}
