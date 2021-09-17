package io.github.hengyunabc.metrics;

import io.github.hengyunabc.zabbix.sender.DataObject;
import io.github.hengyunabc.zabbix.sender.SenderResult;
import io.github.hengyunabc.zabbix.sender.ZabbixSender;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Metered;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Snapshot;
import com.codahale.metrics.Timer;

/**
 * 
 * @author hengyunabc
 *
 */
public class ZabbixReporter extends ScheduledReporter {
	private static final Logger logger = LoggerFactory.getLogger(ZabbixReporter.class);

	private final ZabbixSender zabbixSender;
	private final String hostName;
	private final String prefix;
	private final String suffix;

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
		private String suffix = "";

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

		public Builder suffix(String suffix) {
			this.suffix = suffix;
			return this;
		}

		/**
		 * Builds a {@link ZabbixReporter} with the given properties.
		 *
		 * @return a {@link ZabbixReporter}
		 */
		public ZabbixReporter build(ZabbixSender zabbixSender) {
			if (hostName == null) {
				hostName = HostUtil.getHostName();
				logger.info(name + " detect hostName: " + hostName);
			}
			return new ZabbixReporter(registry, name, rateUnit, durationUnit, filter, zabbixSender, hostName, prefix, suffix);
		}
	}

	private ZabbixReporter(MetricRegistry registry, String name, TimeUnit rateUnit, TimeUnit durationUnit,
			MetricFilter filter, ZabbixSender zabbixSender, String hostName, String prefix, String suffix) {
		super(registry, name, filter, rateUnit, durationUnit);
		this.zabbixSender = zabbixSender;
		this.hostName = hostName;
		this.prefix = prefix;
		this.suffix = suffix;
	}

	private DataObject toDataObject(String key, String keySuffix, Object value, long clock) {
		return DataObject.builder().host(hostName).key(prefix + key + keySuffix + suffix).clock(clock).value(String.valueOf(value)).build();
	}

	/**
	 * for histograms.
	 * 
	 * @param key
	 * @param snapshot
	 * @param dataObjectList
	 */
	private void addSnapshotDataObject(String key, Snapshot snapshot, long clock, List<DataObject> dataObjectList) {
		dataObjectList.add(toDataObject(key, ".min", snapshot.getMin(), clock));
		dataObjectList.add(toDataObject(key, ".max", snapshot.getMax(), clock));
		dataObjectList.add(toDataObject(key, ".mean", snapshot.getMean(), clock));
		dataObjectList.add(toDataObject(key, ".stddev", snapshot.getStdDev(), clock));
		dataObjectList.add(toDataObject(key, ".median", snapshot.getMedian(), clock));
		dataObjectList.add(toDataObject(key, ".75th", snapshot.get75thPercentile(), clock));
		dataObjectList.add(toDataObject(key, ".95th", snapshot.get95thPercentile(), clock));
		dataObjectList.add(toDataObject(key, ".98th", snapshot.get98thPercentile(), clock));
		dataObjectList.add(toDataObject(key, ".99th", snapshot.get99thPercentile(), clock));
		dataObjectList.add(toDataObject(key, ".99.9th", snapshot.get999thPercentile(), clock));
	}

	/**
	 * for timer.
	 * 
	 * @param key
	 * @param snapshot
	 * @param dataObjectList
	 */
	private void addSnapshotDataObjectWithConvertDuration(String key, Snapshot snapshot, long clock,
			List<DataObject> dataObjectList) {
		dataObjectList.add(toDataObject(key, ".min", convertDuration(snapshot.getMin()), clock));
		dataObjectList.add(toDataObject(key, ".max", convertDuration(snapshot.getMax()), clock));
		dataObjectList.add(toDataObject(key, ".mean", convertDuration(snapshot.getMean()), clock));
		dataObjectList.add(toDataObject(key, ".stddev", convertDuration(snapshot.getStdDev()), clock));
		dataObjectList.add(toDataObject(key, ".median", convertDuration(snapshot.getMedian()), clock));
		dataObjectList.add(toDataObject(key, ".75th", convertDuration(snapshot.get75thPercentile()), clock));
		dataObjectList.add(toDataObject(key, ".95th", convertDuration(snapshot.get95thPercentile()), clock));
		dataObjectList.add(toDataObject(key, ".98th", convertDuration(snapshot.get98thPercentile()), clock));
		dataObjectList.add(toDataObject(key, ".99th", convertDuration(snapshot.get99thPercentile()), clock));
		dataObjectList.add(toDataObject(key, ".99.9th", convertDuration(snapshot.get999thPercentile()), clock));
	}

	private void addMeterDataObject(String key, Metered meter, long clock, List<DataObject> dataObjectList) {
		dataObjectList.add(toDataObject(key, ".count", meter.getCount(), clock));
		dataObjectList.add(toDataObject(key, ".meanRate", convertRate(meter.getMeanRate()), clock));
		dataObjectList.add(toDataObject(key, ".1-minuteRate", convertRate(meter.getOneMinuteRate()), clock));
		dataObjectList.add(toDataObject(key, ".5-minuteRate", convertRate(meter.getFiveMinuteRate()), clock));
		dataObjectList.add(toDataObject(key, ".15-minuteRate", convertRate(meter.getFifteenMinuteRate()), clock));
	}

	@SuppressWarnings("rawtypes")
	@Override
	public void report(SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
			SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers) {
		final long clock = System.currentTimeMillis() / 1000;
		List<DataObject> dataObjectList = new LinkedList<DataObject>();
		for (Map.Entry<String, Gauge> entry : gauges.entrySet()) {
			DataObject dataObject = toDataObject(entry.getKey(), "", String.valueOf(entry.getValue().getValue()), clock);
			dataObjectList.add(dataObject);
		}

		for (Map.Entry<String, Counter> entry : counters.entrySet()) {
			DataObject dataObject = toDataObject(entry.getKey(), "", String.valueOf(entry.getValue().getCount()), clock);
			dataObjectList.add(dataObject);
		}

		for (Map.Entry<String, Histogram> entry : histograms.entrySet()) {
			Histogram histogram = entry.getValue();
			Snapshot snapshot = histogram.getSnapshot();
			addSnapshotDataObject(entry.getKey(), snapshot, clock, dataObjectList);
		}

		for (Map.Entry<String, Meter> entry : meters.entrySet()) {
			Meter meter = entry.getValue();
			addMeterDataObject(entry.getKey(), meter, clock, dataObjectList);
		}

		for (Map.Entry<String, Timer> entry : timers.entrySet()) {
			Timer timer = entry.getValue();
			addMeterDataObject(entry.getKey(), timer, clock, dataObjectList);
			addSnapshotDataObjectWithConvertDuration(entry.getKey(), timer.getSnapshot(), clock, dataObjectList);
		}

		try {
			SenderResult senderResult = zabbixSender.send(dataObjectList, clock);
			if (!senderResult.success()) {
				logger.warn("report metrics to zabbix not success!" + senderResult);
			} else if (logger.isDebugEnabled()) {
				logger.info("report metrics to zabbix success. " + senderResult);
			}
		} catch (IOException e) {
			logger.error("report metrics to zabbix error!");
		}
	}

}
