package com.palantir.test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
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
    Counter test() {
        return registry.counter(
                MetricName.builder()
                        .safeName("visibility.test")
                        .putSafeTags("libraryName", LIBRARY_NAME)
                        .putSafeTags("libraryVersion", LIBRARY_VERSION)
                        .build());
    }

    /** Tagged gauge metric. */
    ComplexBuilderFooStage complex() {
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

    interface ComplexBuilderFooStage {
        ComplexBuilderBarStage foo(String foo);
    }

    interface ComplexBuilderBarStage {
        ComplexBuildStage bar(String bar);
    }

    private final class ComplexBuilder
            implements ComplexBuilderFooStage, ComplexBuilderBarStage, ComplexBuildStage {
        private String foo;

        private String bar;

        @Override
        public void build(Gauge<?> gauge) {
            registry.registerWithReplacement(
                    MetricName.builder()
                            .safeName("visibility.complex")
                            .putSafeTags("foo", foo)
                            .putSafeTags("bar", bar)
                            .putSafeTags("libraryName", LIBRARY_NAME)
                            .putSafeTags("libraryVersion", LIBRARY_VERSION)
                            .build(),
                    gauge);
        }

        @Override
        public MetricName buildMetricName() {
            return MetricName.builder()
                    .safeName("visibility.complex")
                    .putSafeTags("foo", foo)
                    .putSafeTags("bar", bar)
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .build();
        }

        @Override
        public ComplexBuilder foo(String foo) {
            Preconditions.checkState(this.foo == null, "foo is already set");
            this.foo = Preconditions.checkNotNull(foo, "foo is required");
            return this;
        }

        @Override
        public ComplexBuilder bar(String bar) {
            Preconditions.checkState(this.bar == null, "bar is already set");
            this.bar = Preconditions.checkNotNull(bar, "bar is required");
            return this;
        }
    }
}
