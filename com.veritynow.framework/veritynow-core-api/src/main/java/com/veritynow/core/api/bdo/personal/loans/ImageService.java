package com.veritynow.core.api.bdo.personal.loans;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class ImageService {

  private final ImageRepository repo;

  public ImageService(ImageRepository repo) {
    this.repo = repo;
  }

  @Transactional
  public ImageEntity save(ImageEntity e) {
    return repo.save(e);
  }

  @Transactional(readOnly = true)
  public Optional<ImageEntity> find(String id) {
    return repo.findById(id);
  }
}
