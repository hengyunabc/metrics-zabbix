package io.github.hengyunabc.metrics;

import java.util.Iterator;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

public class DefaultLLDDataGenerator implements LLDDataGenerator {

	static final String DefaultMacroName = "{#METRICS_KEY}";
	static final String DefaultDiscoveryRuleKey = "metrics_discovery_rule_key";

	String macroName = DefaultMacroName;
	String discoveryRuleKey = DefaultDiscoveryRuleKey;

	@Override
	public String generateLLDDataString(String host, Set<String> keys) {
		JSONObject result = new JSONObject();
		JSONArray data = new JSONArray();

		Iterator<String> iterator = keys.iterator();
		while (iterator.hasNext()) {
			String key = iterator.next();
			JSONObject macro = new JSONObject();
			macro.put(macroName, key);
			data.add(macro);
		}

		result.put("data", data);
		return result.toJSONString();
	}

	public String getMacroName() {
		return macroName;
	}

	public void setMacroName(String macroName) {
		this.macroName = macroName;
	}

	public void setDiscoveryRuleKey(String discoveryRuleKey) {
		this.discoveryRuleKey = discoveryRuleKey;
	}

	@Override
	public String getDiscoveryRuleKey() {
		return discoveryRuleKey;
	}

}
