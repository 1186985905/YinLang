import axios from 'axios'
import { Message } from 'element-ui'

// 创建axios实例
const service = axios.create({
    baseURL: process.env.VUE_APP_BASE_API || '',
    timeout: 15000, // 请求超时时间
    headers: {
        'Content-Type': 'application/json'
    }
})

// 请求拦截器
service.interceptors.request.use(
    config => {
        // 在请求发送之前做一些处理
        const token = localStorage.getItem('token')
        if (token) {
            config.headers['Authorization'] = `Bearer ${token}`
        }

        // 处理GET请求参数
        if (config.method === 'get' && config.params) {
            // 移除值为undefined或null的参数
            Object.keys(config.params).forEach(key => {
                if (config.params[key] === undefined || config.params[key] === null) {
                    delete config.params[key]
                }
            })
        }

        return config
    },
    error => {
        console.error('请求错误:', error)
        return Promise.reject(error)
    }
)

// 响应拦截器
service.interceptors.response.use(
    response => {
        // 如果是下载文件等特殊请求，直接返回
        if (response.config.responseType === 'blob') {
            return response
        }

        // 统一处理响应数据
        const res = response.data

        // 如果后端直接返回数组或对象，包装成统一格式
        if (Array.isArray(res) || (typeof res === 'object' && res !== null && !res.code)) {
            return {
                code: 200,
                data: res,
                message: 'success'
            }
        }

        // 处理后端返回的标准格式响应
        if (res.code && res.code !== 200) {
            Message({
                message: res.message || '请求失败',
                type: 'error',
                duration: 5 * 1000
            })
            return Promise.reject(new Error(res.message || '请求失败'))
        }

        return res
    },
    error => {
        console.error('响应错误:', error)
        const { response } = error
        let message = '服务器错误'

        if (response) {
            switch (response.status) {
                case 400:
                    message = response.data?.message || '请求参数错误'
                    break
                case 401:
                    message = '未授权，请重新登录'
                    // 清除token并跳转到登录页
                    localStorage.removeItem('token')
                    setTimeout(() => {
                        window.location.href = '/login'
                    }, 1500)
                    break
                case 403:
                    message = '拒绝访问，请重新登录'
                    // 清除token并跳转到登录页
                    localStorage.removeItem('token')
                    localStorage.removeItem('user')
                    setTimeout(() => {
                        window.location.href = '/login'
                    }, 1500)
                    break
                case 404:
                    message = '请求的资源不存在'
                    break
                case 500:
                    message = '服务器错误'
                    break
                default:
                    message = response.data?.message || `请求失败(${response.status})`
            }
        } else if (error.request) {
            message = '网络错误，请检查您的网络连接'
        } else {
            message = error.message
        }

        Message({
            message: message,
            type: 'error',
            duration: 5 * 1000
        })
        return Promise.reject(error)
    }
)

export default service 