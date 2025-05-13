<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-title">茵小浪登录</div>
      <el-form
        :model="loginForm"
        :rules="rules"
        ref="loginForm"
        class="login-form"
      >
        <el-form-item prop="username">
          <el-input
            v-model="loginForm.username"
            placeholder="用户名"
            prefix-icon="el-icon-user"
          >
          </el-input>
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="loginForm.password"
            placeholder="密码"
            prefix-icon="el-icon-lock"
            show-password
            @keyup.enter.native="handleLogin"
          >
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            class="login-button"
            :loading="loading"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script>
import { mapActions } from "vuex";
import { login } from '@/api/user';

/* eslint-disable vue/multi-word-component-names */
export default {
  name: "Login",
  data() {
    return {
      loginForm: {
        username: "",
        password: "",
      },
      rules: {
        username: [
          { required: true, message: "请输入用户名", trigger: "blur" },
        ],
        password: [{ required: true, message: "请输入密码", trigger: "blur" }],
      },
      loading: false,
    };
  },
  methods: {
    ...mapActions(["login"]),
    handleLogin() {
      this.$refs.loginForm.validate((valid) => {
        if (valid) {
          this.loading = true;

          // 使用真实API调用
          login(this.loginForm.username, this.loginForm.password)
            .then(response => {
              if (response.code === 200 && response.data) {
                const { token, user } = response.data;
                
                // 保存到Vuex和localStorage
                this.login({ token, user });
                
                // 提示成功
                this.$message.success('登录成功');
                
                // 重定向到首页
                const redirectPath = this.$route.query.redirect || "/";
                this.$router.push(redirectPath);
              } else {
                this.$message.error(response.message || '登录失败');
              }
            })
            .catch(error => {
              console.error('登录失败:', error);
              this.$message.error('登录失败: ' + (error.message || '未知错误'));
            })
            .finally(() => {
              this.loading = false;
            });
        }
      });
    },
  },
};
</script>

<style>
html,
body {
  margin: 0;
  padding: 0;
  height: 100%;
  overflow: hidden;
}
</style>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background-color: #f0f2f5;
}

.login-card {
  width: 400px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  background-color: #fff;
  padding: 20px;
  box-sizing: border-box;
}

.login-title {
  font-size: 24px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 30px;
  color: #409eff;
}

.login-form {
  margin-top: 20px;
}

.login-button {
  width: 100%;
  margin-top: 20px;
}

/* 移动端适配 */
@media screen and (max-width: 480px) {
  .login-card {
    width: 90%;
    margin: 0 auto;
    padding: 16px;
  }

  .login-title {
    font-size: 20px;
    margin-bottom: 20px;
  }

  .login-form :deep(.el-form-item) {
    margin-bottom: 15px;
  }

  .login-button {
    font-size: 16px;
    padding: 10px 0;
    height: 44px; /* 更大的触摸区域 */
  }

  /* 修复输入框在移动端的显示 */
  .login-form :deep(.el-input__inner) {
    height: 40px;
    line-height: 40px;
    font-size: 16px; /* iOS设备上小于16px的字体可能导致缩放问题 */
  }
}
</style>