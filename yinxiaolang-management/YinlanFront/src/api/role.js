import request from '@/utils/request'

// 获取角色列表
export function getRoleList() {
    return request({
        url: '/api/roles',
        method: 'get'
    })
}

// 获取单个角色信息
export function getRoleById(id) {
    return request({
        url: `/api/roles/${id}`,
        method: 'get'
    })
} 