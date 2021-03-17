package com.palantir.test;

import com.codahale.metrics.Meter;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.Optional;

/** General web server metrics. */
public final class MonitorsMetrics {
    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION =
            Optional.ofNullable(MonitorsMetrics.class.getPackage().getImplementationVersion())
                    .orElse("unknown");

    private final TaggedMetricRegistry registry;

    private MonitorsMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static MonitorsMetrics of(TaggedMetricRegistry registry) {
        return new MonitorsMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** Measures number of installations that were processed */
    @CheckReturnValue
    public ProcessingBuilderResultStage processing() {
        return new ProcessingBuilder();
    }

    @Override
    public String toString() {
        return "MonitorsMetrics{registry=" + registry + '}';
    }

    public enum Processing_Result {
        SUCCESS("TagValue{value: success}"),

        FAILURE("TagValue{value: failure}");

        private final String value;

        Processing_Result(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }

    public interface ProcessingBuildStage {
        @CheckReturnValue
        Meter build();
    }

    public interface ProcessingBuilderResultStage {
        @CheckReturnValue
        ProcessingBuilderTypeStage result(Processing_Result result);
    }

    public interface ProcessingBuilderTypeStage {
        @CheckReturnValue
        ProcessingBuildStage type(String type);
    }

    private final class ProcessingBuilder
            implements ProcessingBuilderResultStage,
                    ProcessingBuilderTypeStage,
                    ProcessingBuildStage {
        private Processing_Result result;

        private String type;

        @Override
        public Meter build() {
            return registry.meter(
                    MetricName.builder()
                            .safeName("monitors.processing")
                            .putSafeTags("result", result.getValue())
                            .putSafeTags("type", type)
                            .putSafeTags("libraryName", LIBRARY_NAME)
                            .putSafeTags("libraryVersion", LIBRARY_VERSION)
                            .build());
        }

        @Override
        public ProcessingBuilder result(Processing_Result result) {
            Preconditions.checkState(this.result == null, "result is already set");
            this.result = Preconditions.checkNotNull(result, "result is required");
            return this;
        }

        @Override
        public ProcessingBuilder type(String type) {
            Preconditions.checkState(this.type == null, "type is already set");
            this.type = Preconditions.checkNotNull(type, "type is required");
            return this;
        }
    }
}
