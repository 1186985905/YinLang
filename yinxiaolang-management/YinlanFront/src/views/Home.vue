<template>
  <div>
    <app-layout
      :sidebar-active="sidebarActive"
      active-tab="text"
      sidebar-title="历史对话"
      new-button-text="新对话"
      @toggle-sidebar="toggleSidebar"
      @create-new-chat="createNewChat"
      @command="handleCommand"
      @share="shareHistory"
    >
      <template v-slot:sidebar-content>
        <div
          v-for="(chat, index) in chatHistoryList"
          :key="index"
          class="history-item"
          :class="{ active: currentChatIndex === index }"
          @click="switchChat(index)"
        >
          <div class="history-item-content">
            <i class="el-icon-chat-dot-round"></i>
            <span class="history-title">{{ chat.title }}</span>
          </div>
          <el-dropdown
            trigger="click"
            @command="(cmd) => handleChatAction(cmd, index)"
            @click.stop
          >
            <i class="el-icon-more"></i>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="rename">重命名</el-dropdown-item>
              <el-dropdown-item command="share">导出</el-dropdown-item>
              <el-dropdown-item command="delete">删除</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </template>

      <!-- 聊天记录区域 -->
      <div class="chat-history" ref="chatHistory">
        <div class="greeting-section" v-if="currentChat.messages.length === 0">
          <div class="greeting-header">
            <img src="../assets/back.png" alt="Logo" class="logo-image" />
          <h1 class="greeting-title">Hi~ 我是茵小浪</h1>
          </div>
          <p class="greeting-subtitle">
            你身边的智能助手，可以为你答疑解惑、尽情创作，快来点击下任一功能体验吧~
          </p>
        </div>

        <!-- 提示词区域 -->
        <div class="prompt-section" v-if="currentChat.messages.length === 0">
          <div class="prompt-title">你可以这样问</div>
          <div class="prompt-list">
            <div
              class="prompt-item"
              @click="selectPrompt('午睡20分钟为什么能重置大脑运行效率？')"
            >
              午睡20分钟为什么能重置大脑运行效率？
            </div>
            <div
              class="prompt-item"
              @click="selectPrompt('问歌性斯答能减肥的原理是什么？')"
            >
              问歌性斯答能减肥的原理是什么？
            </div>
            <div
              class="prompt-item"
              @click="
                selectPrompt('作为忙碌的成年人，如何重建生活和心理的秩序？')
              "
            >
              作为忙碌的成年人，如何重建生活和心理的秩序？
            </div>
          </div>
        </div>

        <!-- 历史聊天记录展示区域 -->
        <div class="chat-messages" v-if="currentChat.messages.length > 0">
          <div
            v-for="(message, index) in currentChat.messages"
            :key="index"
            :class="[
              'message',
              message.role === 'user' ? 'user-message' : 'assistant-message',
              message.isError ? 'error-message' : '',
              message.isLoading ? 'loading-message' : '',
              message.isStreaming ? 'streaming-message' : '',
              message.highlightNew ? 'highlight-new' : ''
            ]"
          >
            <div class="message-avatar" v-if="message.role === 'assistant'">
              <img src="../assets/ai.png" alt="AI" class="avatar-image" />
            </div>
            <div class="message-avatar user-avatar-container" v-else>
              <div class="user-message-avatar">{{ userInitial }}</div>
            </div>
            <div class="message-bubble">
              <div class="message-content"
                v-if="message.role === 'user' || (!message.imageUrl && message.role === 'assistant')">
                <!-- 用户消息 - 添加文档解析和图片显示处理 -->
                <template v-if="message.role === 'user'">
                  <template v-if="message.content && message.content.startsWith('已解析文件:')">
                    <div class="file-parse-message">
                      <i class="el-icon-document"></i> 解析: {{ extractFileName(message.content) }}
                    </div>
                  </template>
                  <template v-else-if="isUserImageMessage(message.content)">
                    <div class="user-image-message">
                      <img :src="extractUserImageUrl(message.content)" alt="用户上传的图片" class="user-uploaded-image" />
                      <div class="image-description">分析这张图片</div>
                    </div>
                  </template>
                  <template v-else>
                    {{ message.content }}
                  </template>
                </template>
                
                <!-- AI消息 -->
                <template v-else>
                  <!-- 图片消息处理 -->
                  <template v-if="isImageMessage(message.content)">
                    <div class="ai-image-message">
                      <img :src="extractImageUrl(message.content)" alt="AI生成的图片" class="ai-generated-image" />
                      <div class="image-actions">
                        <el-button size="small" icon="el-icon-download" @click="downloadImage(extractImageUrl(message.content))">下载</el-button>
                        <el-button size="small" :icon="message.liked ? 'el-icon-star-on' : 'el-icon-star-off'" 
                                  :class="{'like-button': true, 'liked': message.liked}"
                                  @click="likeImage(message)">{{ message.liked ? '已点赞' : '点赞' }}</el-button>
                      </div>
                    </div>
                  </template>
                  
                  <!-- 普通文本消息 -->
                  <template v-else>
                    {{ message.content }}
                  </template>
                  </template>
              </div>
              
              <div class="message-actions" v-if="message.role === 'assistant'">
                <i class="el-icon-document-copy" title="复制" @click="copyMessage(message)"></i>
                <i class="el-icon-delete" title="删除" @click="deleteMessage(index)"></i>
                <i class="el-icon-refresh" title="重新生成" @click="regenerateMessage(index)"></i>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 滚动到底部提示按钮 -->
        <div class="scroll-to-bottom-indicator" v-if="showScrollIndicator" @click="forceScrollToBottom">
          <i class="el-icon-arrow-down"></i>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <div class="input-container">
        <div class="input-expander" @click="toggleInputExpanded">
          <i :class="inputExpanded ? 'el-icon-caret-bottom' : 'el-icon-caret-top'"></i>
        </div>
        <div class="input-area" :class="{ 'expanded': inputExpanded }">
          <div class="model-name-display">
            <el-dropdown trigger="click" @command="changeModel">
              <div class="dropdown-trigger">
            <i class="el-icon-cpu"></i>
                {{ currentModel }}
                <i class="el-icon-arrow-down"></i>
              </div>
              <el-dropdown-menu slot="dropdown">
                <el-dropdown-item command="GPT-4o">GPT-4o</el-dropdown-item>
                <el-dropdown-item command="DeepSeek">DeepSeek</el-dropdown-item>
                <el-dropdown-item command="qianwen">qianwen</el-dropdown-item>
              </el-dropdown-menu>
            </el-dropdown>
          </div>
          <el-input
            type="textarea"
            :rows="inputExpanded ? 4 : 1"
            placeholder="输入你的问题，按回车发送"
            v-model="userInput"
            class="chat-input"
            resize="none"
            @keydown.enter.native.prevent="sendMessage"
          >
          </el-input>
          <div class="input-actions">
            <el-button icon="el-icon-link" circle size="small" @click="handleFileUpload"></el-button>
            <el-button
              icon="el-icon-picture-outline"
              circle
              size="small"
              @click="handleImageUpload"
            ></el-button>
            <el-button
              icon="el-icon-microphone"
              circle
              size="small"
              @click="startVoiceInput"
            ></el-button>
          </div>
          <el-button
            type="primary"
            icon="el-icon-s-promotion"
            class="send-button"
            :disabled="!userInput.trim()"
            @click="sendMessage"
          ></el-button>
        </div>
      </div>
    </app-layout>

    <!-- 偏好设置对话框 -->
    <user-preferences 
      :visible.sync="preferencesVisible"
      @save="savePreferences"
    />

    <!-- 共享对话框 -->
    <share-dialog
      :visible.sync="shareDialogVisible"
      :share-link="shareLink"
      @close="shareDialogVisible = false"
    />
  </div>
</template>

<script>
import { mapGetters, mapActions } from "vuex";
import AppLayout from "@/layout/AppLayout.vue";
import UserPreferences from "@/components/UserPreferences.vue";
import ShareDialog from "@/components/ShareDialog.vue";
import { getChatSessions, getChatMessages, createChatSession, deleteChatSession, renameChatSession, initStreamResponse } from '@/api/chat'
import { uploadFile, parseDocument } from '@/api/file'
import { saveAs } from 'file-saver';

export default {
  name: "HomeView",
  components: {
    AppLayout,
    UserPreferences,
    ShareDialog
  },
  data() {
    return {
      userInput: "",
      currentChatIndex: 0,
      sidebarActive: true, // 默认桌面端侧边栏为展开状态
      preferencesVisible: false,
      inputExpanded: false,
      shareDialogVisible: false,
      shareLink: "",
      chatHistoryList: [],
      currentSessionId: null,
      isLoading: false,
      // 流式响应相关
      streamedResponses: new Map(),
      // 存储完整文档内容
      fullDocumentContent: null,
      // 用户滚动状态
      userScrolled: false,
      scrollTimer: null,
    };
  },
  computed: {
    ...mapGetters([
      "currentModel",
      "availableModels",
      "currentUser",
      "isAdmin",
    ]),
    userInitial() {
      return this.currentUser
        ? this.currentUser.username.charAt(0).toUpperCase()
        : "?";
    },
    currentChat() {
      return (
        this.chatHistoryList[this.currentChatIndex] || {
          id: 0,
          title: "新对话",
          messages: [],
        }
      );
    },
    isMobile() {
      return window.innerWidth <= 767;
    },
    // 添加计算属性，控制滚动提示按钮的显示与隐藏
    showScrollIndicator() {
      return this.userScrolled && this.isLoading;
    },
  },
  methods: {
    ...mapActions(["setCurrentModel", "logout", "toggleAccountModal"]),
    
    // 检查用户输入是否只是一个文件名
    isJustFileName(input) {
      return input && 
             (input.endsWith('.txt') || 
              input.endsWith('.docx') || 
              input.endsWith('.doc')) && 
             !input.includes('\n');
    },
    
    // 清理文本内容方法
    cleanTextContent(text) {
      if (!text) return '';
      
      // 替换连续的多个问号为单个问号
      text = text.replace(/\?{2,}/g, '?');
      
      // 移除末尾的问号
      text = text.replace(/\s*\?+\s*$/, '');
      
      // 去掉不必要的空格
      text = text.replace(/\s+([,.!?;:])/g, '$1');
      
      return text;
    },
    
    async loadUserSessions() {
      try {
        const response = await getChatSessions();
        
        if (response.code === 200) {
          // 转换会话数据格式
          const sessions = response.data || [];
          
          // 清空当前会话列表
          this.chatHistoryList = [];
          
          // 加载所有会话
          for (const session of sessions) {
            const messagesResponse = await getChatMessages(session.id);
            
            if (messagesResponse.code === 200) {
              // 转换消息格式
              const messages = messagesResponse.data.map(msg => ({
                role: msg.role,
                content: msg.content
              }));
              
              // 添加到会话列表
              this.chatHistoryList.push({
                id: session.id,
                title: session.title,
                messages: messages,
                defaultModelType: session.defaultModelType
              });
            }
          }
          
          // 如果有会话，选择第一个会话
          if (this.chatHistoryList.length > 0) {
            this.currentChatIndex = 0;
            this.currentSessionId = this.chatHistoryList[0].id;
          } else {
            // 如果没有会话，创建一个新会话
            await this.createNewChatSession();
          }
        } else {
          this.$message.error(`获取会话列表失败: ${response.message}`);
        }
      } catch (error) {
        console.error('获取会话列表出错:', error);
        this.$message.error(`获取会话列表出错: ${error.message || '服务器错误'}`);
      }
    },
    
    async createNewChatSession() {
      try {
        // 调用后端API创建新会话
        const response = await createChatSession(this.currentModel.toLowerCase());
        
        if (response.code === 200) {
          // 创建新对话
          const newChat = {
            id: response.data.id,
            title: "新对话",
            messages: [],
            defaultModelType: response.data.defaultModelType
          };
          
          // 添加到对话列表
          this.chatHistoryList.unshift(newChat);
          this.currentChatIndex = 0;
          this.currentSessionId = newChat.id;
          
          // 确保滚动到顶部
          this.scrollToBottom();
          
          return newChat;
        } else {
          this.$message.error(`创建对话失败: ${response.message}`);
        }
      } catch (error) {
        console.error('创建对话出错:', error);
        this.$message.error(`创建对话出错: ${error.message || '服务器错误'}`);
      }
    },
    
    async sendMessage() {
      if (!this.userInput.trim()) return;

      // 如果是新建的对话且没有会话ID，先创建会话
      if (!this.currentChat.id || this.currentChat.id === 0) {
        await this.createNewChatSession();
      }

      // 保存用户输入
      let userMessage = this.userInput;
      
      // 如果有完整文档内容且用户输入是纯文件名，添加文档内容
      if (this.fullDocumentContent && this.isJustFileName(this.userInput)) {
        userMessage = `已解析文件: ${this.userInput}\n\n${this.fullDocumentContent}`;
        // 重置完整文档内容
        this.fullDocumentContent = null;
      }

      // 添加用户消息到聊天历史
      this.currentChat.messages.push({
        role: "user",
        content: userMessage,
      });
      
      // 清空输入框
      this.userInput = ""; 
      
      // 初始滚动到底部
      this.scrollToBottom();
      
      // 显示加载状态
      this.isLoading = true;
      
      // 添加临时的AI消息，显示加载中
      const tempMessageIndex = this.currentChat.messages.length;
      const messageId = Date.now().toString();
      this.currentChat.messages.push({
        id: messageId,
        role: "assistant",
        content: "思考中...",
        isLoading: true,
        isStreaming: true,
        highlightNew: true
      });
      
      // 再次滚动到底部，确保加载中消息可见
      this.scrollToBottom();
      
      // 初始化流式响应
      this.streamedResponses.set(messageId, '');
      
      try {
        // 准备请求数据
        const requestData = {
          sessionId: this.currentChat.id,
          message: userMessage,
          modelType: this.currentModel.toLowerCase()
        };
        
        // 使用初始化流式响应的API
        const response = await initStreamResponse(requestData);
        
        if (!response || response.code !== 200) {
          throw new Error(response?.message || 'Failed to initialize stream');
        }
        
        // 创建一个新的EventSource来接收流式响应
        const baseUrl = process.env.VUE_APP_BASE_API || 'http://localhost:5000';
        const token = localStorage.getItem('token') || '';
        const eventSource = new EventSource(`${baseUrl}/api/chat/stream/${this.currentChat.id}?token=${token}`);
        
        // 设置滚动定时器，在流式响应过程中定期滚动
        const scrollInterval = setInterval(() => {
          if (this.isLoading) {
            this.scrollToBottom();
          } else {
            clearInterval(scrollInterval);
          }
        }, 300);
        
        // 处理消息事件
        eventSource.addEventListener('message', (event) => {
          const chunk = event.data;
          
          // 检查是否是结束标记
          if (chunk === '[DONE]') {
            eventSource.close();
            // 清除滚动定时器
            clearInterval(scrollInterval);
            
            // 流式响应结束，更新消息状态
            this.currentChat.messages[tempMessageIndex].isLoading = false;
            this.currentChat.messages[tempMessageIndex].isStreaming = false;
            
            // 延迟移除高亮效果
            setTimeout(() => {
              this.$set(this.currentChat.messages[tempMessageIndex], 'highlightNew', false);
            }, 2000);
            
            // 最终清理整个消息内容
            const finalContent = this.cleanTextContent(this.currentChat.messages[tempMessageIndex].content);
            
            // 检查是否是图片响应并修正格式
            if (finalContent.includes('"type":"image"')) {
              try {
                // 尝试找到JSON对象的开始和结束位置
                const startIndex = finalContent.indexOf('{');
                const endIndex = finalContent.lastIndexOf('}') + 1;
                if (startIndex >= 0 && endIndex > startIndex) {
                  // 提取JSON部分
                  const jsonPart = finalContent.substring(startIndex, endIndex);
                  // 更新内容为有效的JSON
                  this.currentChat.messages[tempMessageIndex].content = jsonPart;
                }
              } catch (e) {
                console.error('修复JSON格式失败:', e);
              }
            } else {
              // 普通文本内容
              this.currentChat.messages[tempMessageIndex].content = finalContent;
            }
            
            // 如果是新对话，更新标题
            if (this.currentChat.title === "新对话") {
              this.updateChatTitle(this.currentChatIndex);
            }
            
            this.isLoading = false;
            
            // 结束时滚动到底部确保整个回复可见
            this.scrollToBottom();
            
            // 为了处理可能的延迟内容渲染，再次延迟滚动
            setTimeout(() => {
              this.scrollToBottom();
            }, 800);
            
            return;
          }
          
          // 清理当前数据块
          const cleanedChunk = chunk.replace(/\?{2,}/g, '?');
          
          // 累积响应内容
          const currentContent = this.streamedResponses.get(messageId) + cleanedChunk;
          this.streamedResponses.set(messageId, currentContent);
          
          // 更新消息内容
          this.currentChat.messages[tempMessageIndex].content = currentContent;
          
          // 每次接收新的数据块时滚动到底部，跟踪AI回复
          this.scrollToBottom();
        });
        
        // 处理错误事件
        eventSource.addEventListener('error', (event) => {
          console.error('SSE错误:', event);
          eventSource.close();
          
          // 更新消息为错误状态
          this.currentChat.messages[tempMessageIndex].isLoading = false;
          this.currentChat.messages[tempMessageIndex].isStreaming = false;
          this.currentChat.messages[tempMessageIndex].isError = true;
          this.currentChat.messages[tempMessageIndex].content = "获取AI响应失败，请稍后重试";
          
          this.$message.error("获取AI响应失败，请稍后重试");
          this.isLoading = false;
        });
      } catch (error) {
        console.error('发送消息出错:', error);
        
        // 判断是否是超时错误
        const isTimeout = error.message && (
          error.message.includes('timeout') || 
          error.message.includes('exceeded')
        );
        
        // 替换临时消息为错误消息
        this.currentChat.messages[tempMessageIndex].isLoading = false;
        this.currentChat.messages[tempMessageIndex].isStreaming = false;
        this.currentChat.messages[tempMessageIndex].isError = true;
        this.currentChat.messages[tempMessageIndex].content = isTimeout ? 
          "请求超时，AI模型响应时间过长。您可以尝试重新发送或换个简短的问题。" :
          `发送消息失败: ${error.message || '服务器错误'}`;
        
        const errorMsg = isTimeout ? 
          "请求超时，AI模型响应时间过长" : 
          `发送消息出错: ${error.message || '服务器错误'}`;
          
        this.$message({
          type: "error",
          message: errorMsg,
          duration: 5000,
          showClose: true
        });
        
        this.isLoading = false;
      }
    },
    
    // 更新对话标题
    updateChatTitle(index) {
      const chat = this.chatHistoryList[index];
      if (chat && chat.messages.length > 0) {
        const firstUserMessage = chat.messages.find(msg => msg.role === 'user');
        if (firstUserMessage) {
          chat.title = firstUserMessage.content.length > 20 
            ? firstUserMessage.content.substring(0, 20) + '...' 
            : firstUserMessage.content;
        }
      }
    },
    
    async switchChat(index) {
      this.currentChatIndex = index;
      this.currentSessionId = this.chatHistoryList[index].id;
      
      // 滚动到对话历史底部
      this.scrollToBottom();

      // 移动端时，选择对话后自动关闭侧边栏
      if (this.isMobile) {
        this.sidebarActive = false;
      }
    },
    
    async createNewChat() {
      // 动画效果
      const newChatBtn = document.querySelector('.new-chat-btn');
      if (newChatBtn) {
        newChatBtn.classList.add('creating');
        setTimeout(() => {
          newChatBtn.classList.remove('creating');
        }, 500);
      }
      
      try {
        // 显示加载提示
        const loadingInstance = this.$loading({
          lock: true,
          text: '正在创建新对话...',
          spinner: 'el-icon-loading',
          background: 'rgba(255, 255, 255, 0.7)'
        });
        
        // 调用后端API创建新会话
        const response = await createChatSession(this.currentModel.toLowerCase());
        
        loadingInstance.close();
        
        if (response.code === 200) {
          // 创建新对话
          const newChat = {
            id: response.data.id,
        title: "新对话",
        messages: [],
            defaultModelType: response.data.defaultModelType
          };
          
          // 添加到对话列表
          this.chatHistoryList.unshift(newChat);
      this.currentChatIndex = 0;
          this.currentSessionId = newChat.id;
          
          // 成功提示
          this.$message({
            type: 'success',
            message: '新对话创建成功',
            duration: 1500
          });

      // 移动端时，创建新对话后自动关闭侧边栏
      if (this.isMobile) {
        this.sidebarActive = false;
          }
          
          return newChat;
        } else {
          this.$message.error(`创建对话失败: ${response.message}`);
        }
      } catch (error) {
        console.error('创建对话出错:', error);
        this.$message.error(`创建对话出错: ${error.message || '服务器错误'}`);
      }
    },
    
    async handleChatAction(command, index) {
      if (command === "delete") {
        this.$confirm("确定要删除此对话吗?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        })
        .then(async () => {
          try {
            // 调用后端API删除会话
            const sessionId = this.chatHistoryList[index].id;
            const response = await deleteChatSession(sessionId);
            
            if (response.code === 200) {
              // 从列表中移除
              this.chatHistoryList.splice(index, 1);
              
              // 如果删除的是当前对话
              if (index === this.currentChatIndex) {
                if (this.chatHistoryList.length > 0) {
                  // 切换到第一个对话
                  this.currentChatIndex = 0;
                  this.currentSessionId = this.chatHistoryList[0].id;
                } else {
                  // 没有对话了，创建新的
                  await this.createNewChat();
                }
              } else if (index < this.currentChatIndex) {
                // 如果删除的是当前对话之前的对话，调整索引
                this.currentChatIndex--;
              }
              
              this.$message.success("对话已删除");
            } else {
              this.$message.error(`删除失败: ${response.message}`);
            }
          } catch (error) {
            console.error("删除对话出错:", error);
            this.$message.error(
              `删除对话出错: ${error.message || "服务器错误"}`
            );
            }
          })
          .catch(() => {});
      } else if (command === "rename") {
        // 重命名对话
        const chat = this.chatHistoryList[index];
        this.$prompt("请输入新的标题", "重命名对话", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          inputValue: chat.title,
          inputPattern: /\S+/,
          inputErrorMessage: "标题不能为空",
        })
        .then(async ({ value }) => {
          try {
              // 调用后端API重命名会话
              const response = await renameChatSession(chat.id, value);
            
            if (response.code === 200) {
                // 更新标题
                chat.title = value;
                this.$message.success("重命名成功");
            } else {
              this.$message.error(`重命名失败: ${response.message}`);
            }
          } catch (error) {
              console.error("重命名对话出错:", error);
              this.$message.error(
                `重命名对话出错: ${error.message || "服务器错误"}`
              );
          }
          })
          .catch(() => {});
      } else if (command === "share") {
        // 修改为导出功能
        this.exportToMarkdown(index);
      }
    },
    
    toggleSidebar() {
      this.sidebarActive = !this.sidebarActive;
    },
    
    // 处理窗口大小变化
    handleResize() {
      const isMobileView = window.innerWidth <= 767;

      if (isMobileView) {
        // 移动端默认隐藏侧边栏
        this.sidebarActive = false;
      } else {
        // 桌面端默认显示侧边栏
        this.sidebarActive = true;
      }
    },
    
    // 修改共享历史记录方法为导出当前对话
    shareHistory() {
      this.exportCurrentChat();
    },
    
    // 处理文件上传
    handleFileUpload() {
      // 创建文件输入元素
      const fileInput = document.createElement('input');
      fileInput.type = 'file';
      fileInput.accept = '.pdf,.doc,.docx,.xlsx,.ppt,.pptx,.txt';
      fileInput.multiple = false;
      fileInput.style.display = 'none';
      
      // 监听文件选择事件
      fileInput.addEventListener('change', async (event) => {
        const files = event.target.files;
        if (files && files.length > 0) {
          const file = files[0];
          const maxSize = 100 * 1024 * 1024; // 100MB
          
          if (file.size > maxSize) {
            this.$message.warning(`文件"${file.name}"超过100MB限制`);
            document.body.removeChild(fileInput);
        return;
      }
      
          // 检查文件类型是否为支持的文档类型
          const fileExt = file.name.substring(file.name.lastIndexOf('.')).toLowerCase();
          if (!['.txt', '.docx', '.doc'].includes(fileExt)) {
            this.$message.warning(`当前仅支持解析.txt和.docx/.doc文件`);
            document.body.removeChild(fileInput);
            return;
          }
          
          try {
            // 显示上传中提示
            const loadingInstance = this.$loading({
              lock: true,
              text: '解析文档中...',
              spinner: 'el-icon-loading',
              background: 'rgba(255, 255, 255, 0.7)'
            });
            
            // 创建FormData对象
            const formData = new FormData();
            formData.append('file', file);
            
            // 发送请求到后端文档解析API
            const response = await parseDocument(formData);
            
            loadingInstance.close();
            
            if (response.code === 200) {
              const documentData = response.data;
              const documentContent = documentData.content;
              
              // 提示解析成功
              this.$message.success('文档解析成功');
              
              // 只在输入框中显示文件名
              this.userInput = documentData.filename;
              
              // 存储完整文档内容，以便发送给AI
              this.fullDocumentContent = documentContent;
              
              // 显示提示
              if (documentContent.length > 1000) {
                this.$message.info('文档解析成功，发送后AI将分析完整文档内容');
              }
            } else {
              this.$message.error(`文档解析失败: ${response.message}`);
            }
          } catch (error) {
            console.error('文档解析出错:', error);
            this.$message.error(`文档解析出错: ${error.message || '服务器错误'}`);
          }
        }
        
        // 移除元素
        document.body.removeChild(fileInput);
      });
      
      // 添加到文档并触发点击
      document.body.appendChild(fileInput);
      fileInput.click();
    },
    
    // 处理图片上传
    handleImageUpload() {
      // 创建文件输入元素
      const fileInput = document.createElement('input');
      fileInput.type = 'file';
      fileInput.accept = 'image/*';
      fileInput.multiple = false;
      fileInput.style.display = 'none';
      
      // 监听文件选择事件
      fileInput.addEventListener('change', async (event) => {
        const files = event.target.files;
        if (files && files.length > 0) {
          const file = files[0];
          const maxSize = 10 * 1024 * 1024; // 10MB
          
          if (file.size > maxSize) {
            this.$message.warning(`图片"${file.name}"超过10MB限制`);
            document.body.removeChild(fileInput);
        return;
      }
      
          // 检查文件类型是否支持
          const fileType = file.type;
          if (!fileType.startsWith('image/')) {
            this.$message.warning(`文件"${file.name}"不是有效的图片格式`);
            document.body.removeChild(fileInput);
            return;
          }
          
          try {
            // 显示上传中提示
            const loadingInstance = this.$loading({
              lock: true,
              text: '上传图片中...',
              spinner: 'el-icon-loading',
              background: 'rgba(255, 255, 255, 0.7)'
            });
            
            // 创建FormData对象
            const formData = new FormData();
            formData.append('file', file);
            
            // 发送请求到后端上传API
            const response = await uploadFile(formData);
            
            loadingInstance.close();
            
            if (response.code === 200) {
              // 上传成功，获取图片URL
              const imageUrl = response.data.url;
              
              // 将图片添加到输入框
              this.userInput += `\n分析这张图片:\n${imageUrl}`;
              
              this.$message.success('图片上传成功');
            } else {
              console.error('图片上传响应错误:', response);
              this.$message.error(`图片上传失败: ${response.message || '服务器错误'}`);
            }
          } catch (error) {
            console.error('图片上传出错详情:', error);
            let errorMessage = '图片上传出错';
            if (error.response) {
              // 服务器响应错误
              errorMessage += `: 服务器返回 ${error.response.status} - ${error.response.statusText || '未知错误'}`;
              console.error('响应数据:', error.response.data);
            } else if (error.request) {
              // 请求发送但没有收到响应
              errorMessage += ': 服务器无响应，请检查服务器状态';
            } else {
              // 请求配置出错
              errorMessage += `: ${error.message || '未知错误'}`;
            }
            this.$message.error(errorMessage);
          }
        }
        
        // 移除元素
        document.body.removeChild(fileInput);
      });
      
      // 添加到文档并触发点击
      document.body.appendChild(fileInput);
      fileInput.click();
    },
    
    selectPrompt(prompt) {
      this.userInput = prompt;
    },
    toggleInputExpanded() {
      this.inputExpanded = !this.inputExpanded;
      
      // 更新输入框高度
      setTimeout(() => {
        const textareaEl = document.querySelector('.chat-input .el-textarea__inner');
        if (textareaEl) {
          if (this.inputExpanded) {
            textareaEl.style.height = '100px';
          } else {
            textareaEl.style.height = '';
          }
        }
      }, 0);
    },
    
    // 保存偏好设置
    savePreferences(preferences) {
      console.log('保存偏好设置:', preferences);
      // 应用字体设置
      if (preferences.font.family) {
        document.body.style.fontFamily = preferences.font.family;
      }
      document.body.style.fontSize = `${preferences.font.size}px`;
      
      // 应用其他设置
      this.currentModel = preferences.defaultModel;
    },
    
    changeModel(modelName) {
      this.currentModel = modelName;
      this.$message.success(`已切换至 ${modelName} 模型`);
    },
    
    // 检查是否是图片消息
    isImageMessage(content) {
      if (!content) return false;
      
      try {
        // 尝试解析为JSON
        if (content.trim().startsWith('{') && content.includes('"type":"image"')) {
          const jsonData = JSON.parse(content);
          return jsonData.type === 'image' && jsonData.url;
        }
        
        // 兼容旧格式：检查是否包含图片类型和URL指示符
        return content.includes('"type":"image"') && content.includes('"url"');
      } catch (e) {
        console.error('检查图片消息出错:', e);
        return false;
      }
    },
    
    // 简化提取图片URL方法
    extractImageUrl(content) {
      if (!content) return '';
      
      try {
        // 如果是完整的JSON对象，直接解析
        if (content.trim().startsWith('{')) {
          const jsonData = JSON.parse(content);
          if (jsonData.type === 'image' && jsonData.url) {
            return jsonData.url;
          }
        }
        
        // 兼容旧格式：使用正则表达式提取URL
        const urlMatch = /"url":"([^"]+)"/.exec(content);
        if (urlMatch && urlMatch[1]) {
          return urlMatch[1];
        }
      } catch (e) {
        console.error('提取图片URL失败:', e);
      }
      
      return '';
    },
    
    // 下载图片方法
    downloadImage(url) {
      fetch(url)
        .then(response => response.blob())
        .then(blob => {
          const objUrl = window.URL.createObjectURL(blob);
          const link = document.createElement('a');
          link.href = objUrl;
          link.download = 'AI生成图片.jpg';
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
          window.URL.revokeObjectURL(objUrl);
        })
        .catch(error => {
          console.error('下载图片失败:', error);
          this.$message.error('下载图片失败，请重试');
          // 如果下载失败，尝试直接打开图片
          window.open(url, '_blank');
        });
    },
    
    // 点赞图片方法
    likeImage(message) {
      // 切换点赞状态
      this.$set(message, 'liked', !message.liked);
      
      // 显示点赞反馈
      if (message.liked) {
        this.$message.success('感谢您的点赞！');
        
        // 如果需要保存到后端，可以在这里调用相关API
        console.log('保存图片点赞记录:', this.extractImageUrl(message.content));
        // 例如: this.$http.post('/api/images/like', { imageUrl: this.extractImageUrl(message.content) });
      } else {
        this.$message.info('已取消点赞');
      }
    },
    
    // 提供反馈（点赞或差评）
    provideFeedback(message, type) {
      // 标记已提供反馈，防止重复操作
      this.$set(message, 'feedbackGiven', true);
      this.$set(message, 'feedbackType', type);
      
      // 显示反馈成功消息
      this.$message({
        type: 'success',
        message: type === 'like' ? '感谢您的反馈！' : '感谢您的反馈，我们会继续改进！',
        duration: 2000
      });
    },
    
    // 复制消息内容
    copyMessage(message) {
      navigator.clipboard.writeText(message.content)
        .then(() => {
          this.$message.success('已复制到剪贴板');
        })
        .catch(err => {
          console.error('复制失败:', err);
          this.$message.error('复制失败，请重试');
        });
    },
    
    // 删除消息
    deleteMessage(index) {
      this.$confirm('确定要删除这条消息吗?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 删除消息
        this.currentChat.messages.splice(index, 1);
        this.$message.success('消息已删除');
      }).catch(() => {});
    },
    
    // 重新生成消息
    regenerateMessage(index) {
      // 获取前一个用户消息
      let userMessage = '';
      for (let i = index - 1; i >= 0; i--) {
        if (this.currentChat.messages[i].role === 'user') {
          userMessage = this.currentChat.messages[i].content;
          break;
        }
      }

      if (!userMessage) {
        this.$message.warning('找不到用户消息，无法重新生成');
        return;
      }

      // 删除当前AI消息
      this.currentChat.messages.splice(index, 1);

      // 设置用户输入并发送
      this.userInput = userMessage;
      this.$nextTick(() => {
        this.sendMessage();
      });
    },
    
    // 启动语音输入
    startVoiceInput() {
      // 检查浏览器是否支持语音识别API
      if (!('webkitSpeechRecognition' in window) && !('SpeechRecognition' in window)) {
        this.$message.error('您的浏览器不支持语音识别功能，请使用Chrome浏览器');
        return;
      }
      
      try {
        // 创建语音识别实例
        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
        const recognition = new SpeechRecognition();
        
        // 保存this引用
        const that = this;
        
        // 创建modal对话框
        const modalHtml = `
          <div class="voice-modal-mask" style="position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.5);z-index:9999;display:flex;align-items:center;justify-content:center;">
            <div class="voice-modal-content" style="background:white;width:90%;max-width:400px;border-radius:8px;box-shadow:0 2px 12px rgba(0,0,0,0.1);overflow:hidden;">
              <div style="padding:20px;">
                <div style="text-align:center;margin-bottom:20px;">
                  <div class="recording-icon" style="width:60px;height:60px;border-radius:50%;background:#f56c6c;margin:0 auto 16px;position:relative;animation:pulse 1.5s infinite alternate;">
                    <div class="volume-indicator" style="position:absolute;top:50%;left:50%;transform:translate(-50%, -50%);width:40px;height:40px;display:flex;justify-content:center;align-items:center;gap:1px;">
                      <div class="volume-bar" style="width:3px;height:5px;background:white;border-radius:1px;"></div>
                      <div class="volume-bar" style="width:3px;height:5px;background:white;border-radius:1px;"></div>
                      <div class="volume-bar" style="width:3px;height:5px;background:white;border-radius:1px;"></div>
                      <div class="volume-bar" style="width:3px;height:5px;background:white;border-radius:1px;"></div>
                      <div class="volume-bar" style="width:3px;height:5px;background:white;border-radius:1px;"></div>
                    </div>
                  </div>
                  <div style="font-size:18px;font-weight:500;margin-bottom:15px;">正在录音，请说话...</div>
                  <div id="voiceResult" style="background:#f5f7fa;border-radius:4px;padding:10px;min-height:40px;margin-bottom:15px;text-align:left;word-break:break-all;"></div>
                  <div style="color:#606266;font-size:13px;text-align:left;margin-bottom:15px;">
                    <div>如果没有识别到您的声音，请尝试：</div>
                    <ul style="padding-left:20px;margin-top:5px;">
                      <li>确保麦克风已授权并工作正常</li>
                      <li>靠近麦克风，提高音量并清晰说话</li>
                      <li>减少背景噪音</li>
                    </ul>
                  </div>
                </div>
                <div style="display:flex;justify-content:center;gap:15px;">
                  <button id="voiceFinishBtn" style="background:#409EFF;border:none;color:white;padding:8px 20px;border-radius:4px;cursor:pointer;font-size:14px;">完成</button>
                  <button id="voiceCancelBtn" style="background:#f56c6c;border:none;color:white;padding:8px 20px;border-radius:4px;cursor:pointer;font-size:14px;">取消</button>
                </div>
              </div>
            </div>
          </div>
        `;

        // 添加样式
        const styleElem = document.createElement('style');
        styleElem.textContent = `
          @keyframes pulse {
            0% { transform: scale(1); }
            100% { transform: scale(1.1); }
          }
        `;
        document.head.appendChild(styleElem);

        // 添加modal到DOM
        const modalElem = document.createElement('div');
        modalElem.id = 'voiceRecognitionModal';
        modalElem.innerHTML = modalHtml;
        document.body.appendChild(modalElem);

        // 配置语音识别
        recognition.continuous = true; // 设置为持续识别
        recognition.interimResults = true;
        recognition.maxAlternatives = 1;
        recognition.lang = 'zh-CN';
        
        // 音频上下文和分析器，用于获取音量
        let audioContext;
        let analyser;
        let microphone;
        let javascriptNode;
        
        // 尝试设置音频上下文
        try {
          window.AudioContext = window.AudioContext || window.webkitAudioContext;
          audioContext = new AudioContext();
          
          // 请求麦克风访问
          navigator.mediaDevices.getUserMedia({ audio: true, video: false })
            .then(function(stream) {
              // 创建媒体源
            microphone = audioContext.createMediaStreamSource(stream);
            
              // 创建分析器
              analyser = audioContext.createAnalyser();
            analyser.fftSize = 256;
              
              // 创建处理节点
              javascriptNode = audioContext.createScriptProcessor(1024, 1, 1);
              
              // 连接节点
              microphone.connect(analyser);
              analyser.connect(javascriptNode);
              javascriptNode.connect(audioContext.destination);
              
              // 设置音量条
              const volumeBars = document.querySelectorAll('.volume-bar');
              
              // 处理音频数据
              javascriptNode.onaudioprocess = function() {
                const array = new Uint8Array(analyser.frequencyBinCount);
                analyser.getByteFrequencyData(array);
                
                // 计算平均音量
                let values = 0;
                for (let i = 0; i < array.length; i++) {
                  values += array[i];
                }
                
                const average = values / array.length;
                
                // 更新音量可视化
                for (let i = 0; i < 5; i++) { // 现在只有5个音量条
                  const threshold = (i + 1) * 10; // 调整阈值
                  if (average >= threshold) {
                    volumeBars[i].style.height = Math.min(20, 5 + average / 10) + 'px'; // 动态高度
                    volumeBars[i].style.backgroundColor = 'white';
                  } else {
                    volumeBars[i].style.height = '5px'; // 非活跃状态
                    volumeBars[i].style.backgroundColor = 'rgba(255, 255, 255, 0.5)';
                  }
                }
              };
            })
            .catch(function(err) {
              console.error('获取麦克风失败: ', err);
            });
        } catch (e) {
          console.error('创建音频上下文失败: ', e);
        }
        
        // 结果变量
        let finalTranscript = '';
        let recognitionActive = true;
        
        // 获取按钮和结果显示元素
        const finishBtn = document.getElementById('voiceFinishBtn');
        const cancelBtn = document.getElementById('voiceCancelBtn');
        const resultDisplay = document.getElementById('voiceResult');
        
        // 清理函数
        const cleanupRecognition = () => {
          if (modalElem && modalElem.parentNode) {
            document.body.removeChild(modalElem);
          }
          if (styleElem && styleElem.parentNode) {
            document.head.removeChild(styleElem);
          }
          if (recognitionActive) {
            try {
              recognition.stop();
            } catch (e) {
              console.error('停止语音识别出错:', e);
            }
          }
          
          // 清理音频上下文
          if (javascriptNode) {
            javascriptNode.disconnect();
          }
          if (analyser) {
            analyser.disconnect();
          }
          if (microphone) {
            microphone.disconnect();
          }
          if (audioContext && audioContext.state !== 'closed') {
            audioContext.close();
          }
        };
        
        // 语音识别事件
        recognition.onstart = function() {
          console.log('开始语音识别');
          recognitionActive = true;
        };
        
        recognition.onresult = function(event) {
          let interimTranscript = '';
          
          // 获取最新的识别结果
          for (let i = event.resultIndex; i < event.results.length; i++) {
            const transcript = event.results[i][0].transcript;
            if (event.results[i].isFinal) {
              finalTranscript += transcript + ' ';
          } else {
              interimTranscript += transcript;
            }
          }
          
          // 显示结果
          if (resultDisplay) {
            resultDisplay.textContent = finalTranscript + interimTranscript;
          }
        };
        
        recognition.onerror = function(event) {
          console.error('语音识别错误:', event.error);
          
          // 自定义错误处理
          let errorMsg = '';
          switch(event.error) {
            case 'network':
              errorMsg = '网络连接错误，请检查您的网络连接';
              break;
            case 'not-allowed':
              errorMsg = '麦克风权限被拒绝，请在浏览器中允许访问麦克风';
              break;
            case 'no-speech':
              // 对于持续模式，no-speech不是错误
              return;
            default:
              errorMsg = `语音识别错误: ${event.error}`;
          }
          
          that.$message.error(errorMsg);
          
          // 不因错误而停止识别，让用户决定何时结束
          if (event.error === 'network' || event.error === 'not-allowed') {
            recognitionActive = false;
            cleanupRecognition();
          }
        };
        
        recognition.onend = function() {
          console.log('语音识别结束');
          
          // 如果是意外结束且用户未主动停止，尝试重新启动
          if (recognitionActive) {
            console.log('尝试重新启动语音识别');
            try {
              recognition.start();
            } catch (e) {
              console.error('重新启动语音识别失败:', e);
              recognitionActive = false;
              cleanupRecognition();
            }
          } else {
            // 用户主动结束，处理最终结果
            if (finalTranscript) {
              that.userInput = finalTranscript.trim();
              that.$message.success('语音识别完成');
            }
          }
        };

        // 绑定按钮事件 - 使用onclick而不是addEventListener
        if (finishBtn) {
          finishBtn.onclick = function() {
            console.log('点击完成按钮');
            recognitionActive = false; // 标记用户主动结束
            recognition.stop();
            cleanupRecognition();
          };
        }
        
        if (cancelBtn) {
          cancelBtn.onclick = function() {
            console.log('点击取消按钮');
            finalTranscript = ''; // 清空结果
            recognitionActive = false; // 标记用户主动结束
            recognition.stop();
            cleanupRecognition();
          };
        }
        
        // 启动语音识别
        recognition.start();
        
      } catch (error) {
        console.error('初始化语音识别失败:', error);
        this.$message.error('启动语音识别失败: ' + error.message);
      }
    },
    
    // 添加滚动事件处理函数
    handleScroll() {
      const chatHistory = this.$refs.chatHistory;
      if (!chatHistory) return;
      
      // 判断是否用户手动滚动到了非底部位置
      const isAtBottom = chatHistory.scrollHeight - chatHistory.scrollTop <= chatHistory.clientHeight + 100;
      
      if (!isAtBottom) {
        this.userScrolled = true;
        
        // 设置延迟重置，当用户停止操作一段时间后恢复自动滚动
        if (this.scrollTimer) clearTimeout(this.scrollTimer);
        this.scrollTimer = setTimeout(() => {
          this.userScrolled = false;
          // 如果此时还在加载，则滚动到底部
          if (this.isLoading) {
            this.scrollToBottom();
          }
        }, 5000); // 5秒后恢复自动滚动
      } else {
        this.userScrolled = false;
      }
    },

    // 改进滚动逻辑
    scrollToBottom() {
      // 如果用户手动滚动到了非底部位置且消息仍在加载中，则添加一个滚动提示
      if (this.userScrolled && this.isLoading) {
        this.showScrollHint();
        return;
      }
      
      this.$nextTick(() => {
        const chatHistory = this.$refs.chatHistory;
        if (chatHistory) {
          // 直接设置滚动位置，不使用平滑滚动以确保立即滚动到底部
          chatHistory.scrollTop = chatHistory.scrollHeight;
          
          // 为移动设备添加额外处理
          if (this.isMobile) {
            // 移动设备上可能需要强制布局重新计算
            chatHistory.style.overflow = 'hidden';
            chatHistory.offsetHeight; // 触发重新计算
            chatHistory.style.overflow = 'auto';
            chatHistory.scrollTop = chatHistory.scrollHeight;
          }
          
          // 添加额外的延迟滚动，以处理内容可能需要时间渲染的情况
          setTimeout(() => {
            if (chatHistory) {
              chatHistory.scrollTop = chatHistory.scrollHeight;
              
              // 再添加一个延迟检查，确保内容完全加载后仍然在底部
              setTimeout(() => {
                if (chatHistory) {
                  chatHistory.scrollTop = chatHistory.scrollHeight;
                }
              }, 500);
            }
          }, 100);
        }
      });
    },
    
    // 显示滚动提示
    showScrollHint() {
      // 不需要做任何事情，通过computed属性自动显示滚动提示按钮
    },

    // 提取文件名
    extractFileName(content) {
      if (!content || !content.startsWith('已解析文件:')) return '';
      
      // 从内容中提取文件名
      const match = content.match(/已解析文件:\s*(.*?)(?:\s*\n|\s*$)/);
      if (match && match[1]) {
        return match[1].trim();
      }
      return '未知文件';
    },
    
    // 强制滚动到底部（用户点击滚动提示按钮时）
    forceScrollToBottom() {
      this.userScrolled = false;
      this.scrollToBottom();
    },
    
    // 检查是否是用户上传的图片消息
    isUserImageMessage(content) {
      if (!content) return false;
      return content.includes('分析这张图片:') && content.includes('http');
    },
    
    // 从用户消息中提取图片URL
    extractUserImageUrl(content) {
      if (!content) return '';
      const match = content.match(/http[s]?:\/\/[^\s]+/);
      return match ? match[0] : '';
    },
    
    // 导出当前对话的方法
    async exportCurrentChat() {
      try {
        // 直接使用当前对话数据
        if (!this.currentChat || !this.currentChat.messages || this.currentChat.messages.length === 0) {
          this.$message.error('无法导出：当前对话为空或无效');
          return;
        }

        console.log('开始导出当前对话...');
        
        // 准备导出内容
        let content = `# ${this.currentChat.title || '对话记录'}\n\n`;
        content += `导出时间：${new Date().toLocaleString()}\n\n`;
        
        // 添加消息内容
        for (const message of this.currentChat.messages) {
          try {
            const role = message.role === 'assistant' ? 'AI助手' : '用户';
            // 使用安全的方式获取时间
            let timeString = new Date().toLocaleString();
            try {
              if (message.createdAt) {
                timeString = new Date(message.createdAt).toLocaleString();
              }
            } catch (timeError) {
              console.warn('解析消息时间出错:', timeError);
            }
            
            content += `## ${role} (${timeString})\n\n`;
            
            if (message.role === 'assistant' && this.isImageMessage(message.content)) {
              const imageUrl = this.extractImageUrl(message.content);
              if (imageUrl) {
                content += `![AI生成的图片](${imageUrl})\n\n`;
              } else {
                content += `${message.content}\n\n`;
              }
            } else if (message.content && this.isUserImageMessage(message.content)) {
              const imageUrl = this.extractUserImageUrl(message.content);
              if (imageUrl) {
                content += `![用户上传的图片](${imageUrl})\n\n`;
              } else {
                content += `${message.content}\n\n`;
              }
            } else {
              // 移除HTML标签，保留纯文本
              const plainText = message.content ? message.content.replace(/<[^>]+>/g, '') : '';
              content += `${plainText}\n\n`;
            }
          } catch (messageError) {
            console.error('处理消息时出错:', messageError, message);
            content += `[消息处理错误]\n\n`;
          }
        }
        
        console.log('内容准备完成，准备创建文件...');
        
        // 创建Blob对象
        const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
        
        // 生成文件名
        const fileName = `${this.currentChat.title || '对话记录'}_${new Date().toISOString().slice(0,10)}.md`;
        
        console.log('开始下载文件:', fileName);
        
        // 下载文件
        saveAs(blob, fileName);
        
        this.$message.success('对话导出成功');
      } catch (error) {
        console.error('导出对话失败详情:', error);
        this.$message.error(`导出对话失败: ${error.message || '未知错误'}`);
      }
    },
    
    // 保留原来的方法以兼容其他地方的调用
    async exportToMarkdown(index) {
      try {
        // 如果是当前对话，使用新方法
        if (index === this.currentChatIndex) {
          return this.exportCurrentChat();
        }
        
        const chat = this.chatHistoryList[index];
        if (!chat || !chat.messages) {
          this.$message.error('无法导出：对话数据无效');
          return;
        }

        // 准备导出内容
        let content = `# ${chat.title || '对话记录'}\n\n`;
        content += `导出时间：${new Date().toLocaleString()}\n\n`;
        
        // 添加消息内容
        for (const message of chat.messages) {
          const role = message.role === 'assistant' ? 'AI助手' : '用户';
          let timeString = new Date().toLocaleString();
          try {
            if (message.createdAt) {
              timeString = new Date(message.createdAt).toLocaleString();
            }
          } catch (e) {
            console.warn('解析消息时间出错:', e);
          }
          
          content += `## ${role} (${timeString})\n\n`;
          
          if (message.role === 'assistant' && this.isImageMessage(message.content)) {
            const imageUrl = this.extractImageUrl(message.content);
            if (imageUrl) {
              content += `![AI生成的图片](${imageUrl})\n\n`;
            }
          } else if (this.isUserImageMessage(message.content)) {
            const imageUrl = this.extractUserImageUrl(message.content);
            if (imageUrl) {
              content += `![用户上传的图片](${imageUrl})\n\n`;
            }
          } else {
            // 移除HTML标签，保留纯文本
            const plainText = message.content ? message.content.replace(/<[^>]+>/g, '') : '';
            content += `${plainText}\n\n`;
          }
        }
        
        // 创建Blob对象
        const blob = new Blob([content], { type: 'text/markdown;charset=utf-8' });
        
        // 生成文件名
        const fileName = `${chat.title || '对话记录'}_${new Date().toISOString().slice(0,10)}.md`;
        
        // 下载文件
        saveAs(blob, fileName);
        
        this.$message.success('对话导出成功');
      } catch (error) {
        console.error('导出对话失败:', error);
        this.$message.error(`导出对话失败: ${error.message || '未知错误'}`);
      }
    },
  },
  created() {
    // 加载用户会话
    this.loadUserSessions();
    
    // 监听窗口大小变化
    window.addEventListener("resize", this.handleResize);

    // 初始化侧边栏状态
    this.handleResize();
    
    // 添加消息容器变化监听
    this.$nextTick(() => {
      // 使用MutationObserver监听消息列表的变化
      const chatMessages = this.$el.querySelector('.chat-messages');
      if (chatMessages) {
        this.observer = new MutationObserver(() => {
          if (!this.userScrolled) {
            this.scrollToBottom();
          }
        });
        
        this.observer.observe(chatMessages, {
          childList: true,
          subtree: true,
          characterData: true
        });
      }
      
      // 监听滚动事件，检测用户是否手动滚动
      const chatHistory = this.$refs.chatHistory;
      if (chatHistory) {
        chatHistory.addEventListener('scroll', this.handleScroll);
      }
    });
  },
  beforeDestroy() {
    // 移除事件监听
    window.removeEventListener("resize", this.handleResize);
    
    // 移除MutationObserver
    if (this.observer) {
      this.observer.disconnect();
    }
    
    // 移除滚动事件监听
    const chatHistory = this.$refs.chatHistory;
    if (chatHistory) {
      chatHistory.removeEventListener('scroll', this.handleScroll);
    }
  },
};
</script>

<style scoped>
.chat-history {
  flex: 1;
  width: 100%;
  max-width: 100%;
  padding: 16px;
  margin: 0;
  box-sizing: border-box;
  overflow-y: auto;
  min-height: 0;
  -webkit-overflow-scrolling: touch;
  background-color: #fafafa;
  position: relative;
  display: flex;
  flex-direction: column;
}

.content-area {
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  position: relative;
  flex: 1;
}

.app-container {
  width: 100vw;
  max-width: 100vw;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  height: 100vh;
  display: flex;
  overflow: hidden;
  position: relative;
}

.main-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%;
  position: relative;
}

.greeting-section {
  text-align: center;
  margin-bottom: 24px;
  flex: 1 0 auto;
}

.greeting-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.logo-image {
  width: 60px;
  height: 60px;
  margin-right: 15px;
}

.greeting-title {
  font-size: 28px;
  font-weight: 500;
  color: #000000;
  margin-bottom: 16px;
}

.greeting-subtitle {
  font-size: 16px;
  color: #666666;
  margin: 0;
  line-height: 1.5;
}

.prompt-section {
  margin: 0 auto;
  width: 100%;
  max-width: 800px;
  box-sizing: border-box;
  flex: 1 0 auto;
}

.prompt-title {
  font-size: 15px;
  color: #999;
  margin-bottom: 16px;
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  margin-bottom: 24px;
}

.prompt-item {
  background-color: #f5f5f5;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 15px;
  color: #333;
  cursor: pointer;
  transition: background-color 0.2s;
}

.prompt-item:hover {
  background-color: #e8e8e8;
}

/* 聊天消息样式 */
.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 16px;
  width: 100%;
  max-width: 100%;
  box-sizing: border-box;
  flex: 1 0 auto;
}

.message {
  display: flex;
  max-width: 85%;
  margin-bottom: 20px;
  align-items: flex-start;
}

.user-message {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.assistant-message {
  align-self: flex-start;
  animation: fadeIn 0.5s ease-out;
}

@keyframes fadeIn {
  from {
    opacity: 0.5;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.assistant-message.highlight-new {
  box-shadow: 0 0 8px rgba(103, 194, 58, 0.6);
  animation: pulse 2s ease-out;
}

@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0.4);
  }
  70% {
    box-shadow: 0 0 0 10px rgba(103, 194, 58, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(103, 194, 58, 0);
  }
}

.message-bubble {
  border-radius: 12px;
  padding: 10px 16px;
  max-width: 100%;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.1);
}

.user-message .message-bubble {
  background-color: #ecf5ff;
  color: #333;
  margin-right: 10px;
}

.assistant-message .message-bubble {
  background-color: #f5f5f5;
  color: #333;
  margin-left: 10px;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.avatar-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-message-avatar {
  width: 100%;
  height: 100%;
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
}

.message-header {
  margin-bottom: 6px;
}

.model-info {
  font-size: 12px;
  color: #666;
}

.model-name {
  margin-right: 8px;
  font-weight: 500;
}

.response-time {
  color: #999;
}

.message-content {
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.message-actions {
  display: flex;
  gap: 15px;
  margin-top: 8px;
  justify-content: flex-end;
}

.message-actions i {
  font-size: 16px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.message-actions i:hover {
  color: #409EFF;
}

/* 输入区域 */
.input-container {
  padding: 0;
  background-color: #fff;
  border-top: 1px solid #eaeaea;
  flex-shrink: 0;
  z-index: 2;
  position: sticky;
  bottom: 0;
  width: 100%;
}

.input-expander {
  text-align: center;
  padding: 2px 0;
  cursor: pointer;
  color: #999;
  border-bottom: 1px solid #eaeaea;
  background-color: #f5f5f5;
}

.input-expander:hover {
  background-color: #e8e8e8;
  color: #409EFF;
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  position: relative;
  transition: all 0.3s ease;
  padding: 10px 16px;
}

.input-area.expanded {
  min-height: 120px;
}

.chat-input {
  flex: 1;
}

.el-dropdown-link {
  cursor: pointer;
  color: #409EFF;
  display: flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  margin-right: 8px;
  border: 1px solid #DCDFE6;
  transition: all 0.3s;
}

.el-dropdown-link:hover {
  background-color: #ecf5ff;
  border-color: #c6e2ff;
}

.model-dropdown {
  margin-right: 8px;
}

.user-info {
  display: flex;
  justify-content: flex-end;
  color: #909399;
  font-size: 12px;
  margin-bottom: 4px;
}

.send-time {
  color: #909399;
}

.input-actions {
  display: flex;
  align-items: center;
  margin: 0 8px;
  padding-right: 15px;
}

.input-actions .el-button {
  border: none;
  background: transparent;
  color: #666;
  margin-left: 8px;
  width: 32px;
  height: 32px;
}

.input-actions .el-button:hover {
  background-color: #e8e8e8;
}

.send-button {
  border-radius: 8px;
  padding: 8px;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.send-button.is-disabled {
  background-color: #ccc;
  border-color: #ccc;
}

/* 移动端适配 */
@media screen and (max-width: 767px) {
  .greeting-title {
    font-size: 24px;
    margin-bottom: 12px;
  }

  .greeting-subtitle {
    font-size: 15px;
    padding: 0;
  }

  .prompt-section {
    padding: 0;
  }

  .prompt-title {
    font-size: 14px;
    margin-bottom: 12px;
  }

  .prompt-item {
    padding: 10px 14px;
    font-size: 14px;
    border-radius: 6px;
  }

  .input-container {
    padding: 8px 12px;
  }

  .input-area {
    padding: 6px 8px;
    border-radius: 6px;
  }

  .chat-input :deep(.el-textarea__inner) {
    font-size: 15px;
    padding: 4px 0;
  }

  .input-actions .el-button {
    font-size: 18px;
    width: 30px;
    height: 30px;
    margin-left: 4px;
  }

  .send-button {
    width: 30px;
    height: 30px;
    padding: 6px;
  }

  .greeting-header {
    flex-direction: column;
  }
  
  .logo-image {
    margin-right: 0;
    margin-bottom: 10px;
  }
}

/* 隐藏旧的样式 */
.message-info {
  display: none;
}

/* 历史项样式 */
.history-item {
  padding: 8px 16px;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background-color 0.2s;
}

.history-item:hover {
  background-color: #f0f0f0;
}

.history-item.active {
  background-color: #e6f4ff;
}

.history-item-content {
  display: flex;
  align-items: center;
  flex: 1;
  overflow: hidden;
}

.history-item-content i {
  margin-right: 8px;
  color: #666;
}

.history-title {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 13px;
  color: #333;
}

.history-item .el-icon-more {
  color: #aaa;
  font-size: 16px;
  cursor: pointer;
  padding: 4px;
  visibility: hidden;
}

.history-item:hover .el-icon-more {
  visibility: visible;
}

.model-name-display {
  margin-right: 12px;
  cursor: pointer;
}

.dropdown-trigger {
  display: flex;
  align-items: center;
  padding: 4px 8px;
  border-radius: 4px;
  background-color: #f5f5f5;
  transition: all 0.3s;
}

.dropdown-trigger:hover {
  background-color: #e8e8e8;
}

.dropdown-trigger i.el-icon-cpu {
  margin-right: 4px;
  color: #409eff;
}

.dropdown-trigger i.el-icon-arrow-down {
  margin-left: 4px;
  font-size: 12px;
  color: #909399;
}

/* 图片相关样式 */
.ai-image-message {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.ai-generated-image {
  max-width: 256px;
  max-height: 256px;
  width: auto;
  height: auto;
  object-fit: contain;
  border-radius: 8px;
  margin-top: 10px;
  margin-bottom: 10px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.image-actions {
  margin-top: 5px;
  margin-bottom: 5px;
}

.image-feedback {
  display: flex;
  justify-content: center;
  gap: 10px;
  margin-top: 10px;
}

.feedback-given {
  color: #67c23a;
  font-size: 14px;
}

.file-parse-message {
  display: flex;
  align-items: center;
  background-color: #f0f9eb;
  padding: 8px 12px;
  border-radius: 4px;
  color: #606266;
}

.file-parse-message i {
  margin-right: 8px;
  color: #67c23a;
  font-size: 16px;
}

/* 滚动提示按钮样式 */
.scroll-to-bottom-indicator {
  position: absolute;
  bottom: 20px;
  right: 20px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: #409EFF;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.15);
  z-index: 100;
  animation: bounce 1s infinite alternate;
  opacity: 0.9;
  transition: opacity 0.3s;
}

.scroll-to-bottom-indicator:hover {
  opacity: 1;
}

@keyframes bounce {
  from {
    transform: translateY(0px);
  }
  to {
    transform: translateY(-5px);
  }
}

/* 录音指示器样式 */
.recording-indicator {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.recording-content {
  background-color: white;
  border-radius: 8px;
  padding: 20px;
  width: 80%;
  max-width: 400px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.recording-icon {
  width: 60px;
  height: 60px;
  background-color: #f56c6c;
  border-radius: 50%;
  margin: 0 auto 16px;
  position: relative;
  animation: pulse-recording 1.5s infinite alternate;
}

.recording-icon:before {
  content: '';
  width: 20px;
  height: 30px;
  background-color: white;
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  border-radius: 4px;
}

.recording-text {
  font-size: 18px;
  font-weight: 500;
  margin-bottom: 16px;
  color: #303133;
}

.interim-text {
  font-size: 14px;
  color: #606266;
  min-height: 40px;
  margin-bottom: 16px;
  word-break: break-word;
  max-height: 100px;
  overflow-y: auto;
  background-color: #f5f7fa;
  padding: 8px;
  border-radius: 4px;
}

.recording-buttons {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 16px;
}

.done-button {
  border: none;
  background-color: #409EFF;
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.done-button:hover {
  background-color: #66b1ff;
}

.cancel-button {
  border: none;
  background-color: #f56c6c;
  color: white;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  transition: background-color 0.3s;
}

.cancel-button:hover {
  background-color: #e64242;
}

@keyframes pulse-recording {
  0% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.1);
    opacity: 0.8;
  }
  100% {
    transform: scale(1);
    opacity: 1;
  }
}

.like-button.liked {
  background-color: #f56c6c;
  border-color: #f56c6c;
  color: white;
}

.like-button.liked i {
  color: white;
}

/* 用户上传的图片样式 */
.user-image-message {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.user-uploaded-image {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.image-description {
  font-size: 14px;
  color: #606266;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 10px;
}
</style> 