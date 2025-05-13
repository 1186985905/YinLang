package com.nan.repository;

import com.nan.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, String> {
    
    List<ChatSession> findByUserIdOrderByUpdatedAtDesc(Long userId);
    
    void deleteByUserIdAndId(Long userId, String id);
}