Metric Schema
=============
_A simple opinionated toolchain to define and document metrics produced by a project._

Features:
* Source of truth for metrics, eliminating incorrect and outdated documentation.
* Produces a standardized representation of metrics to library and service consumers based on the classpath.

The generated java utilities wrap a [Tritium](https://github.com/palantir/tritium) registry to provide simple, concise
accessors to declared metrics.

Usage
-----

Metric definitions are located in project `src/main/metrics` directories.
Files are YAML formatted with the `.yml` extension and represent a collection of namespaces, a logical group of metrics 
with a shared prefix. The schema is defined using [conjure](https://palantir.github.io/conjure),
and can be [found here](metric-schema-api/src/main/conjure/metric-schema-api.yml).

Metric utilities are updated using the `generateMetrics` gradle task. If this is the first
metric definition in the module, it may be necessary to regenerate the IDE configuration
after metrics are generated, IntelliJ IDEA users can run the `idea` task.

```bash
./gradlew generateMetrics
```

Metric documentation is updated using the `generateMetricsMarkdown` gradle task or by running 
`./gradlew --write-locks`. The gradle plugin will ensure that the metrics markdown is always up to date.

All metric definitions will be embedded within the output JAR as a resource located `metric-schema/metrics.json`.

Example
-------

```yaml
# Prefix applied to all metrics
namespaces:
  my.service:
    # Documentation describing the entire namespace.
    docs: Metrics helpful for monitoring a my-service instance.
    metrics:
      # Results in a meter with name `my.service.failures` tagged with `{operationType: <value>}`
      failures:
        type: meter
        tags:
          - operationType
        docs: Rate of failures by `operationType`
      # results in a histogram with name `my.service.result.batch.size` and no tags.
      result.batch.size:
        type: histogram
        # Provide additional context beyond the metric name
        docs: Batch size (based on the number of elements) of results written to s3.
```

The example schema produces the following utility class for java consumers:

```java
/** Metrics helpful for monitoring a my-service instance. */
public final class MyServiceMetrics {
    private final TaggedMetricRegistry registry;

    private MyServiceMetrics(TaggedMetricRegistry registry) {
        this.registry = registry;
    }

    public static MyServiceMetrics of(TaggedMetricRegistry registry) {
        return new MyServiceMetrics(Preconditions.checkNotNull(registry, "TaggedMetricRegistry"));
    }

    /** Rate of failures by <code>operationType</code> */
    public Meter failures(String operationType) {
        return registry.meter(
                MetricName.builder()
                        .safeName("my.service.failures")
                        .putSafeTags("operationType", operationType)
                        .build());
    }

    /** Batch size (based on the number of elements) of results written to s3. */
    public Histogram resultBatchSize() {
        return registry.histogram(
                MetricName.builder().safeName("my.service.result.batch.size").build());
    }

    @Override
    public String toString() {
        return "MyServiceMetrics{registry=" + registry + '}';
    }
}
```

The utility should be created once and reused.
```java
MyServiceMetrics metrics = MyServiceMetrics.of(server.taggedMetrics());
// Metric objects should be reused as much as possible to avoid unnecessary lookups
Meter creationFailures = metrics.failures("create");
```

### Options
Metric definitions can also include options that do not change the overall declaration, but may affect the way it is 
handled in a particular context.
```yml
options:
  javaPackage: 'com.palantir.my.package' # Specifies under which package Java classes should be generated
  javaVisibility: packagePrivate # Specifies visibility of generated Utility class. Defaults to public
namespaces:
...
```
Installation
------------

Add the `gradle-metric-schema` gradle plugin dependency to the root `build.gradle`.
```groovy
buildscript {
  dependencies {
    classpath 'com.palantir.metricschema:gradle-metric-schema:<VERSION>'   
  }
}
```

Apply the `com.palantir.metric-schema` plugin to `build.gradle` in the module which defines metrics.
```groovy
apply plugin: 'com.palantir.metric-schema'
```

Architecture
------------
Design documentation describing the rationale for decisions is maintained in [design.md](docs/design.md).

Gradle Tasks
------------
`./gradlew tasks` - to get the list of gradle tasks


Start Developing
----------------
Run one of the following commands:

* `./gradlew idea` for IntelliJ
* `./gradlew eclipse` for Eclipse
