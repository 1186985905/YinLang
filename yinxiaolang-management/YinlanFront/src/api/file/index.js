import request from '@/utils/request'

/**
 * 上传文件并返回可访问的URL
 * @param {FormData} formData 包含文件的FormData对象
 */
export function uploadFile(formData) {
  return request({
    url: '/api/file/upload',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 解析文档内容（支持.txt和.docx文件）
 * @param {FormData} formData 包含文件的FormData对象
 */
export function parseDocument(formData) {
  return request({
    url: '/api/file/parse-document',
    method: 'post',
    data: formData,
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 根据文件URL获取文档内容
 * @param {String} url 文档URL
 */
export function getDocumentContent(url) {
  return request({
    url: '/api/file/get-document-content',
    method: 'post',
    data: { url }
  })
}

/**
 * 将本地图片转换为Base64编码
 * @param {String} url 本地图片URL
 */
export function proxyImage(url) {
  return request({
    url: '/api/file/proxy-image',
    method: 'post',
    data: { url }
  })
}

/**
 * 获取文件下载链接
 * @param {String} fileId 文件ID或路径
 */
export function getFileDownloadUrl(fileId) {
  return request({
    url: `/api/file/download/${fileId}`,
    method: 'get',
    responseType: 'blob'
  })
}

/**
 * 直接下载文件
 * @param {String} url 文件URL
 * @param {String} filename 下载后的文件名
 */
export function downloadFile(url, filename) {
  return fetch(url)
    .then(response => response.blob())
    .then(blob => {
      const objectUrl = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = objectUrl
      link.download = filename || 'downloaded-file'
      link.click()
      window.URL.revokeObjectURL(objectUrl)
    })
} 