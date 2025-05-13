package com.nan.controller;

import com.nan.entity.ApiResult;
import com.nan.entity.Department;
import com.nan.entity.User;
import com.nan.entity.dto.LoginRequest;
import com.nan.entity.dto.LoginResponse;
import com.nan.repository.DepartmentRepository;
import com.nan.service.UserService;
import com.nan.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final DepartmentRepository departmentRepository;
    
    @PostMapping("/login")
    public ApiResult<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            // 使用Spring Security进行认证
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
            
            // 认证成功，将认证信息存入Security上下文
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            // 生成JWT令牌
            String token = jwtUtil.generateToken(request.getUsername());
            
            // 获取用户信息并更新最后登录时间
            User user = userService.findByUsername(request.getUsername()).orElseThrow();
            user.setLastLoginTime(LocalDateTime.now());
            userService.updateUser(user);
            
            // 构建响应对象（不包含密码）
            user.setPassword(null);
            LoginResponse response = LoginResponse.builder()
                    .token(token)
                    .user(user)
                    .build();
            
            return ApiResult.success("登录成功", response);
        } catch (BadCredentialsException e) {
            log.warn("用户验证失败: {}", request.getUsername());
            return ApiResult.error(HttpStatus.UNAUTHORIZED.value(), "用户名或密码错误");
        } catch (Exception e) {
            log.error("登录失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "登录处理失败: " + e.getMessage());
        }
    }
    
    @PostMapping("/register")
    public ApiResult<User> register(@RequestBody User user) {
        try {
            // 检查用户是否存在
            if (userService.checkIfUserExistsByUsername(user.getUsername())) {
                return ApiResult.error(HttpStatus.BAD_REQUEST.value(), "用户名已存在");
            }
            
            // 设置默认值
            user.setRole("user");
            user.setStatus(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // 创建用户
            User createdUser = userService.createUser(user);
            
            // 返回创建的用户(不包括密码)
            createdUser.setPassword(null);
            return ApiResult.success("注册成功", createdUser);
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "注册失败: " + e.getMessage());
        }
    }
    
    @GetMapping("/profile")
    public ApiResult<User> getUserProfile() {
        try {
            // 从安全上下文中获取当前认证用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String username = authentication.getName();
            
            return userService.findByUsername(username)
                    .map(user -> {
                        // 不返回密码
                        user.setPassword(null);
                        return ApiResult.success(user);
                    })
                    .orElse(ApiResult.error(HttpStatus.NOT_FOUND.value(), "用户不存在"));
        } catch (Exception e) {
            log.error("获取用户信息失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取用户信息失败: " + e.getMessage());
        }
    }
    
    @GetMapping
    public ApiResult<List<User>> getAllUsers(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            log.info("获取用户列表请求: username={}, departmentId={}, startTime={}, endTime={}, pageNum={}, pageSize={}",
                     username, departmentId, startTime, endTime, pageNum, pageSize);
            
            List<User> users = userService.findAllUsers();
            
            // 过滤掉管理员用户（ID为1）
            users = users.stream()
                    .filter(user -> user.getId() != null && !user.getId().equals(1L))
                    .collect(Collectors.toList());
            
            // 简单过滤实现
            if (username != null && !username.isEmpty()) {
                users = users.stream()
                        .filter(user -> user.getUsername().toLowerCase().contains(username.toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            if (departmentId != null) {
                users = users.stream()
                        .filter(user -> {
                            if (user.getDepartment() == null) {
                                return false;
                            }
                            return user.getDepartment().getId().equals(departmentId);
                        })
                        .collect(Collectors.toList());
            }
            
            // 简单的分页实现
            int total = users.size();
            int fromIndex = (pageNum - 1) * pageSize;
            int toIndex = Math.min(fromIndex + pageSize, total);
            
            if (fromIndex < total) {
                users = users.subList(fromIndex, toIndex);
            } else {
                users = new ArrayList<>();
            }
            
            // 移除所有用户的密码
            users.forEach(user -> user.setPassword(null));
            
            return ApiResult.success(users);
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "获取用户列表失败: " + e.getMessage());
        }
    }
    
    @PostMapping
    public ApiResult<User> createUser(@RequestBody Map<String, Object> requestMap) {
        try {
            String username = (String) requestMap.get("username");
            String password = (String) requestMap.get("password");
            Number departmentIdObj = (Number) requestMap.get("departmentId");
            
            log.info("创建用户请求: username={}, departmentId={}", username, departmentIdObj);
            
            // 检查用户是否存在
            if (userService.checkIfUserExistsByUsername(username)) {
                return ApiResult.error(HttpStatus.BAD_REQUEST.value(), "用户名已存在");
            }
            
            // 创建用户对象
            User user = new User();
            user.setUsername(username);
            user.setPassword(password);
            
            // 设置部门
            if (departmentIdObj != null) {
                Long departmentId = departmentIdObj.longValue();
                Optional<Department> departmentOpt = departmentRepository.findById(departmentId);
                if (departmentOpt.isPresent()) {
                    user.setDepartment(departmentOpt.get());
                } else {
                    log.warn("部门ID不存在: {}", departmentId);
                }
            }
            
            // 设置默认值
            user.setRole("user");
            user.setStatus(true);
            user.setCreatedAt(LocalDateTime.now());
            user.setUpdatedAt(LocalDateTime.now());
            
            // 创建用户
            User createdUser = userService.createUser(user);
            
            // 返回创建的用户(不包括密码)
            createdUser.setPassword(null);
            return ApiResult.success("用户创建成功", createdUser);
        } catch (Exception e) {
            log.error("创建用户失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "创建用户失败: " + e.getMessage());
        }
    }
    
    @PutMapping("/{id}")
    public ApiResult<User> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> requestMap) {
        try {
            User existingUser = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在"));
            
            // 更新部门信息
            Number departmentIdObj = (Number) requestMap.get("departmentId");
            if (departmentIdObj != null) {
                Long departmentId = departmentIdObj.longValue();
                Optional<Department> departmentOpt = departmentRepository.findById(departmentId);
                if (departmentOpt.isPresent()) {
                    existingUser.setDepartment(departmentOpt.get());
                } else {
                    log.warn("部门ID不存在: {}", departmentId);
                }
            }
            
            // 更新密码（如果提供了新密码）
            String password = (String) requestMap.get("password");
            if (password != null && !password.trim().isEmpty()) {
                existingUser.setPassword(password);
            }
            
            existingUser.setUpdatedAt(LocalDateTime.now());
            User updatedUser = userService.updateUser(existingUser);
            updatedUser.setPassword(null);
            
            return ApiResult.success("用户更新成功", updatedUser);
        } catch (Exception e) {
            log.error("更新用户失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "更新用户失败: " + e.getMessage());
        }
    }
    
    @DeleteMapping("/{id}")
    public ApiResult<Void> deleteUser(@PathVariable Long id) {
        try {
            if (!userService.findById(id).isPresent()) {
                return ApiResult.error(HttpStatus.NOT_FOUND.value(), "用户不存在");
            }
            
            userService.deleteUser(id);
            return ApiResult.success("用户删除成功");
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return ApiResult.error(HttpStatus.INTERNAL_SERVER_ERROR.value(), "删除用户失败: " + e.getMessage());
        }
    }
}