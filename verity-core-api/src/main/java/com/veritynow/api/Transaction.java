package com.veritynow.api;

public record Transaction (
	String path,
	String blobRef,
	String blobMimetype,
	String operation
){
}
