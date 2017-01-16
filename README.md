# metrics-zabbix
Dropwizard Metrics reporter for zabbix. Support zabbix 3.0.

https://github.com/dropwizard/metrics

## something

* This just use zabbix sender to sender data to zabbix. You have to config item and graph in zabbix server by youself.
It will not create item auto.

* If you want to create item in zabbix auto, try to use zabbix `Low-level discovery`.

https://www.zabbix.com/documentation/2.0/manual/discovery/low_level_discovery

You can use macro in your metric key, such as `{#FSNAME}`.

##example
```java
import io.github.hengyunabc.metrics.ZabbixReporter;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

public class GetStarted {
	static final MetricRegistry metrics = new MetricRegistry();

	public static void main(String args[]) throws IOException,
			InterruptedException {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		metrics.register("jvm.mem", new MemoryUsageGaugeSet());
		metrics.register("jvm.gc", new GarbageCollectorMetricSet());
		reporter.start(5, TimeUnit.SECONDS);

		String hostName = "192.168.66.29";
		ZabbixSender zabbixSender = new ZabbixSender("192.168.90.102", 10051);
		ZabbixReporter zabbixReporter = ZabbixReporter.forRegistry(metrics)
				.hostName(hostName).prefix("test.").build(zabbixSender);

		zabbixReporter.start(1, TimeUnit.SECONDS);

		TimeUnit.SECONDS.sleep(500);
	}
}
```

## Maven dependency

```xml
<dependency>
    <groupId>io.github.hengyunabc</groupId>
    <artifactId>metrics-zabbix</artifactId>
    <version>0.0.4</version>
</dependency>
```

## Others

https://github.com/hengyunabc/zabbix-api

https://github.com/hengyunabc/zabbix-sender

https://github.com/hengyunabc/metrics-kafka

https://github.com/hengyunabc/kafka-zabbix

##License

Apache License V2
