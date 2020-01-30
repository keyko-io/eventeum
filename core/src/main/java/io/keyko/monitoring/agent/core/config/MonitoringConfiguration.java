package io.keyko.monitoring.agent.core.config;

import io.keyko.monitoring.agent.core.monitoring.EventeumValueMonitor;
import io.keyko.monitoring.agent.core.monitoring.MicrometerValueMonitor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class MonitoringConfiguration {

    public class PrometheusConfiguration {

        @Bean
        @ConditionalOnProperty(name = "management.endpoint.metrics.enabled", havingValue = "true")
        public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(Environment environment) {
            return registry -> registry.config().commonTags("application", "Eventeum", "environment", environment.getActiveProfiles()[0]);
        }

        @Bean
        @ConditionalOnProperty(name = "management.endpoint.metrics.enabled", havingValue = "true")
        public EventeumValueMonitor eventeumValueMonitor(MeterRegistry meterRegistry) {
            return new MicrometerValueMonitor(meterRegistry);
        }

        @Bean
        @ConditionalOnProperty(name = "management.endpoint.prometheus.enabled", havingValue = "true")
        public PrometheusMeterRegistry.Config configurePrometheus(MeterRegistry meterRegistry) {
            return meterRegistry.config().namingConvention(new CustomNamingConvention());
        }

    }

    @ConditionalOnProperty(value = "management.endpoint.metrics.enabled", havingValue = "false", matchIfMissing = true)
    public class DoNothingMonitoringConfiguration {

        @Bean
        public EventeumValueMonitor eventeumValueMonitor() {
            return new EventeumValueMonitor() {

                @Override
                public <T extends Number> T monitor(String name, String node, T number) {
                    return number;
                }
            };
        }
    }
}
