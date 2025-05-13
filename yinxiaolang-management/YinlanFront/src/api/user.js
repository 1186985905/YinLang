import request from '@/utils/request'

// 获取用户列表
export function getUserList(params) {
    // 处理分页参数
    const pageNum = params.page || 1
    const pageSize = params.pageSize || 10

    // 处理时间范围
    let startTime = undefined
    let endTime = undefined
    if (params.timeRange && params.timeRange.length === 2) {
        startTime = params.timeRange[0]
        endTime = params.timeRange[1]
    }

    return request({
        url: '/api/users',
        method: 'get',
        params: {
            username: params.username || undefined,
            departmentId: params.departmentId || undefined,
            startTime,
            endTime,
            pageNum,
            pageSize
        }
    })
}

// 获取单个用户信息
export function getUserById(id) {
    return request({
        url: `/api/users/${id}`,
        method: 'get'
    })
}

// 创建用户
export function createUser(data) {
    return request({
        url: '/api/users',
        method: 'post',
        data: {
            username: data.username,
            password: data.password,
            departmentId: data.departmentId
        }
    })
}

// 更新用户
export function updateUser(id, data) {
    // 构造请求数据对象
    const requestData = {
        departmentId: data.departmentId
    }
    
    // 如果有密码，则添加密码字段
    if (data.password) {
        requestData.password = data.password
    }
    
    return request({
        url: `/api/users/${id}`,
        method: 'put',
        data: requestData
    })
}

// 删除用户
export function deleteUser(id) {
    return request({
        url: `/api/users/${id}`,
        method: 'delete'
    })
}

// 用户登录
export function login(username, password) {
    return request({
        url: '/api/users/login',
        method: 'post',
        data: {
            username,
            password
        }
    })
}  