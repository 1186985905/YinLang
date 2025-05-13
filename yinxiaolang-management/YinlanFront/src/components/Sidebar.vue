<template>
  <div class="app-sidebar" :class="{ 'sidebar-collapsed': isCollapsed }">
    <!-- 顶部区域 -->
    <div class="sidebar-top">
      <!-- 收起/展开按钮 -->
      <el-button
        type="text"
        icon="el-icon-menu"
        @click="toggleSidebar"
        class="collapse-btn"
      ></el-button>

      <!-- 发起新对话按钮 -->
      <el-button
        type="primary"
        icon="el-icon-document-add"
        class="new-chat-btn"
        :class="{ 'new-chat-btn-collapsed': isCollapsed }"
      >
        <span v-show="!isCollapsed">发起新对话</span>
      </el-button>
    </div>

    <!-- 菜单区域 -->
    <el-menu
      class="sidebar-menu"
      default-active="recent-0"
      :collapse="isCollapsed"
      :collapse-transition="false"
    >
      <!-- 近期对话区域 -->
      <div class="menu-section">
        <div class="section-title" v-show="!isCollapsed">近期对话</div>
        <el-menu-item
          v-for="(item, idx) in recentChats"
          :key="`recent-${idx}`"
          :index="`recent-${idx}`"
          @click="navigateToHome"
        >
          <i class="el-icon-chat-dot-round menu-icon"></i>
          <span slot="title" class="menu-item-text">{{ item.title }}</span>
        </el-menu-item>
      </div>

      <!-- 添加分割线 -->
      <el-divider class="sidebar-divider" v-show="!isCollapsed"></el-divider>

      <!-- 底部区域 -->
      <div class="sidebar-bottom">
        <div class="location-info" v-show="!isCollapsed">
          <p>娄底市</p>
          <a href="#">根据 IP 地址确定 • 更新位置信息</a>
        </div>
      </div>
    </el-menu>
  </div>
</template>

<script>
export default {
  name: "AppSidebar",
  data() {
    return {
      isCollapsed: true, // 设置默认为收起状态
      recentChats: [
        { title: "美国公布核弹头数量" },
        { title: "国际法院敦促以色列结束非法占领" },
        { title: "国内外经济动态" },
        { title: "陕西柞水桥梁垮塌事故" },
        { title: "四川汉源山洪灾害" },
        { title: "商务部对美进口丙酸征收反倾销税" },
        { title: "天津无证导游强制购物被罚" },
        { title: "特朗普与泽连斯基通话承诺结束俄乌冲突" },
        { title: "今日热点新闻" },
      ],
    };
  },
  methods: {
    toggleSidebar() {
      this.isCollapsed = !this.isCollapsed;
      this.$emit("toggled", this.isCollapsed);
    },
    navigateToHome() {
      // 返回主页
      this.$router.push("/");
    },
  },
};
</script>

<style scoped>
.app-sidebar {
  width: 280px;
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #f8f9fa;
  transition: width 0.3s ease;
  border-right: 1px solid #e9ecef;
  position: relative;
  z-index: 1000;
}

.app-sidebar.sidebar-collapsed {
  width: 48px;
  background-color: #f8f9fa;
}

/* 顶部区域样式 */
.sidebar-top {
  display: flex;
  padding: 12px 12px 6px 12px;
  align-items: center;
  flex-shrink: 0;
  border-bottom: 1px solid #e9ecef;
  margin-bottom: 8px;
  background-color: #fff;
}

.app-sidebar.sidebar-collapsed .sidebar-top {
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px 6px;
}

/* 收起/展开按钮样式 */
.collapse-btn {
  font-size: 16px;
  color: #5f6368;
  padding: 6px;
  margin-right: 6px;
  border: none;
}

.app-sidebar.sidebar-collapsed .collapse-btn {
  margin-right: 0;
  margin-bottom: 12px;
}

/* 发起新对话按钮样式 */
.new-chat-btn {
  flex-grow: 1;
  border-radius: 4px;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  color: #333;
  font-weight: normal;
  font-size: 13px;
  height: 32px;
  transition: all 0.3s ease;
  overflow: hidden;
  white-space: nowrap;
  display: flex;
  align-items: center;
  justify-content: center;
}

.new-chat-btn-collapsed {
  width: 32px;
  min-width: 32px;
  padding: 0;
  flex-grow: unset;
}

.new-chat-btn:hover {
  background-color: #f5f7fa;
  border-color: #dcdfe6;
}

/* 菜单区域样式 */
.sidebar-menu {
  flex-grow: 1;
  border-right: none;
  background-color: transparent;
  overflow-y: auto;
  overflow-x: hidden;
  padding: 0;
  margin-top: 0;
  position: relative;
  display: flex;
  flex-direction: column;
  height: calc(100% - 60px);
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.el-menu--collapse {
  width: 100%;
}

.menu-section {
  flex: 1;
  margin-bottom: 0;
  overflow-y: auto;
}

.section-title {
  font-size: 12px;
  color: #999;
  padding: 6px 12px;
  white-space: nowrap;
  overflow: hidden;
}

.sidebar-divider {
  margin: 8px 0 16px 0;
}

/* 菜单项样式 */
.el-menu-item {
  height: 40px !important;
  line-height: 40px !important;
  font-size: 13px;
  color: #333;
  border-radius: 0;
  margin-bottom: 0;
  display: flex;
  align-items: center;
  padding: 0 12px !important;
  white-space: nowrap;
  overflow: hidden;
  background-color: transparent;
}

.el-menu--collapse .el-menu-item {
  padding: 0 !important;
  justify-content: center;
}

.el-menu-item:hover {
  background-color: #f0f2f5;
}

.el-menu-item.is-active {
  background-color: #e6f4ff;
  color: #1890ff;
}

.menu-icon {
  margin-right: 12px;
  font-size: 16px;
  width: 24px;
  text-align: center;
  color: #666;
  vertical-align: middle;
}

.el-menu--collapse .menu-icon {
  margin-right: 0;
  margin: 0 auto;
}

.menu-item-text {
  flex-grow: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: middle;
}

/* 底部区域样式 */
.sidebar-bottom {
  padding: 12px;
  border-top: 1px solid #e9ecef;
  background-color: #fff;
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 10;
}

.location-info {
  font-size: 12px;
  color: #999;
  padding: 0;
  margin: 0;
}

.location-info p {
  margin: 0 0 4px 0;
  color: #666;
  font-size: 13px;
}

.location-info a {
  color: #666;
  text-decoration: none;
  font-size: 11px;
  display: block;
  line-height: 1.4;
}

.location-info a:hover {
  color: #1890ff;
}

/* 修复菜单收起状态的样式 */
.el-menu--collapse {
  border-right: none !important;
}

.app-sidebar.sidebar-collapsed .el-menu-item {
  padding: 0 !important;
}

.app-sidebar.sidebar-collapsed .menu-icon {
  margin: 0 auto !important;
  width: 100% !important;
  text-align: center !important;
}

.app-sidebar.sidebar-collapsed .sidebar-bottom {
  padding: 12px 0;
  display: flex;
  justify-content: center;
  align-items: center;
}

.app-sidebar.sidebar-collapsed .location-info p {
  text-align: center;
  margin: 0;
}

/* 帮助菜单样式 */
::v-deep .help-popover {
  padding: 0;
  border-radius: 8px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
  background-color: #2a2a2a;
  border: 1px solid #3c3c3c;
}

.help-menu {
  display: flex;
  flex-direction: column;
  color: #e8eaed;
}

.help-menu-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.help-menu-item:hover {
  background-color: #3c3c3c;
}

.help-menu-item i {
  margin-right: 12px;
  font-size: 18px;
  width: 20px;
  text-align: center;
  color: #a0a0a0;
}

.help-menu-item span {
  flex: 1;
  font-size: 14px;
}

.help-badge {
  position: absolute;
  right: 16px;
}

::v-deep .help-badge .el-badge__content.is-dot {
  background-color: #4285f4;
  border: none;
  right: 0;
}

/* 设置菜单样式 */
::v-deep .settings-popover {
  padding: 0;
  border-radius: 8px;
  box-shadow: 0 3px 12px rgba(0, 0, 0, 0.15);
  background-color: #2a2a2a;
  border: 1px solid #3c3c3c;
}

.settings-menu {
  display: flex;
  flex-direction: column;
  color: #e8eaed;
}

.settings-menu-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  position: relative;
}

.settings-menu-item:hover {
  background-color: #3c3c3c;
}

.settings-menu-item i {
  margin-right: 12px;
  font-size: 18px;
  width: 20px;
  text-align: center;
  color: #a0a0a0;
}

.settings-menu-item span {
  flex: 1;
  font-size: 14px;
}

.settings-badge {
  position: absolute;
  right: 16px;
}

::v-deep .settings-badge .el-badge__content.is-dot {
  background-color: #4285f4;
  border: none;
  right: 0;
}

.theme-switch {
  margin-left: auto;
}

/* 移动端适配 */
@media screen and (max-width: 767px) {
  .app-sidebar {
    position: fixed;
    left: 0;
    top: 0;
    z-index: 2000;
    box-shadow: 2px 0 8px rgba(0, 0, 0, 0.15);
  }

  .app-sidebar.sidebar-collapsed {
    width: 48px;
    transform: translateX(0);
    transition: transform 0.3s ease, width 0.3s ease;
  }

  .mobile-hidden {
    transform: translateX(-100%);
  }

  .sidebar-top {
    padding: 10px 8px;
  }

  .menu-section {
    max-height: calc(100vh - 150px);
  }

  .sidebar-overlay {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.4);
    z-index: 1500;
    display: none;
  }

  .sidebar-overlay.visible {
    display: block;
  }

  .collapse-btn {
    font-size: 18px;
  }

  .new-chat-btn {
    height: 36px;
  }

  .el-menu-item {
    font-size: 14px;
  }
}
</style> 