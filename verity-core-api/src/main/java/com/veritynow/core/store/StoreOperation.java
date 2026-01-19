package com.veritynow.core.store;

public enum StoreOperation {
	Created, Read, Updated,Deleted,Undeleted, Restored;
	
	public static String Created() {
		return Created.toString();
	}
	
	public static String Read() {
		return Read.toString();
	}
	
	public  static  String Updated() {
		return Updated.toString();
	}
	
	public  static String Deleted() {
		return Deleted.toString();
	}
	
	public  static String Undeleted() {
		return Undeleted.toString();
	}
	
	public  static String Restored() {
		return Restored.toString();
	}
	
	
	
}
