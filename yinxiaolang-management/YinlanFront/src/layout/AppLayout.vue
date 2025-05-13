<template>
  <div class="app-container">
    <!-- 左侧历史对话列表 -->
    <div class="chat-sidebar" :class="{ active: sidebarActive }">
      <div class="sidebar-header">
        <span class="sidebar-title">{{ sidebarTitle }}</span>
        <el-button type="text" icon="el-icon-plus" @click="createNewChat"
          >{{ newButtonText }}</el-button
        >
      </div>
      <div class="chat-history-list">
        <slot name="sidebar-content"></slot>
      </div>
    </div>

    <!-- 侧边栏遮罩层 - 仅在移动端显示 -->
    <div
      class="sidebar-overlay"
      v-if="isMobile && sidebarActive"
      @click="toggleSidebar"
    ></div>

    <!-- 主要内容区域 -->
    <div class="main-content">
      <!-- 顶部操作栏 -->
      <div class="top-bar">
        <div class="left-section">
          <el-button
            icon="el-icon-s-fold"
            class="sidebar-toggle-btn"
            @click="toggleSidebar"
          ></el-button>
        </div>
        <div class="user-actions">
          <el-button type="text" icon="el-icon-download" class="share-btn" @click="$emit('share')">导出</el-button>
          <el-dropdown trigger="click" @command="handleCommand">
            <el-avatar size="small" class="user-avatar">
              {{ userInitial }}
            </el-avatar>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item command="profile">
                <i class="el-icon-user"></i> 个人信息
              </el-dropdown-item>
              <el-dropdown-item command="preferences">
                <i class="el-icon-setting"></i> 偏好设置
              </el-dropdown-item>
              <el-dropdown-item v-if="isAdmin" command="admin">
                <i class="el-icon-s-tools"></i> 管理面板
              </el-dropdown-item>
              <el-dropdown-item command="logout">
                <i class="el-icon-switch-button"></i> 退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区域 -->
      <div class="content-area">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from "vuex";

export default {
  name: "AppLayout",
  props: {
    sidebarActive: {
      type: Boolean,
      default: true
    },
    activeTab: {
      type: String,
      required: true,
      validator: value => ['chat', 'text', 'image'].includes(value)
    },
    sidebarTitle: {
      type: String,
      default: "历史对话"
    },
    newButtonText: {
      type: String,
      default: "新对话"
    }
  },
  computed: {
    ...mapGetters([
      "currentUser",
      "isAdmin",
    ]),
    userInitial() {
      return this.currentUser
        ? this.currentUser.username.charAt(0).toUpperCase()
        : "?";
    },
    isMobile() {
      return window.innerWidth <= 767;
    }
  },
  methods: {
    toggleSidebar() {
      this.$emit('toggle-sidebar');
    },
    createNewChat() {
      this.$emit('create-new-chat');
    },
    handleCommand(command) {
      this.$emit('command', command);
    }
  }
}
</script>

<style scoped>
.app-container {
  width: 100vw;
  max-width: 100vw;
  margin: 0;
  padding: 0;
  box-sizing: border-box;
  height: 100%;
  display: flex;
  overflow: hidden;
  position: relative;
}

/* 左侧聊天历史列表 */
.chat-sidebar {
  width: 240px;
  min-width: 240px;
  border-right: 1px solid #eaeaea;
  background-color: #f9f9f9;
  display: flex;
  flex-direction: column;
  height: 100%;
  transition: transform 0.3s ease, width 0.3s ease, min-width 0.3s ease;
  overflow: hidden;
  z-index: 10;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
}

.sidebar-header {
  padding: 12px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid #eaeaea;
}

.sidebar-title {
  font-weight: 500;
  color: #333;
  font-size: 15px;
}

.chat-history-list {
  flex: 1;
  overflow-y: auto;
  padding: 8px 0;
  max-height: calc(100vh - 60px);
  scrollbar-width: thin;
}

/* 主内容区域 */
.main-content {
  flex: 1;
  min-width: 0;
  width: auto;
  margin: 0;
  margin-left: 240px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: 100%;
  overflow: hidden;
  background-color: #fff;
  transition: margin-left 0.3s ease, width 0.3s ease;
}

/* 顶部操作栏 */
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background-color: #fff;
  border-bottom: 1px solid #f0f0f0;
  z-index: 10;
  height: 50px;
}

.left-section {
  display: flex;
  align-items: center;
}

.user-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.user-avatar {
  background-color: #409eff;
  color: white;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.share-btn {
  margin-right: 8px;
  display: flex;
  align-items: center;
}

.share-btn i {
  margin-right: 4px;
}

.sidebar-toggle-btn {
  margin-right: 10px;
  padding: 6px;
  border: none;
  background: transparent;
}

.sidebar-toggle-btn:hover {
  color: #409eff;
  background-color: rgba(64, 158, 255, 0.1);
}

.content-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  position: relative;
  width: 100%;
}

/* 确保页面内容一致性 */
::v-deep .chat-history {
  flex: 1;
  overflow-y: auto;
  padding: 24px 16px;
  background-color: #fafafa;
  scroll-behavior: smooth;
}

::v-deep .input-container {
  padding: 12px 16px;
  background-color: #fff;
  border-top: 1px solid #eaeaea;
  width: 100%;
  box-sizing: border-box;
}

::v-deep .input-area {
  display: flex;
  align-items: flex-end;
  gap: 12px;
  position: relative;
  width: 100%;
}

/* 侧边栏遮罩层样式 */
.sidebar-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 9;
}

/* 移动端适配 */
@media screen and (max-width: 767px) {
  .chat-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    bottom: 0;
    transform: translateX(-100%);
    width: 80%;
    max-width: 240px;
  }

  .chat-sidebar.active {
    transform: translateX(0);
  }

  .main-content {
    margin-left: 0;
  }

  .top-bar {
    padding: 6px 12px;
  }

  .share-btn {
    display: none;
  }
}

@media screen and (min-width: 768px) {
  /* 侧边栏折叠状态 */
  .app-container:not(.mobile) .chat-sidebar:not(.active) {
    width: 0;
    min-width: 0;
    border-right: none;
  }

  /* 侧边栏展开状态 */
  .app-container:not(.mobile) .chat-sidebar.active {
    width: 240px;
    min-width: 240px;
  }

  /* 侧边栏折叠状态下主内容区域无边距 */
  .app-container:not(.mobile) .chat-sidebar:not(.active) + .main-content {
    margin-left: 0;
    width: 100%;
  }

  /* 侧边栏展开状态下主内容区域有边距，但没有留白 */
  .app-container:not(.mobile) .chat-sidebar.active + .main-content {
    margin-left: 240px;
    width: calc(100% - 240px);
  }
}
</style> 