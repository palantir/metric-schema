package com.palantir.metric.schema.datadog;

import java.nio.file.Path;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable
public abstract class GeneratorArgs {

    /**
     * Input directory for metric schema files with the ".yml" extension.
     */
    abstract Set<Path> inputs();

    /** Output directory for generated java code. */
    abstract Path output();

    /** The default Java package name for generated classes. */
    abstract String defaultPackageName();

    public static final class Builder extends ImmutableGeneratorArgs.Builder {}

    public static Builder builder() {
        return new Builder();
    }
}
