package com.palantir.test;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.Safe;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.Objects;

/**
 * Tests that reserved words are escaped.
 */
public final class ReservedConflictMetrics {
    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION = Objects.requireNonNullElse(
            ReservedConflictMetrics.class.getPackage().getImplementationVersion(), "unknown");

    private final TaggedMetricRegistry registry;

    private ReservedConflictMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static ReservedConflictMetrics of(TaggedMetricRegistry registry) {
        return new ReservedConflictMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /**
     * Uh-oh!
     */
    @CheckReturnValue
    public IntBuilderIntStage int_() {
        return new IntBuilder();
    }

    /**
     * Meter with a single tag.
     */
    @CheckReturnValue
    public Meter long_(@Safe String int_) {
        return registry.meter(MetricName.builder()
                .safeName("reserved.conflict.long")
                .putSafeTags("int", int_)
                .putSafeTags("libraryName", LIBRARY_NAME)
                .putSafeTags("libraryVersion", LIBRARY_VERSION)
                .build());
    }

    /**
     * Gauge metric with a single no tags.
     */
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

    /**
     * Gauge metric with a single tag.
     */
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

    public interface IntBuilderIntStage {
        @CheckReturnValue
        IntBuilderRegistryStage int_(@Safe String int_);
    }

    public interface IntBuilderRegistryStage {
        @CheckReturnValue
        IntBuilderLongStage registry_(@Safe String registry_);
    }

    public interface IntBuilderLongStage {
        @CheckReturnValue
        IntBuildStage long_(@Safe String long_);
    }

    private final class IntBuilder
            implements IntBuilderIntStage, IntBuilderRegistryStage, IntBuilderLongStage, IntBuildStage {
        private String int_;

        private String registry_;

        private String long_;

        @Override
        public Histogram build() {
            return registry.histogram(MetricName.builder()
                    .safeName("reserved.conflict.int")
                    .putSafeTags("int", int_)
                    .putSafeTags("registry", registry_)
                    .putSafeTags("long", long_)
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .build());
        }

        @Override
        public IntBuilder int_(@Safe String int_) {
            Preconditions.checkState(this.int_ == null, "int is already set");
            this.int_ = Preconditions.checkNotNull(int_, "int is required");
            return this;
        }

        @Override
        public IntBuilder registry_(@Safe String registry_) {
            Preconditions.checkState(this.registry_ == null, "registry is already set");
            this.registry_ = Preconditions.checkNotNull(registry_, "registry is required");
            return this;
        }

        @Override
        public IntBuilder long_(@Safe String long_) {
            Preconditions.checkState(this.long_ == null, "long is already set");
            this.long_ = Preconditions.checkNotNull(long_, "long is required");
            return this;
        }
    }

    public interface DoubleBuildStage {
        void build(Gauge<?> gauge);

        MetricName buildMetricName();
    }

    public interface DoubleBuilderIntStage {
        @CheckReturnValue
        DoubleBuildStage int_(@Safe String int_);
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
        public DoubleBuilder int_(@Safe String int_) {
            Preconditions.checkState(this.int_ == null, "int is already set");
            this.int_ = Preconditions.checkNotNull(int_, "int is required");
            return this;
        }
    }
}
