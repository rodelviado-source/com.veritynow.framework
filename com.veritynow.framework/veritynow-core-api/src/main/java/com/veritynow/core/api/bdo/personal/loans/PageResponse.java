package com.veritynow.core.api.bdo.personal.loans;

import java.util.List;

public class PageResponse<T> {
	private java.util.List<T> items;
	private int page;
	private int size;
	private long total;

	public PageResponse() {
	}

	public PageResponse(java.util.List<T> items, int page, int size, long total) {
		this.items = items;
		this.page = page;
		this.size = size;
		this.total = total;
	}

	public java.util.List<T> getItems() {
		return items;
	}

	public void setItems(java.util.List<T> items) {
		this.items = items;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public long getTotal() {
		return total;
	}

	public void setTotal(long total) {
		this.total = total;
	}
}