package io.github.hengyunabc.metrics.test;

import io.github.hengyunabc.metrics.DefaultLLDDataGenerator;
import io.github.hengyunabc.metrics.ZabbixReporter;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;

public class GetStarted {
	static final MetricRegistry metrics = new MetricRegistry();

	static public Timer timer = new Timer();

	public static void main(String args[]) throws IOException {
		startReport();

		final Histogram responseSizes = metrics
				.histogram("response-sizes--TEST");

		timer.schedule(new TimerTask() {
			int i = 100;

			@Override
			public void run() {
				responseSizes.update(i++);

			}

		}, 1000, 1000);

		Meter requests = metrics.meter("requests");
		requests.mark();
		wait5Seconds();
	}

	static void startReport() throws IOException {
		ConsoleReporter reporter = ConsoleReporter.forRegistry(metrics)
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS).build();
		metrics.register("jvm.mem", new MemoryUsageGaugeSet());
		// reporter.start(1, TimeUnit.SECONDS);

		JmxReporter jmxReporter = JmxReporter.forRegistry(metrics).build();
		jmxReporter.start();

		reporter.start(1, TimeUnit.MINUTES);

		String hostName = "172.17.42.1";
		ZabbixSender zabbixSender = new ZabbixSender("localhost", 49156);
		ZabbixReporter zabbixReporter = ZabbixReporter.forRegistry(metrics)
				.hostName(hostName).prefix("metric_")
				.lldDataGenerator(new DefaultLLDDataGenerator())
				.build(zabbixSender);

		zabbixReporter.start(1, TimeUnit.SECONDS);
	}

	static void wait5Seconds() {
		try {
			Thread.sleep(500 * 1000);
		} catch (InterruptedException e) {
		}
	}
}