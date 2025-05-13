<template>
  <div>
    <app-layout
      :sidebar-active="sidebarActive"
      active-tab="image"
      sidebar-title="历史记录"
      new-button-text="新对话"
      @toggle-sidebar="toggleSidebar"
      @create-new-chat="createNewChat"
      @command="handleCommand"
      @share="shareHistory"
    >
      <template v-slot:sidebar-content>
        <div
          v-for="(chat, index) in imageHistoryList"
          :key="index"
          class="history-item"
          :class="{ active: currentChatIndex === index }"
          @click="switchChat(index)"
        >
          <div class="history-item-content">
            <i class="el-icon-picture-outline"></i>
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
              <el-dropdown-item command="share">共享</el-dropdown-item>
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
            你身边的智能助手，在这里你可以创建各种精美图片，快来体验吧~
          </p>
        </div>

        <!-- 提示词区域 -->
        <div class="prompt-section" v-if="currentChat.messages.length === 0">
          <div class="prompt-title">你可以尝试这些风格</div>
          <div class="prompt-list">
            <div
              class="prompt-item"
              @click="selectPrompt('一个美丽的日落场景，湖边有小船，水彩风格')"
            >
              一个美丽的日落场景，湖边有小船，水彩风格
            </div>
            <div
              class="prompt-item"
              @click="selectPrompt('未来城市夜景，赛博朋克风格，霓虹灯闪烁')"
            >
              未来城市夜景，赛博朋克风格，霓虹灯闪烁
            </div>
            <div
              class="prompt-item"
              @click="
                selectPrompt('一只可爱的猫咪，卡通风格，坐在窗台上')
              "
            >
              一只可爱的猫咪，卡通风格，坐在窗台上
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
            ]"
          >
            <div class="message-avatar" v-if="message.role === 'assistant'">
              <img src="../assets/ai.png" alt="AI" class="avatar-image" />
            </div>
            <div class="message-avatar user-avatar-container" v-else>
              <div class="user-message-avatar">{{ userInitial }}</div>
            </div>
            <div class="message-bubble">
              <div class="message-header" v-if="message.role === 'assistant'">
                <div class="model-info">
                  <span class="model-name">{{ message.model }}</span>
                  <span class="response-time">{{ message.time }}</span>
                </div>
              </div>
              <div class="message-header" v-else>
                <div class="user-info">
                  <span class="send-time">{{ message.time || new Date().toLocaleString('zh-CN', { hour12: false }) }}</span>
                </div>
              </div>
              <div class="message-content" v-if="message.role === 'user'">{{ message.content }}</div>
              <div class="image-content" v-if="message.role === 'assistant'">
                <img :src="message.imageUrl" alt="Generated Image" class="generated-image" />
              </div>
              <div class="message-actions" v-if="message.role === 'assistant'">
                <i class="el-icon-top" title="点赞" @click="likeImage(message)"></i>
                <i class="el-icon-download" title="下载" @click="downloadImage(message)"></i>
                <i class="el-icon-delete" title="删除"></i>
                <i class="el-icon-refresh" title="重新生成"></i>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部输入区域 -->
      <div class="input-container">
        <div class="input-expander" @click="toggleInputExpanded">
          <i :class="inputExpanded ? 'el-icon-caret-bottom' : 'el-icon-caret-top'"></i>
        </div>
        <div class="image-options">
          <el-dropdown trigger="click" @command="handleModelChange">
            <span class="el-dropdown-link">
              <i class="el-icon-cpu"></i>
              {{ currentModel }}
              <i class="el-icon-arrow-down el-icon--right"></i>
            </span>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="Seedream">Seedream</el-dropdown-item>
              <el-dropdown-item command="GPT-4o">GPT-4o</el-dropdown-item>
              <el-dropdown-item command="Imagen 3">Imagen 3</el-dropdown-item>
              <el-dropdown-item command="Midjourney v6.1">Midjourney v6.1</el-dropdown-item>
              <el-dropdown-item command="FLUX 1.1 Pro">FLUX 1.1 Pro</el-dropdown-item>
              <el-dropdown-item command="Ideogram 3.0">Ideogram 3.0</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
          <el-select v-model="selectedStyle" placeholder="选择风格">
            <el-option
              v-for="item in styles"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
          <el-select v-model="selectedRatio" placeholder="比例">
            <el-option
              v-for="item in ratios"
              :key="item.value"
              :label="item.label"
              :value="item.value">
            </el-option>
          </el-select>
        </div>
        <div class="input-area" :class="{ 'expanded': inputExpanded }">
          <el-input
            type="textarea"
            :rows="inputExpanded ? 4 : 2"
            placeholder="描述你想要生成的图片内容"
            v-model="userInput"
            class="chat-input"
            resize="none"
          >
          </el-input>
          <el-button
            type="primary"
            icon="el-icon-picture"
            class="send-button"
            :disabled="!userInput.trim()"
            @click="generateImage"
          >生成</el-button>
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

export default {
  name: "ImageGenerator",
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
      selectedStyle: "",
      selectedRatio: "1:1",
      styles: [
        { value: "水彩", label: "水彩画风格" },
        { value: "赛博朋克", label: "赛博朋克风格" },
        { value: "写实", label: "写实风格" },
        { value: "卡通", label: "卡通风格" }
      ],
      ratios: [
        { value: "1:1", label: "1:1 正方形" },
        { value: "4:3", label: "4:3 横向" },
        { value: "3:4", label: "3:4 纵向" },
        { value: "16:9", label: "16:9 宽屏" }
      ],
      imageHistoryList: [
        {
          id: 1,
          title: "日落风景",
          messages: [
            { role: "user", content: "一个美丽的日落场景，湖边有小船，水彩风格" },
            {
              role: "assistant",
              content: "日落风景图片",
              imageUrl: "https://picsum.photos/600/400",
              model: "GPT-4o",
              time: "2024-06-01 10:30"
            },
          ],
        },
        {
          id: 2,
          title: "未来城市",
          messages: [
            { role: "user", content: "未来城市夜景，赛博朋克风格，霓虹灯闪烁" },
            {
              role: "assistant",
              content: "未来城市图片",
              imageUrl: "https://picsum.photos/600/400?random=1",
              model: "DALL-E 3",
              time: "2024-06-01 11:15"
            },
          ],
        },
        {
          id: 3,
          title: "新对话",
          messages: [],
        },
      ],
      preferencesVisible: false,
      inputExpanded: false,
      shareDialogVisible: false,
      shareLink: "",
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
        this.imageHistoryList[this.currentChatIndex] || {
          id: 0,
          title: "新对话",
          messages: [],
        }
      );
    },
    isMobile() {
      return window.innerWidth <= 767;
    },
  },
  methods: {
    ...mapActions(["setCurrentModel", "logout"]),
    generateImage() {
      if (!this.userInput.trim()) return;

      // 如果是新建的对话且没有标题，使用用户输入的前10个字作为标题
      if (this.currentChat.messages.length === 0) {
        this.currentChat.title =
          this.userInput.substring(0, 10) +
          (this.userInput.length > 10 ? "..." : "");
      }

      // 添加用户消息到聊天历史
      this.currentChat.messages.push({
        role: "user",
        content: this.userInput,
      });

      // 模拟图片生成
      setTimeout(() => {
        this.currentChat.messages.push({
          role: "assistant",
          content: "生成的图片",
          imageUrl: `https://picsum.photos/600/400?random=${Date.now()}`,
          model: this.currentModel,
          time: new Date().toLocaleString('zh-CN', { hour12: false })
        });

        // 滚动到底部
        this.$nextTick(() => {
          const chatHistory = this.$refs.chatHistory;
          if (chatHistory) {
            chatHistory.scrollTop = chatHistory.scrollHeight;
          }
        });
      }, 1500);

      this.userInput = ""; // 清空输入框
    },
    handleModelChange(modelName) {
      this.setCurrentModel(modelName);
    },
    selectPrompt(prompt) {
      this.userInput = prompt;
    },
    handleCommand(command) {
      if (command === "profile") {
        this.$router.push("/profile");
      } else if (command === "logout") {
        this.$store.dispatch("logout");
        this.$router.push("/login");
      } else if (command === "admin") {
        this.$router.push("/admin");
      } else if (command === "preferences") {
        this.preferencesVisible = true;
      }
    },
    switchChat(index) {
      this.currentChatIndex = index;
      // 滚动到对话历史底部
      this.$nextTick(() => {
        const chatHistory = this.$refs.chatHistory;
        if (chatHistory && this.currentChat.messages.length > 0) {
          chatHistory.scrollTop = chatHistory.scrollHeight;
        }
      });

      // 移动端时，选择对话后自动关闭侧边栏
      if (this.isMobile) {
        this.sidebarActive = false;
      }
    },
    createNewChat() {
      this.imageHistoryList.unshift({
        id: Date.now(),
        title: "新对话",
        messages: [],
      });
      this.currentChatIndex = 0;

      // 移动端时，创建新对话后自动关闭侧边栏
      if (this.isMobile) {
        this.sidebarActive = false;
      }
    },
    handleChatAction(cmd, index) {
      if (cmd === "rename") {
        this.$prompt("请输入新的图片生成记录名称", "重命名", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          inputValue: this.imageHistoryList[index].title,
        })
          .then(({ value }) => {
            if (value) {
              this.imageHistoryList[index].title = value;
              this.$message({
                type: "success",
                message: "重命名成功",
              });
            }
          })
          .catch(() => {});
      } else if (cmd === "share") {
        // 修改为导出功能
        this.exportChat(index);
      } else if (cmd === "delete") {
        this.$confirm("确定删除该图片生成记录吗?", "提示", {
          confirmButtonText: "确定",
          cancelButtonText: "取消",
          type: "warning",
        })
          .then(() => {
            this.imageHistoryList.splice(index, 1);
            if (this.imageHistoryList.length === 0) {
              this.imageHistoryList.push({
                id: Date.now(),
                title: "新对话",
                messages: [],
              });
            }
            if (index === this.currentChatIndex) {
              this.currentChatIndex = 0;
            } else if (index < this.currentChatIndex) {
              this.currentChatIndex--;
            }
            this.$message({
              type: "success",
              message: "删除成功!",
            });
          })
          .catch(() => {});
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
    // 导出图片生成记录
    exportChat(index) {
      const chat = this.imageHistoryList[index];
      if (!chat || !chat.messages || chat.messages.length === 0) {
        this.$message.warning('该记录没有内容可导出');
        return;
      }
      
      // 格式化内容为文本
      let content = `# ${chat.title}\n\n`;
      content += `导出时间: ${new Date().toLocaleString('zh-CN')}\n\n`;
      
      // 获取所有图片URL和提示词
      const images = [];
      chat.messages.forEach(msg => {
        if (msg.role === 'user') {
          content += `## 提示词 (${msg.time || '未知时间'})\n\n${msg.content}\n\n`;
        } else if (msg.role === 'assistant' && msg.imageUrl) {
          content += `## 生成图片 - ${msg.model || '未知模型'} (${msg.time || '未知时间'})\n\n图片URL: ${msg.imageUrl}\n\n`;
          images.push({
            url: msg.imageUrl,
            time: msg.time || new Date().toISOString()
          });
        }
      });
      
      // 创建Blob对象
      const blob = new Blob([content], { type: 'text/plain;charset=utf-8' });
      
      // 创建下载链接
      const link = document.createElement('a');
      link.href = URL.createObjectURL(blob);
      link.download = `${chat.title}-${new Date().toISOString().slice(0, 10)}.txt`;
      
      // 触发下载
      document.body.appendChild(link);
      link.click();
      
      // 清理
      document.body.removeChild(link);
      
      // 提示用户可以单独下载图片
      if (images.length > 0) {
        this.$message({
          message: '文本记录已导出。你也可以点击单个图片下方的下载按钮来保存图片。',
          type: 'success',
          duration: 5000
        });
      } else {
        this.$message.success('记录导出成功');
      }
    },
    // 共享图片生成记录
    shareChat(index) {
      const chat = this.imageHistoryList[index];
      if (!chat || !chat.messages || chat.messages.length === 0) {
        this.$message.warning('该记录没有内容可共享');
        return;
      }
      
      // 生成共享链接 (实际应用中这里会调用API生成)
      this.shareLink = `https://example.com/share/${chat.id}`;
      this.shareDialogVisible = true;
    },
    // 导出按钮点击
    shareHistory() {
      this.shareChat(this.currentChatIndex);
    },
    // 保存偏好设置
    savePreferences(preferences) {
      console.log('保存偏好设置:', preferences);
      // 应用字体设置
      if (preferences.font.family) {
        document.body.style.fontFamily = preferences.font.family;
      }
      document.body.style.fontSize = `${preferences.font.size}px`;
      
      // 应用其他设置（如果为图像模型，则使用图像模型的偏好）
      if (preferences.defaultModel.includes('Midjourney') || 
          preferences.defaultModel.includes('DALL-E') || 
          preferences.defaultModel.includes('Stable Diffusion') ||
          preferences.defaultModel.includes('Imagen')) {
        this.currentModel = preferences.defaultModel;
      }
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
    likeImage(message) {
      // 实际项目中这里会调用后端API进行点赞
      this.$message.success('已点赞此图片');
      // 可以在message对象上添加一个liked属性来标记已点赞
      this.$set(message, 'liked', true);
    },
    downloadImage(message) {
      if (!message.imageUrl) return;
      
      // 创建一个临时的a标签并设置下载属性
      const a = document.createElement('a');
      a.href = message.imageUrl;
      a.download = `image-${Date.now()}.jpg`;
      document.body.appendChild(a);
      a.click();
      document.body.removeChild(a);
      
      this.$message.success('图片下载中');
    },
  },
  created() {
    // 监听窗口大小变化
    window.addEventListener("resize", this.handleResize);

    // 初始化侧边栏状态
    this.handleResize();
    
    // 设置默认模型为GPT-4o
    this.setCurrentModel('GPT-4o');
  },
  beforeDestroy() {
    // 移除事件监听
    window.removeEventListener("resize", this.handleResize);
  },
};
</script>

<style scoped>
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

.greeting-section {
  text-align: center;
  margin: 40px auto;
  max-width: 500px;
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
  font-size: 24px;
  color: #333;
  margin-bottom: 12px;
}

.greeting-subtitle {
  font-size: 16px;
  color: #666;
  line-height: 1.5;
}

.prompt-section {
  margin: 40px auto;
  max-width: 700px;
}

.prompt-title {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin-bottom: 16px;
  text-align: center;
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.prompt-item {
  background-color: #f0f0f0;
  padding: 16px;
  border-radius: 8px;
  cursor: pointer;
  color: #333;
  transition: background-color 0.2s;
  border: 1px solid #e0e0e0;
}

.prompt-item:hover {
  background-color: #e0e0e0;
}

.chat-messages {
  display: flex;
  flex-direction: column;
  gap: 20px;
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
  background-color: #fff;
  color: #333;
  margin-left: 10px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
  border: 1px solid #eaeaea;
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

.message-content {
  line-height: 1.5;
  white-space: pre-wrap;
  word-break: break-word;
}

.image-content {
  width: 100%;
}

.generated-image {
  max-width: 100%;
  border-radius: 8px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.1);
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

.image-options {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
  flex-wrap: wrap;
  max-width: 100%;
  padding: 10px 16px 0;
  overflow-x: auto;
}

.el-select {
  max-width: 140px;
}

.input-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  position: relative;
  transition: all 0.3s ease;
  padding: 0 16px 10px;
}

.input-area.expanded {
  min-height: 120px;
}

.chat-input {
  flex: 1;
  width: calc(100% - 100px);
}

.input-actions {
  position: absolute;
  right: 100px;
  bottom: 8px;
  display: flex;
  gap: 8px;
}

.send-button {
  height: 40px;
  width: 80px;
  margin-right: 15px;
}

/* 移动端适配 */
@media screen and (max-width: 767px) {
  .greeting-title {
    font-size: 22px;
    margin-bottom: 10px;
  }

  .greeting-subtitle {
    font-size: 14px;
    padding: 0;
  }

  .prompt-item {
    padding: 12px;
    font-size: 14px;
  }

  .chat-history {
    padding: 16px 12px;
  }

  .message {
    max-width: 90%;
  }

  .image-options {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 8px;
  }

  .el-select {
    width: 120px;
    flex-shrink: 0;
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
.image-message, .image-info {
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

.el-dropdown .el-dropdown-link {
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

.el-dropdown .el-dropdown-link:hover {
  background-color: #ecf5ff;
  border-color: #c6e2ff;
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
</style> 