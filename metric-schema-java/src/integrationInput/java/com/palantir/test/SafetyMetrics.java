package com.palantir.test;

import com.codahale.metrics.Counter;
import com.google.common.hash.Hashing;
import com.google.errorprone.annotations.CheckReturnValue;
import com.palantir.logsafe.Preconditions;
import com.palantir.tritium.metrics.registry.MetricName;
import com.palantir.tritium.metrics.registry.TaggedMetricRegistry;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Test for tag safety
 */
public final class SafetyMetrics {
    private static final String LIBRARY_NAME = "witchcraft";

    private static final String LIBRARY_VERSION = Optional.ofNullable(
                    SafetyMetrics.class.getPackage().getImplementationVersion())
            .orElse("unknown");

    private final TaggedMetricRegistry registry;

    private SafetyMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static SafetyMetrics of(TaggedMetricRegistry registry) {
        return new SafetyMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /**
     * A metric with safe and unsafe tags
     */
    @CheckReturnValue
    public TestBuilderSafeStage test() {
        return new TestBuilder();
    }

    @Override
    public String toString() {
        return "SafetyMetrics{registry=" + registry + '}';
    }

    public enum Test_SafeMultiple {
        VALUE1("value1"),

        VALUE2("value2");

        private final String value;

        Test_SafeMultiple(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }

    public enum Test_UnsafeMultiple {
        VALUE1("value1"),

        VALUE2("value2");

        private final String value;

        Test_UnsafeMultiple(String value) {
            this.value = value;
        }

        private String getValue() {
            return value;
        }
    }

    public interface TestBuildStage {
        @CheckReturnValue
        Counter build();
    }

    public interface TestBuilderSafeStage {
        @CheckReturnValue
        TestBuilderUnsafeStage safe(String safe);
    }

    public interface TestBuilderUnsafeStage {
        @CheckReturnValue
        TestBuilderSafeMultipleStage unsafe(String unsafe);
    }

    public interface TestBuilderSafeMultipleStage {
        @CheckReturnValue
        TestBuilderUnsafeMultipleStage safeMultiple(Test_SafeMultiple safeMultiple);
    }

    public interface TestBuilderUnsafeMultipleStage {
        @CheckReturnValue
        TestBuildStage unsafeMultiple(Test_UnsafeMultiple unsafeMultiple);
    }

    private final class TestBuilder
            implements TestBuilderSafeStage,
                    TestBuilderUnsafeStage,
                    TestBuilderSafeMultipleStage,
                    TestBuilderUnsafeMultipleStage,
                    TestBuildStage {
        private String safe;

        private String unsafe;

        private Test_SafeMultiple safeMultiple;

        private Test_UnsafeMultiple unsafeMultiple;

        @Override
        public Counter build() {
            return registry.counter(MetricName.builder()
                    .safeName("safety.test")
                    .putSafeTags("safe", safe)
                    .putSafeTags(
                            "unsafe",
                            Hashing.murmur3_32_fixed()
                                    .hashString(unsafe, StandardCharsets.UTF_8)
                                    .toString())
                    .putSafeTags("safe-single", "value")
                    .putSafeTags("unsafe-single", "value")
                    .putSafeTags("safe-multiple", safeMultiple.getValue())
                    .putSafeTags("unsafe-multiple", unsafeMultiple.getValue())
                    .putSafeTags("libraryName", LIBRARY_NAME)
                    .putSafeTags("libraryVersion", LIBRARY_VERSION)
                    .build());
        }

        @Override
        public TestBuilder safe(String safe) {
            Preconditions.checkState(this.safe == null, "safe is already set");
            this.safe = Preconditions.checkNotNull(safe, "safe is required");
            return this;
        }

        @Override
        public TestBuilder unsafe(String unsafe) {
            Preconditions.checkState(this.unsafe == null, "unsafe is already set");
            this.unsafe = Preconditions.checkNotNull(unsafe, "unsafe is required");
            return this;
        }

        @Override
        public TestBuilder safeMultiple(Test_SafeMultiple safeMultiple) {
            Preconditions.checkState(this.safeMultiple == null, "safe-multiple is already set");
            this.safeMultiple = Preconditions.checkNotNull(safeMultiple, "safe-multiple is required");
            return this;
        }

        @Override
        public TestBuilder unsafeMultiple(Test_UnsafeMultiple unsafeMultiple) {
            Preconditions.checkState(this.unsafeMultiple == null, "unsafe-multiple is already set");
            this.unsafeMultiple = Preconditions.checkNotNull(unsafeMultiple, "unsafe-multiple is required");
            return this;
        }
    }
}
