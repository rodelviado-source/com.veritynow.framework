package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.stereotype.Service;

@Service
public class ImageService {
	private final ImageRepository repo;

	public ImageService(ImageRepository repo) {
		this.repo = repo;
	}

	public ImageEntity find(String id) {
		return repo.findById(id).orElse(null);
	}

	public ImageEntity save(ImageEntity e) {
		return repo.save(e);
	}
}