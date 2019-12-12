package com.palantir.metric.schema.datadog;

import com.google.common.annotations.VisibleForTesting;
import com.palantir.metric.schema.MetricNamespace;
import com.palantir.metric.schema.MonitorDefinition;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DatadogMonitorGenerator {

    public static List<File> generate(GeneratorArgs args) {
        args.inputs().stream()
            .map(SchemaParser.get()::parseFile)
            .flatMap(schema -> schema.getNamespaces().entrySet().stream()
            .flatMap(namespace -> namespace.getValue().getMonitors().entrySet().stream().map(monitor -> generateMonitor(schema.getNamespaces(), monitor.getKey(), mon))))
            .collect(Collectors.toList());

        return null;
    }

    @VisibleForTesting
    static Object generateMonitor(Map<String, MetricNamespace> namespaces, String monitorName, MonitorDefinition monitor) {



        return null;
    }
}
