package com.nan.controller;

import com.nan.entity.Department;
import com.nan.entity.ApiResult;
import com.nan.repository.DepartmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    @Autowired
    private DepartmentRepository departmentRepository;

    // 获取所有部门
    @GetMapping
    public ApiResult getAllDepartments() {
        List<Department> departments = departmentRepository.findAll();
        return ApiResult.success(departments);
    }

    // 根据ID获取部门
    @GetMapping("/{id}")
    public ApiResult getDepartmentById(@PathVariable Long id) {
        Optional<Department> department = departmentRepository.findById(id);
        return department.isPresent()
                ? ApiResult.success(department.get())
                : ApiResult.fail("部门不存在");
    }

    // 创建部门
    @PostMapping
    public ApiResult createDepartment(@RequestBody Map<String, String> payload) {
        String name = payload.get("name");
        String description = payload.get("description");

        if (name == null || name.trim().isEmpty()) {
            return ApiResult.fail("部门名称不能为空");
        }

        // 检查部门名称是否已存在
        Department existingDepartment = departmentRepository.findByName(name);
        if (existingDepartment != null) {
            return ApiResult.fail("部门名称已存在");
        }

        Department department = new Department();
        department.setName(name);
        department.setDescription(description);

        Department savedDepartment = departmentRepository.save(department);
        return ApiResult.success(savedDepartment);
    }

    // 更新部门
    @PutMapping("/{id}")
    public ApiResult updateDepartment(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<Department> optionalDepartment = departmentRepository.findById(id);
        if (!optionalDepartment.isPresent()) {
            return ApiResult.fail("部门不存在");
        }

        String name = payload.get("name");
        String description = payload.get("description");

        if (name == null || name.trim().isEmpty()) {
            return ApiResult.fail("部门名称不能为空");
        }

        // 检查部门名称是否已被其他部门使用
        Department existingDepartment = departmentRepository.findByName(name);
        if (existingDepartment != null && !existingDepartment.getId().equals(id)) {
            return ApiResult.fail("部门名称已存在");
        }

        Department department = optionalDepartment.get();
        department.setName(name);
        department.setDescription(description);

        Department updatedDepartment = departmentRepository.save(department);
        return ApiResult.success(updatedDepartment);
    }

    // 删除部门
    @DeleteMapping("/{id}")
    public ApiResult deleteDepartment(@PathVariable Long id) {
        Optional<Department> department = departmentRepository.findById(id);
        if (!department.isPresent()) {
            return ApiResult.fail("部门不存在");
        }

        try {
            departmentRepository.deleteById(id);
            return ApiResult.success("部门删除成功");
        } catch (Exception e) {
            return ApiResult.fail("部门删除失败，可能存在关联用户");
        }
    }
} 