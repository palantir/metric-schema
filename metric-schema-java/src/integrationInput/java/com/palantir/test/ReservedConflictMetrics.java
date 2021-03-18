package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.Optional;

/** Tests that reserved words are escaped. */
public final class ReservedConflictMetrics {
    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION =
            Optional.ofNullable(
                            ReservedConflictMetrics.class.getPackage().getImplementationVersion())
                    .orElse("unknown");

    private final TaggedMetricRegistry registry;

    private ReservedConflictMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static ReservedConflictMetrics of(TaggedMetricRegistry registry) {
        return new ReservedConflictMetrics(
                Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** Uh-oh! */
    @CheckReturnValue
    public IntBuilderLongStage int_() {
        return new IntBuilder();
    }

    /** Meter with a single tag. */
    @CheckReturnValue
    public Meter long_(String int_) {
        return registry.meter(
                MetricName.builder()
                        .safeName("reserved.conflict.long")
                        .putSafeTags("int", int_)
                        .putSafeTags("libraryName", LIBRARY_NAME)
                        .putSafeTags("libraryVersion", LIBRARY_VERSION)
                        .build());
    }

    /** Gauge metric with a single no tags. */
    public void float_(Gauge<?> gauge) {
        registry.registerWithReplacement(floatMetricName(), gauge);
    }

    public static MetricName floatMetricName() {
        return MetricName.builder()
                .safeName("reserved.conflict.float")
                .putSafeTags("libraryName", LIBRARY_NAME)
                .putSafeTags("libraryVersion", LIBRARY_VERSION)
                .build();
    }

    /** Gauge metric with a single tag. */
    @CheckReturnValue
    public DoubleBuilderIntStage double_() {
        return new DoubleBuilder();
    }

    @Override
    public String toString() {
        return "ReservedConflictMetrics{registry=" + registry + '}';
    }

    public interface IntBuildStage {
        @CheckReturnValue
        Histogram build();
    }

    public interface IntBuilderLongStage {
        @CheckReturnValue
        IntBuilderRegistryStage long_(String long_);
    }

    public interface IntBuilderRegistryStage {
        @CheckReturnValue
        IntBuilderIntStage registry_(String registry_);
    }

    public interface IntBuilderIntStage {
        @CheckReturnValue
        IntBuildStage int_(String int_);
    }

    private final class IntBuilder
            implements IntBuilderLongStage,
                    IntBuilderRegistryStage,
                    IntBuilderIntStage,
                    IntBuildStage {
        private String long_;

        private String registry_;

        private String int_;

        @Override
        public Histogram build() {
            return registry.histogram(
                    MetricName.builder()
                            .safeName("reserved.conflict.int")
                            .putSafeTags("long", long_)
                            .putSafeTags("registry", registry_)
                            .putSafeTags("int", int_)
                            .putSafeTags("libraryName", LIBRARY_NAME)
                            .putSafeTags("libraryVersion", LIBRARY_VERSION)
                            .build());
        }

        @Override
        public IntBuilder long_(String long_) {
            Preconditions.checkState(
                    this.long_ == null, "TagDefinition{name: long, values: []} is already set");
            this.long_ =
                    Preconditions.checkNotNull(
                            long_, "TagDefinition{name: long, values: []} is required");
            return this;
        }

        @Override
        public IntBuilder registry_(String registry_) {
            Preconditions.checkState(
                    this.registry_ == null,
                    "TagDefinition{name: registry, values: []} is already set");
            this.registry_ =
                    Preconditions.checkNotNull(
                            registry_, "TagDefinition{name: registry, values: []} is required");
            return this;
        }

        @Override
        public IntBuilder int_(String int_) {
            Preconditions.checkState(
                    this.int_ == null, "TagDefinition{name: int, values: []} is already set");
            this.int_ =
                    Preconditions.checkNotNull(
                            int_, "TagDefinition{name: int, values: []} is required");
            return this;
        }
    }

    public interface DoubleBuildStage {
        void build(Gauge<?> gauge);

        MetricName buildMetricName();
    }

    public interface DoubleBuilderIntStage {
        @CheckReturnValue
        DoubleBuildStage int_(String int_);
    }

    private final class DoubleBuilder implements DoubleBuilderIntStage, DoubleBuildStage {
        private String int_;

        @Override
        public void build(Gauge<?> gauge) {
            registry.registerWithReplacement(buildMetricName(), gauge);
        }

        @Override
        public MetricName buildMetricName() {
            return MetricName.builder()
                    .safeName("reserved.conflict.double")
                    .putSafeTags("int", int_)
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .build();
        }

        @Override
        public DoubleBuilder int_(String int_) {
            Preconditions.checkState(
                    this.int_ == null, "TagDefinition{name: int, values: []} is already set");
            this.int_ =
                    Preconditions.checkNotNull(
                            int_, "TagDefinition{name: int, values: []} is required");
            return this;
        }
    }
}
