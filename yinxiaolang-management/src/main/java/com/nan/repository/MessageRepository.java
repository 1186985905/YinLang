package com.nan.repository;

import com.nan.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    
    List<Message> findBySessionIdOrderByCreatedAt(String sessionId);
    
    void deleteBySessionId(String sessionId);
}