package com.veritynow.rest.api;

public record Transaction (
	String path,
	String blobRef,
	String blobMimetype,
	String operation
){
}
