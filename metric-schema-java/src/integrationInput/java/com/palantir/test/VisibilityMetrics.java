package com.palantir.test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
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

/** Tests we respect javaVisibility */
final class VisibilityMetrics {
    private static final String JAVA_VERSION = System.getProperty("java.version", "unknown");

    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION = loadLibraryVersion();

    private static final MetricName testMetricName = MetricName.builderWithExpectedTags(3)
            .safeName("visibility.test")
            .putSafeTags("javaVersion", JAVA_VERSION)
            .putSafeTags("libraryName", LIBRARY_NAME)
            .putSafeTags("libraryVersion", LIBRARY_VERSION)
            .build();

    private final TaggedMetricRegistry registry;

    private VisibilityMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    private static String loadLibraryVersion() {
        try (InputStream stream = Preconditions.checkNotNull(
                VisibilityMetrics.class.getResourceAsStream("witchcraft-metric-schema.properties"),
                "Missing generated resource witchcraft-metric-schema.properties")) {
            Properties properties = new Properties();
            properties.load(new InputStreamReader(stream, StandardCharsets.UTF_8));
            // Only recorded when a release version is known at build time
            return Objects.requireNonNullElseGet(
                    properties.getProperty("libraryVersion"),
                    () -> Objects.requireNonNullElse(
                            VisibilityMetrics.class.getPackage().getImplementationVersion(), "unknown"));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static VisibilityMetrics of(TaggedMetricRegistry registry) {
        return new VisibilityMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** just a metric */
    @CheckReturnValue
    Counter test() {
        return registry.counter(testMetricName());
    }

    static MetricName testMetricName() {
        return testMetricName;
    }

    /** Tagged gauge metric. */
    @CheckReturnValue
    ComplexBuilderFooStage complex() {
        return new ComplexBuilder();
    }

    @Override
    public String toString() {
        return "VisibilityMetrics{registry=" + registry + '}';
    }

    interface ComplexBuildStage {
        void build(Gauge<? extends Number> gauge);

        @CheckReturnValue
        MetricName buildMetricName();
    }

    interface ComplexBuilderFooStage {
        @CheckReturnValue
        ComplexBuilderBarStage foo(@Safe String foo);
    }

    interface ComplexBuilderBarStage {
        @CheckReturnValue
        ComplexBuildStage bar(@Safe String bar);
    }

    private final class ComplexBuilder implements ComplexBuilderFooStage, ComplexBuilderBarStage, ComplexBuildStage {
        private String foo;

        private String bar;

        @Override
        public ComplexBuilder foo(@Safe String foo) {
            Preconditions.checkState(this.foo == null, "foo is already set");
            this.foo = Preconditions.checkNotNull(foo, "foo is required");
            return this;
        }

        @Override
        public ComplexBuilder bar(@Safe String bar) {
            Preconditions.checkState(this.bar == null, "bar is already set");
            this.bar = Preconditions.checkNotNull(bar, "bar is required");
            return this;
        }

        @Override
        public void build(Gauge<? extends Number> gauge) {
            registry.registerWithReplacement(buildMetricName(), gauge);
        }

        @Override
        public MetricName buildMetricName() {
            return MetricName.builderWithExpectedTags(5)
                    .safeName("visibility.complex")
                    .putSafeTags("bar", bar)
                    .putSafeTags("foo", foo)
                    .putSafeTags("javaVersion", JAVA_VERSION)
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .build();
        }
    }
}
