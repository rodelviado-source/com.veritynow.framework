package com.veritynow.v2.store.core;

import util.StringUtils;

public class StoreUtils {
	public static  String setOrDefault(String s, String def) {
		if (s == null || s.isBlank()) return def;
		return s;
	}
	
	public static String setRequired(String s, String argName) {
		if (StringUtils.isEmpty(s)) throw new IllegalArgumentException(argName + " must not be blank");
		return s;
	}
}
