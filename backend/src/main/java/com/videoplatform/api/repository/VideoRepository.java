package com.videoplatform.api.repository;

import com.videoplatform.api.entity.Video;
import com.videoplatform.api.entity.VideoStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface VideoRepository extends JpaRepository<Video, UUID> {

    Page<Video> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Video> findByStatus(VideoStatus status);

    long countByStatus(VideoStatus status);
}
