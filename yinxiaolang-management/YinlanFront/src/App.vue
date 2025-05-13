<template>
  <div id="app">
    <template v-if="$route.path === '/login'">
      <router-view />
    </template>
    <template v-else>
      <el-container class="main-container">
        <el-main :class="{ 'no-padding': $route.path === '/' }">
          <router-view />
        </el-main>
      </el-container>
    </template>
    <AppAccountModal :visible.sync="isAccountModalVisible" />
  </div>
</template>

<script>
import AppAccountModal from "./components/AccountModal.vue";
import { mapActions, mapGetters } from "vuex";

export default {
  name: "App",
  components: {
    AppAccountModal,
  },
  computed: {
    ...mapGetters(["isAccountModalVisible", "isAdmin", "currentUser"]),
    isMobile() {
      return window.innerWidth <= 767;
    },
  },
  data() {
    return {
      darkThemeEnabled: false,
    };
  },
  methods: {
    ...mapActions(["toggleAccountModal", "logout"]),
    handleCommand(command) {
      if (command === "logout") {
        this.logout();
        this.$router.push("/login");
      } else if (command === "admin") {
        if (this.$route.path.includes("/admin")) {
          this.$router.push("/");
        } else {
          this.$router.push("/admin");
        }
      }
    },
  },
};
</script>

<style>
:root {
  --primary-color: #409eff;
  --background-color: #f5f7fa;
  --text-color: #303133;
  --border-color: #e0e0e0;
  --hover-color: #ecf5ff;
  --font-size-small: 13px;
  --font-size-base: 14px;
  --font-size-medium: 16px;
  --font-size-large: 18px;
  --radius-small: 4px;
  --radius-base: 8px;
  --shadow-light: 0 2px 12px rgba(0, 0, 0, 0.1);
  --transition-base: all 0.3s ease;
}

@media screen and (max-width: 767px) {
  :root {
    --font-size-small: 12px;
    --font-size-base: 14px;
    --font-size-medium: 15px;
    --font-size-large: 16px;
    --radius-small: 3px;
    --radius-base: 6px;
  }
}

body {
  margin: 0;
  padding: 0;
  height: 100vh;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto,
    "Helvetica Neue", Arial, sans-serif;
  font-size: var(--font-size-base);
  color: var(--text-color);
  -webkit-tap-highlight-color: transparent;
}

html {
  overflow: hidden;
}

#app {
  height: 100vh;
  overflow: hidden;
}

.el-container {
  height: 100vh;
}

.main-container {
  background-color: #fff;
  height: 100vh;
  display: flex;
  flex-direction: column;
}

.el-main {
  flex: 1;
  padding: 0;
  overflow-x: hidden;
  overflow-y: auto;
}

.no-padding {
  padding: 0 !important;
}

.user-avatar {
  background-color: #409eff;
  color: white;
  font-weight: bold;
  cursor: pointer;
  transition: transform 0.2s;
}

.user-avatar:hover {
  transform: scale(1.05);
}

.el-dropdown-menu__item i {
  margin-right: 8px;
}

#app {
  font-family: "Helvetica Neue", Helvetica, "PingFang SC", "Hiragino Sans GB",
    "Microsoft YaHei", "微软雅黑", Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
  -moz-osx-font-smoothing: grayscale;
  color: #2c3e50;
  height: 100%;
}

html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  width: 100%;
  overscroll-behavior: none;
}

/* Element UI 样式优化 */
.el-input__inner {
  transition: var(--transition-base);
}

.el-button {
  font-weight: 400;
}

.el-message {
  min-width: 240px;
  box-shadow: var(--shadow-light);
}

/* 移动端适配全局样式 */
@media screen and (max-width: 767px) {
  /* 防止缩放和自动放大 */
  input,
  select,
  textarea {
    font-size: 16px !important;
  }

  /* 优化移动端触摸体验 */
  button,
  [role="button"] {
    min-height: 44px;
    min-width: 44px;
  }

  /* 移除点击延迟 */
  * {
    touch-action: manipulation;
  }

  /* 提高对比度 */
  .el-button--primary {
    background-color: var(--primary-color) !important;
    border-color: var(--primary-color) !important;
  }

  /* 优化表单元素 */
  .el-input__inner,
  .el-textarea__inner {
    font-size: var(--font-size-medium) !important;
    line-height: 1.5;
  }

  /* 触摸区域优化 */
  .el-button,
  .el-dropdown,
  .el-menu-item,
  .el-checkbox,
  .el-radio {
    padding: 10px 15px;
  }

  /* 对话框适配 */
  .el-message-box {
    width: 90% !important;
    max-width: 320px;
  }

  /* 下拉菜单适配 */
  .el-dropdown-menu__item {
    line-height: 36px;
    padding: 0 15px;
  }
}
</style>
