package com.veritynow.core.api.bdo.personal.loans;

import jakarta.persistence.*;

@Entity
@Table(name = "images")
public class ImageEntity {
	@Id
	@Column(length = 128)
	private String id;
	@Lob
	@Basic(fetch = FetchType.LAZY)
	private byte[] data;
	@Column(length = 64)
	private String contentType;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public byte[] getData() {
		return data;
	}

	public void setData(byte[] data) {
		this.data = data;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}
}