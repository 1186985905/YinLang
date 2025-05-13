import request from '@/utils/request'

// 获取模型列表
export function getModelList(params) {
    return request({
        url: '/models',
        method: 'get',
        params
    })
}

// 新增模型
export function addModel(data) {
    // 转换数据格式
    const modelData = {
        modelName: String(data.modelName),
        modelType: String(data.modelType),
        provider: String(data.provider),
        status: String(data.status),
        apiKey: String(data.apiKey)
    }
    return request({
        url: '/models',
        method: 'post',
        data: modelData
    })
}

// 删除单个模型
export function deleteModel(id) {
    return request({
        url: `/models/${id}`,
        method: 'delete'
    })
}

// 批量删除模型
export function batchDeleteModels(data) {
    return request({
        url: '/models/batch',
        method: 'delete',
        data
    })
}

// 更新模型状态
export function updateModelStatus(id, status) {
    return request({
        url: `/models/${id}/status`,
        method: 'patch',
        data: { status }
    })
} 