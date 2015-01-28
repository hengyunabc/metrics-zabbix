package io.github.hengyunabc.metrics;

import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.SenderResult;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;

public class ZabbixReporter extends ScheduledReporter {
	private static final Logger logger = LoggerFactory
			.getLogger(ZabbixReporter.class);

	private ZabbixSender zabbixSender;
	private String hostName;
	private String prefix;

	private LLDDataGenerator lldDataGenerator;
	private Set<String> oldKeys = Collections.emptySet();

	public static Builder forRegistry(MetricRegistry registry) {
		return new Builder(registry);
	}

	public static class Builder {
		private final MetricRegistry registry;
		private String name = "zabbix-reporter";
		private TimeUnit rateUnit;
		private TimeUnit durationUnit;
		private MetricFilter filter;

		private String hostName;
		private String prefix = "";

		private LLDDataGenerator lldDataGenerator;

		public Builder(MetricRegistry registry) {
			this.registry = registry;

			this.rateUnit = TimeUnit.SECONDS;
			this.durationUnit = TimeUnit.MILLISECONDS;
			this.filter = MetricFilter.ALL;

		}

		/**
		 * Convert rates to the given time unit.
		 *
		 * @param rateUnit
		 *            a unit of time
		 * @return {@code this}
		 */
		public Builder convertRatesTo(TimeUnit rateUnit) {
			this.rateUnit = rateUnit;
			return this;
		}

		/**
		 * Convert durations to the given time unit.
		 *
		 * @param durationUnit
		 *            a unit of time
		 * @return {@code this}
		 */
		public Builder convertDurationsTo(TimeUnit durationUnit) {
			this.durationUnit = durationUnit;
			return this;
		}

		/**
		 * Only report metrics which match the given filter.
		 *
		 * @param filter
		 *            a {@link MetricFilter}
		 * @return {@code this}
		 */
		public Builder filter(MetricFilter filter) {
			this.filter = filter;
			return this;
		}

		/**
		 * default register name is "zabbix-reporter".
		 * 
		 * @param name
		 * @return
		 */
		public Builder name(String name) {
			this.name = name;
			return this;
		}

		public Builder hostName(String hostName) {
			this.hostName = hostName;
			return this;
		}

		public Builder prefix(String prefix) {
			this.prefix = prefix;
			return this;
		}

		public Builder lldDataGenerator(LLDDataGenerator lldDataGenerator) {
			this.lldDataGenerator = lldDataGenerator;
			return this;
		}

		/**
		 * Builds a {@link ZabbixReporter} with the given properties.
		 *
		 * @return a {@link ZabbixReporter}
		 */
		public ZabbixReporter build(ZabbixSender zabbixSender) {
			return new ZabbixReporter(registry, name, rateUnit, durationUnit,
					filter, zabbixSender, hostName, prefix, lldDataGenerator);
		}
	}

	private ZabbixReporter(MetricRegistry registry, String name,
			TimeUnit rateUnit, TimeUnit durationUnit, MetricFilter filter,
			ZabbixSender zabbixSender, String hostName, String prefix,
			LLDDataGenerator lldDataGenerator) {
		super(registry, name, filter, rateUnit, durationUnit);
		this.zabbixSender = zabbixSender;
		this.hostName = hostName;
		this.prefix = prefix;
		this.lldDataGenerator = lldDataGenerator;
	}

//	protected ZabbixReporter(MetricRegistry registry, String name,
//			MetricFilter filter, TimeUnit rateUnit, TimeUnit durationUnit) {
//		super(registry, name, filter, rateUnit, durationUnit);
//		// TODO Auto-generated constructor stub
//	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges,
			SortedMap<String, Counter> counters,
			SortedMap<String, Histogram> histograms,
			SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		List<DataObject> dataObjectList = new LinkedList<DataObject>();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			DataObject dataObject = DataObject.builder().host(hostName)
					.key(prefix + entry.getKey())
					.value(entry.getValue().getValue().toString()).build();
			dataObjectList.add(dataObject);
		}

		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			DataObject dataObject = DataObject.builder().host(hostName)
					.key(prefix + entry.getKey())
					.value("" + entry.getValue().getCount()).build();
			dataObjectList.add(dataObject);
		}

		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			DataObject dataObject = DataObject.builder().host(hostName)
					.key(prefix + entry.getKey())
					.value("" + entry.getValue().getCount()).build();
			dataObjectList.add(dataObject);
		}

		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			DataObject dataObject = DataObject.builder().host(hostName)
					.key(prefix + entry.getKey())
					.value("" + entry.getValue().getCount()).build();
			dataObjectList.add(dataObject);
		}

		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			DataObject dataObject = DataObject.builder().host(hostName)
					.key(prefix + entry.getKey())
					.value("" + entry.getValue().getCount()).build();
			dataObjectList.add(dataObject);
		}

		if (lldDataGenerator != null) {
			// check if has new key
			HashSet<String> newKeys = new HashSet<String>();
			for (DataObject object : dataObjectList) {
				newKeys.add(object.getKey());
			}
			if (!oldKeys.containsAll(newKeys)) {
				String dataString = lldDataGenerator.generateLLDDataString(
						hostName, newKeys);
				try {
					SenderResult senderResult = zabbixSender.send(DataObject
							.builder().host(hostName)
							.key(lldDataGenerator.getDiscoveryRuleKey())
							.value(dataString).build());
					if(!senderResult.success()){
						logger.warn("send DDL info to zabbix server not success!" + senderResult);
					}
					oldKeys = newKeys;
				} catch (IOException e) {
					logger.error("send DDL info to zabbix server error!", e);
				}
			}
		}

		try {
			SenderResult senderResult = zabbixSender.send(dataObjectList);
			if (!senderResult.success()) {
				logger.warn("report metrics to zabbix not success!" + senderResult);
			} else if (logger.isDebugEnabled()) {
				logger.info("report metrics to zabbix success. " + senderResult);
			}
		} catch (IOException e) {
			logger.error("report metris to zabbix error!");
		}

		for (DataObject object : dataObjectList) {
			System.out.println(object);
		}
	}

}
