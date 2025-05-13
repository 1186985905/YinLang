package com.nan.repository;

import com.nan.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {
    // 根据名称查找部门
    Department findByName(String name);
} 