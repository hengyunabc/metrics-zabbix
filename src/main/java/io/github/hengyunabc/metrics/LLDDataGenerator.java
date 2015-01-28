package io.github.hengyunabc.metrics;

import java.util.Set;

/**
 * zabbix can set
 * 
 * https://www.zabbix.com/documentation/2.2/manual/discovery/low_level_discovery
 * 
 * @author hengyunabc
 *
 */
public interface LLDDataGenerator {
	
	/**
	 * this key should config in zabbix.
	 * @return
	 */
	public String getDiscoveryRuleKey();

	/**
	 * must return a json string. like this:
	 * {@code
	 * 
	 * { "data": [ { "{#FSNAME}": "/", "{#FSTYPE}": "rootfs" }, { "{#FSNAME}":
	 * "/sys", "{#FSTYPE}": "sysfs" } ] } }
	 * 
	 * @return
	 */
	public String generateLLDDataString(String host, Set<String> keys);
}
