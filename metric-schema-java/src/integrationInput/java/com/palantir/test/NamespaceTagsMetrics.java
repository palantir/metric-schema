package com.palantir.test;

import com.codahale.metrics.Meter;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.logsafe.Safe;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * General web server metrics.
 */
public final class NamespaceTagsMetrics {
    private static final String JAVA_VERSION = System.getProperty("java.version", "unknown");

    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION =
            Objects.requireNonNullElse(NamespaceTagsMetrics.class.getPackage().getImplementationVersion(), "unknown");

    private final TaggedMetricRegistry registry;

    private final Map<String, String> tags;

    private NamespaceTagsMetrics(TaggedMetricRegistry registry, Map<String, String> tags) {
        this.registry = registry;
        this.tags = tags;
    }

    public static NamespaceTagsMetrics of(TaggedMetricRegistry registry, String locator) {
        Map<String, String> tags = new HashMap<>();
        tags.put("locator", locator);
        return new NamespaceTagsMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"), tags);
    }

    /**
     * Measures number of installations that were processed
     */
    @CheckReturnValue
    public ProcessingBuilderResultStage processing() {
        return new ProcessingBuilder();
    }

    /**
     * Measures more
     */
    @CheckReturnValue
    public Meter more() {
        return registry.meter(MetricName.builder()
                .safeName("namespace-tags.more")
                .putSafeTags("libraryName", LIBRARY_NAME)
                .putSafeTags("libraryVersion", LIBRARY_VERSION)
                .putSafeTags("javaVersion", JAVA_VERSION)
                .build());
    }

    @Override
    public String toString() {
        return "NamespaceTagsMetrics{registry=" + registry + '}';
    }

    public enum Processing_Result {
        SUCCESS("success"),

        FAILURE("failure");

        private final String value;

        Processing_Result(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }

    public enum Processing_OtherLocator {
        PACKAGE_IDENTIFIER("package:identifier"),

        PACKAGE_IDENTIFIER2("package:identifier2");

        private final String value;

        Processing_OtherLocator(String value) {
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
        /**
         * The result of processing
         */
        @CheckReturnValue
        ProcessingBuilderTypeStage result(@Safe Processing_Result result);
    }

    public interface ProcessingBuilderTypeStage {
        @CheckReturnValue
        ProcessingBuilderOtherLocatorStage type(@Safe String type);
    }

    public interface ProcessingBuilderOtherLocatorStage {
        @CheckReturnValue
        ProcessingBuildStage otherLocator(@Safe Processing_OtherLocator otherLocator);
    }

    private final class ProcessingBuilder
            implements ProcessingBuilderResultStage,
                    ProcessingBuilderTypeStage,
                    ProcessingBuilderOtherLocatorStage,
                    ProcessingBuildStage {
        private Processing_Result result;

        private String type;

        private Processing_OtherLocator otherLocator;

        @Override
        public Meter build() {
            return registry.meter(MetricName.builder()
                    .safeName("namespace-tags.processing")
                    .putSafeTags("result", result.getValue())
                    .putSafeTags("type", type)
                    .putSafeTags("otherLocator", otherLocator.getValue())
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .putSafeTags("javaVersion", JAVA_VERSION)
                    .build());
        }

        @Override
        public ProcessingBuilder result(@Safe Processing_Result result) {
            Preconditions.checkState(this.result == null, "result is already set");
            this.result = Preconditions.checkNotNull(result, "result is required");
            return this;
        }

        @Override
        public ProcessingBuilder type(@Safe String type) {
            Preconditions.checkState(this.type == null, "type is already set");
            this.type = Preconditions.checkNotNull(type, "type is required");
            return this;
        }

        @Override
        public ProcessingBuilder otherLocator(@Safe Processing_OtherLocator otherLocator) {
            Preconditions.checkState(this.otherLocator == null, "otherLocator is already set");
            this.otherLocator = Preconditions.checkNotNull(otherLocator, "otherLocator is required");
            return this;
        }
    }
}
