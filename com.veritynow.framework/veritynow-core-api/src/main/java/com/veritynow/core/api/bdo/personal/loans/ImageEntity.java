package com.veritynow.core.api.bdo.personal.loans;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "images")
public class ImageEntity {

  @Id
  @Column(length = 128, nullable = false, updatable = false)
  private String id;

  @Column(length = 64)
  private String contentType;

  @Lob
  @Basic(fetch = FetchType.LAZY)
  @Column(name = "data", nullable = false)
  private byte[] data;

  @Column(nullable = false)
  private Instant createdAt = Instant.now();

  // getters/setters

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }

  public String getContentType() { return contentType; }
  public void setContentType(String contentType) { this.contentType = contentType; }

  public byte[] getData() { return data; }
  public void setData(byte[] data) { this.data = data; }

  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
