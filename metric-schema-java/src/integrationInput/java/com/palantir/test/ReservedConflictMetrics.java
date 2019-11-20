package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;

/** Tests that reserved words are escaped. */
public final class ReservedConflictMetrics {
    private final TaggedMetricRegistry registry;

    private ReservedConflictMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static ReservedConflictMetrics of(TaggedMetricRegistry registry) {
        return new ReservedConflictMetrics(
                Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** Uh-oh! */
    public IntBuilderIntStage int_() {
        return new IntBuilder();
    }

    /** Meter with a single tag. */
    public Meter long_(String int_) {
        return registry.meter(
                MetricName.builder()
                        .safeName("reserved.conflict.long")
                        .putSafeTags("int", int_)
                        .build());
    }

    /** Gauge metric with a single no tags. */
    public void float_(Gauge<?> gauge) {
        registry.gauge(MetricName.builder().safeName("reserved.conflict.float").build(), gauge);
    }

    /** Gauge metric with a single tag. */
    public DoubleBuilderIntStage double_() {
        return new DoubleBuilder();
    }

    @Override
    public String toString() {
        return "ReservedConflictMetrics{registry=" + registry + '}';
    }

    public interface IntBuildStage {
        Histogram build();
    }

    public interface IntBuilderIntStage {
        IntBuilderRegistryStage int_(String int_);
    }

    public interface IntBuilderRegistryStage {
        IntBuilderLongStage registry_(String registry_);
    }

    public interface IntBuilderLongStage {
        IntBuildStage long_(String long_);
    }

    private final class IntBuilder
            implements IntBuilderIntStage,
                    IntBuilderRegistryStage,
                    IntBuilderLongStage,
                    IntBuildStage {
        private String int_;

        private String registry_;

        private String long_;

        @Override
        public Histogram build() {
            return registry.histogram(
                    MetricName.builder()
                            .safeName("reserved.conflict.int")
                            .putSafeTags("int", int_)
                            .putSafeTags("registry", registry_)
                            .putSafeTags("long", long_)
                            .build());
        }

        @Override
        public IntBuilder int_(String int_) {
            this.int_ = Preconditions.checkNotNull(int_, "int is required");
            return this;
        }

        @Override
        public IntBuilder registry_(String registry_) {
            this.registry_ = Preconditions.checkNotNull(registry_, "registry is required");
            return this;
        }

        @Override
        public IntBuilder long_(String long_) {
            this.long_ = Preconditions.checkNotNull(long_, "long is required");
            return this;
        }
    }

    public interface DoubleBuildStage {
        void build(Gauge<?> gauge);
    }

    public interface DoubleBuilderIntStage {
        DoubleBuildStage int_(String int_);
    }

    private final class DoubleBuilder implements DoubleBuilderIntStage, DoubleBuildStage {
        private String int_;

        @Override
        public void build(Gauge<?> gauge) {
            registry.gauge(
                    MetricName.builder()
                            .safeName("reserved.conflict.double")
                            .putSafeTags("int", int_)
                            .build(),
                    gauge);
        }

        @Override
        public DoubleBuilder int_(String int_) {
            this.int_ = Preconditions.checkNotNull(int_, "int is required");
            return this;
        }
    }
}
