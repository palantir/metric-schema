package com.palantir.test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.Optional;

/** Tests we respect javaVisibility */
final class VisibilityMetrics {
    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION =
            Optional.ofNullable(VisibilityMetrics.class.getPackage().getImplementationVersion())
                    .orElse("unknown");

    private final TaggedMetricRegistry registry;

    private VisibilityMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static VisibilityMetrics of(TaggedMetricRegistry registry) {
        return new VisibilityMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** just a metric */
    @CheckReturnValue
    Counter test() {
        return registry.counter(
                MetricName.builder()
                        .safeName("visibility.test")
                        .putSafeTags("libraryName", LIBRARY_NAME)
                        .putSafeTags("libraryVersion", LIBRARY_VERSION)
                        .build());
    }

    /** Tagged gauge metric. */
    @CheckReturnValue
    ComplexBuilderBarStage complex() {
        return new ComplexBuilder();
    }

    @Override
    public String toString() {
        return "VisibilityMetrics{registry=" + registry + '}';
    }

    interface ComplexBuildStage {
        void build(Gauge<?> gauge);

        MetricName buildMetricName();
    }

    interface ComplexBuilderBarStage {
        @CheckReturnValue
        ComplexBuilderFooStage bar(String bar);
    }

    interface ComplexBuilderFooStage {
        @CheckReturnValue
        ComplexBuildStage foo(String foo);
    }

    private final class ComplexBuilder
            implements ComplexBuilderBarStage, ComplexBuilderFooStage, ComplexBuildStage {
        private String bar;

        private String foo;

        @Override
        public void build(Gauge<?> gauge) {
            registry.registerWithReplacement(buildMetricName(), gauge);
        }

        @Override
        public MetricName buildMetricName() {
            return MetricName.builder()
                    .safeName("visibility.complex")
                    .putSafeTags("bar", bar)
                    .putSafeTags("foo", foo)
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .build();
        }

        @Override
        public ComplexBuilder bar(String bar) {
            Preconditions.checkState(
                    this.bar == null, "TagDefinition{name: bar, values: []} is already set");
            this.bar =
                    Preconditions.checkNotNull(
                            bar, "TagDefinition{name: bar, values: []} is required");
            return this;
        }

        @Override
        public ComplexBuilder foo(String foo) {
            Preconditions.checkState(
                    this.foo == null, "TagDefinition{name: foo, values: []} is already set");
            this.foo =
                    Preconditions.checkNotNull(
                            foo, "TagDefinition{name: foo, values: []} is required");
            return this;
        }
    }
}
