<template>
  <div class="shared-chat-container">
    <div class="shared-chat-header">
      <div class="header-left">
        <h3 class="session-title">{{ chatSession.title || '共享对话' }}</h3>
      </div>
      <div class="header-right">
        <el-button size="small" type="primary" @click="exportChat" icon="el-icon-download" style="margin-right: 10px;">导出对话</el-button>
        <el-button size="small" @click="goBack" icon="el-icon-back">返回</el-button>
      </div>
    </div>
    
    <div class="shared-messages-container">
      <el-skeleton :loading="loading" animated>
        <template slot="template">
          <div v-for="i in 3" :key="i" class="skeleton-message">
            <el-skeleton-item variant="circle" style="width: 40px; height: 40px; margin-right: 16px;" />
            <div style="flex: 1;">
              <el-skeleton-item variant="p" style="width: 50%" />
              <el-skeleton-item variant="text" style="width: 100%; margin-top: 8px;" />
              <el-skeleton-item variant="text" style="width: 80%; margin-top: 8px;" />
            </div>
          </div>
        </template>
        
        <template>
          <div class="messages-list" ref="messagesList">
            <template v-if="messages.length > 0">
              <div v-for="(message, index) in messages" :key="index" 
                class="message-item" 
                :class="[message.role === 'assistant' ? 'ai-message' : 'user-message']"
              >
                <div class="message-header">
                  <div class="message-role">
                    <span class="role-icon">
                      <i :class="message.role === 'assistant' ? 'el-icon-s-custom' : 'el-icon-user'"></i>
                    </span>
                    <span class="role-name">{{ message.role === 'assistant' ? 'AI助手' : '用户' }}</span>
                  </div>
                  <div class="message-time">
                    {{ formatTime(message.createdAt) }}
                  </div>
                </div>
                
                <div class="message-content">
                  <div v-if="message.role === 'assistant' && message.imageUrl" class="image-message">
                    <img :src="message.imageUrl" alt="AI生成的图片" class="ai-generated-image" />
                  </div>
                  <div v-else-if="isUserImageMessage(message)" class="user-image-message">
                    <img :src="extractUserImageUrl(message)" alt="用户上传的图片" class="user-uploaded-image" />
                  </div>
                  <div v-else class="message-text" v-html="formatMessage(message.content)"></div>
                </div>
              </div>
            </template>
            
            <div v-else class="empty-messages">
              <i class="el-icon-chat-dot-square"></i>
              <p>该对话不存在或已被删除</p>
            </div>
          </div>
        </template>
      </el-skeleton>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import { mapGetters } from 'vuex';
import { marked } from 'marked';
import DOMPurify from 'dompurify';
import hljs from 'highlight.js';
import 'highlight.js/styles/vs2015.css';
import { saveAs } from 'file-saver';

// 配置 marked
marked.setOptions({
  highlight: function(code, lang) {
    const language = hljs.getLanguage(lang) ? lang : 'plaintext';
    return hljs.highlight(code, { language }).value;
  },
  langPrefix: 'hljs language-',
  breaks: true
});

export default {
  name: 'SharedChat',
  data() {
    return {
      loading: true,
      messages: [],
      chatSession: {},
      shareId: null
    };
  },
  computed: {
    ...mapGetters(['baseUrl'])
  },
  created() {
    this.shareId = this.$route.params.shareId;
    this.loadSharedChat();
  },
  methods: {
    async loadSharedChat() {
      try {
        this.loading = true;
        console.log('正在加载共享对话:', this.shareId);
        
        // 构建API URL，确保使用正确的baseUrl和路径格式
        const baseApiUrl = `${this.baseUrl || ''}/share/api`;
        console.log('API基础URL:', baseApiUrl);
        
        // 获取会话信息
        console.log('获取会话信息...');
        const sessionUrl = `${baseApiUrl}/session/${this.shareId}`;
        console.log('Session API URL:', sessionUrl);
        
        const sessionResponse = await axios.get(sessionUrl);
        console.log('会话信息响应:', sessionResponse);
        
        if (sessionResponse.data.code === 200) {
          this.chatSession = sessionResponse.data.data;
          console.log('成功获取会话信息:', this.chatSession);
        } else {
          console.warn('获取会话信息失败:', sessionResponse.data);
          this.$message.warning(sessionResponse.data.message || '获取会话信息失败');
        }
        
        // 获取会话消息
        console.log('获取会话消息...');
        const messagesUrl = `${baseApiUrl}/${this.shareId}`;
        console.log('Messages API URL:', messagesUrl);
        
        const messagesResponse = await axios.get(messagesUrl);
        console.log('会话消息响应:', messagesResponse);
        
        if (messagesResponse.data.code === 200) {
          this.messages = messagesResponse.data.data;
          console.log('成功获取消息列表:', this.messages);
        } else {
          console.warn('获取消息列表失败:', messagesResponse.data);
          this.$message.warning(messagesResponse.data.message || '获取消息列表失败');
        }
      } catch (error) {
        console.error('加载共享对话失败:', error);
        console.error('错误详情:', error.response || error.message || error);
        
        if (error.response) {
          if (error.response.status === 401) {
            // 如果是未授权错误，不跳转到登录页面
            this.$message.error('访问未授权，请检查链接是否正确');
          } else if (error.response.status === 404) {
            this.$message.error('该对话不存在或已被删除');
          } else {
            this.$message.error(`加载失败(${error.response.status}): ${error.response.data?.message || '未知错误'}`);
          }
        } else {
          this.$message.error(`加载失败: ${error.message || '网络错误，请稍后再试'}`);
        }
      } finally {
        this.loading = false;
      }
    },
    formatTime(timestamp) {
      if (!timestamp) return '';
      const date = new Date(timestamp);
      return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`;
    },
    formatMessage(content) {
      if (!content) return '';
      
      // 使用DOMPurify净化HTML，防止XSS攻击
      const sanitized = DOMPurify.sanitize(marked.parse(content));
      return sanitized;
    },
    isUserImageMessage(message) {
      if (message.role !== 'user') return false;
      
      try {
        const content = JSON.parse(message.content);
        return content && content.type === 'image' && content.url;
      } catch (e) {
        return false;
      }
    },
    extractUserImageUrl(message) {
      try {
        const content = JSON.parse(message.content);
        return content.url;
      } catch (e) {
        return '';
      }
    },
    goBack() {
      if (window.history.length > 1) {
        this.$router.go(-1);
      } else {
        this.$router.push('/');
      }
    },
    async exportChat() {
      try {
        // 准备导出内容
        let content = `# ${this.chatSession.title || '共享对话'}\n\n`;
        content += `导出时间：${new Date().toLocaleString()}\n\n`;
        
        // 添加消息内容
        for (const message of this.messages) {
          const role = message.role === 'assistant' ? 'AI助手' : '用户';
          const time = this.formatTime(message.createdAt);
          content += `## ${role} (${time})\n\n`;
          
          if (message.role === 'assistant' && message.imageUrl) {
            content += `![AI生成的图片](${message.imageUrl})\n\n`;
          } else if (this.isUserImageMessage(message)) {
            content += `![用户上传的图片](${this.extractUserImageUrl(message)})\n\n`;
          } else {
            // 移除HTML标签，保留纯文本
            const plainText = message.content.replace(/<[^>]+>/g, '');
            content += `${plainText}\n\n`;
          }
        }
        
        // 创建Blob对象
        const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
        
        // 生成文件名
        const fileName = `${this.chatSession.title || '共享对话'}_${new Date().toISOString().slice(0,10)}.md`;
        
        // 下载文件
        saveAs(blob, fileName);
        
        this.$message.success('对话导出成功');
      } catch (error) {
        console.error('导出对话失败:', error);
        this.$message.error('导出对话失败，请稍后重试');
      }
    }
  }
};
</script>

<style scoped>
.shared-chat-container {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background-color: #f5f7fa;
}

.shared-chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  background-color: #fff;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.session-title {
  margin: 0;
  font-size: 18px;
  font-weight: 500;
}

.shared-messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.skeleton-message {
  display: flex;
  padding: 16px;
  margin-bottom: 16px;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.message-item {
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.ai-message {
  background-color: #fff;
}

.user-message {
  background-color: #e6f7ff;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  background-color: rgba(0, 0, 0, 0.03);
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
}

.message-role {
  display: flex;
  align-items: center;
  gap: 8px;
}

.role-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f2f5;
  border-radius: 50%;
}

.ai-message .role-icon {
  background-color: #e6f7ff;
  color: #1890ff;
}

.user-message .role-icon {
  background-color: #f6ffed;
  color: #52c41a;
}

.message-time {
  font-size: 12px;
  color: #8c8c8c;
}

.message-content {
  padding: 16px;
  font-size: 14px;
  line-height: 1.6;
}

.image-message {
  text-align: center;
}

.ai-generated-image {
  max-width: 100%;
  max-height: 500px;
  border-radius: 4px;
}

.user-image-message {
  text-align: center;
}

.user-uploaded-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.1);
}

.empty-messages {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #bfbfbf;
}

.empty-messages i {
  font-size: 60px;
  margin-bottom: 16px;
}

/* 消息内容中代码块的样式 */
:deep(.message-text pre) {
  background-color: #282c34;
  border-radius: 6px;
  padding: 16px;
  overflow: auto;
  margin: 16px 0;
}

:deep(.message-text code) {
  font-family: 'Courier New', monospace;
  font-size: 14px;
}

:deep(.message-text p) {
  margin: 8px 0;
}

:deep(.message-text ul, .message-text ol) {
  padding-left: 24px;
}

:deep(.message-text table) {
  border-collapse: collapse;
  width: 100%;
  margin: 16px 0;
}

:deep(.message-text th, .message-text td) {
  border: 1px solid #e8e8e8;
  padding: 8px 16px;
  text-align: left;
}

:deep(.message-text th) {
  background-color: #fafafa;
  font-weight: 500;
}

@media (max-width: 767px) {
  .shared-chat-header {
    padding: 12px 16px;
  }
  
  .session-title {
    font-size: 16px;
  }
  
  .shared-messages-container {
    padding: 12px;
  }
  
  .message-content {
    padding: 12px;
    font-size: 13px;
  }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style> 