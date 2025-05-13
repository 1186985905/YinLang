import request from '@/utils/request'

/**
 * 获取聊天会话列表
 * @param {Number} userId 用户ID
 */
export function getChatSessions(userId) {
  return request({
    url: '/api/chat/sessions',
    method: 'get',
    params: { userId }
  })
}

/**
 * 创建新的聊天会话
 * @param {String} modelType 模型类型
 * @param {Number} userId 用户ID（可选）
 */
export function createChatSession(modelType, userId) {
  return request({
    url: '/api/chat/session',
    method: 'post',
    params: { modelType, userId }
  })
}

/**
 * 获取会话的消息历史
 * @param {String} sessionId 会话ID
 */
export function getChatMessages(sessionId) {
  return request({
    url: `/api/chat/messages/${sessionId}`,
    method: 'get'
  })
}

/**
 * 删除聊天会话
 * @param {String} sessionId 会话ID
 */
export function deleteChatSession(sessionId) {
  return request({
    url: `/api/chat/session/${sessionId}`,
    method: 'delete'
  })
}

/**
 * 重命名聊天会话
 * @param {String} sessionId 会话ID
 * @param {String} title 新标题
 */
export function renameChatSession(sessionId, title) {
  return request({
    url: `/api/chat/session/${sessionId}/title`,
    method: 'put',
    data: { title }
  })
}

/**
 * 发送聊天消息(非流式)
 * @param {Object} data 包含sessionId和message的对象
 */
export function sendChatMessage(data) {
  return request({
    url: '/api/chat/message',
    method: 'post',
    data
  })
}

/**
 * 初始化流式响应
 * @param {Object} data 包含sessionId、message和modelType的对象
 */
export function initStreamResponse(data) {
  return request({
    url: '/api/chat/stream',
    method: 'post',
    data
  })
}

/**
 * 获取支持的模型列表
 */
export function getModelTypes() {
  return request({
    url: '/api/chat/models',
    method: 'get'
  })
}

/**
 * 创建EventSource用于流式接收消息
 * @param {String} sessionId 会话ID
 * @returns {EventSource} 返回EventSource实例
 */
export function createStreamConnection(sessionId) {
  const baseUrl = process.env.VUE_APP_BASE_API || ''
  return new EventSource(`${baseUrl}/api/chat/stream/${sessionId}`)
}

/**
 * 获取共享聊天会话的消息
 * @param {String} shareId 共享ID
 */
export function getSharedChatMessages(shareId) {
  return request({
    url: `/share/api/${shareId}`,
    method: 'get'
  })
}

/**
 * 获取共享聊天会话信息
 * @param {String} shareId 共享ID
 */
export function getSharedChatSession(shareId) {
  return request({
    url: `/share/api/session/${shareId}`,
    method: 'get'
  })
}