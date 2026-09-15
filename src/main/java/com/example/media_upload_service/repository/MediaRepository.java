package com.example.media_upload_service.repository;

import com.example.media_upload_service.entity.Media;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaRepository extends JpaRepository<Media, Long> {
}