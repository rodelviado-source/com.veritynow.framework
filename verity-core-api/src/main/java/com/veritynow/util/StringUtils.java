package com.veritynow.util;

public class StringUtils {
	public static boolean isEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	public static boolean isNotEmpty(String s) {
		return s != null && !s.trim().isEmpty();
	}
}
