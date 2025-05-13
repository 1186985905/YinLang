import request from '@/utils/request'

// 获取部门列表
export function getDepartmentList() {
    return request({
        url: '/api/departments',
        method: 'get'
    })
}

// 获取部门详情
export function getDepartmentById(id) {
    return request({
        url: `/api/departments/${id}`,
        method: 'get'
    })
}

// 创建部门
export function createDepartment(data) {
    return request({
        url: '/api/departments',
        method: 'post',
        data: {
            name: data.name,
            description: data.description
        }
    })
}

// 更新部门
export function updateDepartment(id, data) {
    return request({
        url: `/api/departments/${id}`,
        method: 'put',
        data: {
            name: data.name,
            description: data.description
        }
    })
}

// 删除部门
export function deleteDepartment(id) {
    return request({
        url: `/api/departments/${id}`,
        method: 'delete'
    })
}

// 提示词模板接口
export function getPromptList(departmentId) {
    return request({
        url: `/api/departments/${departmentId}/prompts`,
        method: 'get'
    })
}

export function addPrompt(data) {
    const { department_id, ...promptData } = data
    return request({
        url: `/api/departments/${department_id}/prompts`,
        method: 'post',
        data: promptData
    })
}

export function updatePrompt(id, data) {
    const { department_id, ...promptData } = data
    return request({
        url: `/api/departments/${department_id}/prompts/${id}`,
        method: 'put',
        data: promptData
    })
}

export function deletePrompt(id, departmentId) {
    return request({
        url: `/api/departments/${departmentId}/prompts/${id}`,
        method: 'delete'
    })
} 