package com.yinlan.repository;

import com.yinlan.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ModelRepository extends JpaRepository<Model, Long> {
    List<Model> findByModelNameContaining(String modelName);

    @Modifying
    @Query("UPDATE Model m SET m.status = :status WHERE m.id = :id")
    int updateStatus(Long id, String status);
}