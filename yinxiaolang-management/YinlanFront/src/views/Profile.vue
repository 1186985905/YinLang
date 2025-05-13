<template>
  <div class="profile-container">
    <div class="profile-card">
      <div class="profile-header">
        <div class="header-left">
          <el-button icon="el-icon-back" size="small" @click="goToHome" class="back-btn">返回主页</el-button>
          <h2>个人信息</h2>
        </div>
      </div>

      <div class="profile-avatar-section">
        <el-avatar :size="80" class="user-avatar">{{ userInitial }}</el-avatar>
      </div>

      <el-divider></el-divider>

      <div class="profile-info">
        <div class="info-item">
          <span class="label">用户名</span>
          <span class="value">{{ userProfile.username }}</span>
        </div>

        <div class="info-item">
          <span class="label">密码</span>
          <div class="password-field">
            <span class="value">{{ showPassword ? userProfile.password : '********' }}</span>
            <el-button 
              type="text"
              icon="el-icon-view" 
              v-if="showPassword"
              @click="togglePassword"
              class="password-toggle">
            </el-button>
            <el-button 
              type="text"
              icon="el-icon-view" 
              v-else
              @click="togglePassword"
              class="password-toggle">
            </el-button>
          </div>
        </div>

        <div class="info-item">
          <span class="label">用户类型</span>
          <span class="value">{{ userProfile.userType }}</span>
        </div>

        <div class="info-item">
          <span class="label">部门</span>
          <span class="value">{{ userProfile.department }}</span>
        </div>

        <div class="info-item">
          <span class="label">手机号码</span>
          <span class="value">{{ userProfile.phone || '未设置' }}</span>
        </div>

        <div class="info-item">
          <span class="label">创建时间</span>
          <span class="value">{{ formatDate(userProfile.createdAt) }}</span>
        </div>
        
        <div class="info-item">
          <span class="label">上次登录时间</span>
          <span class="value">{{ formatDate(userProfile.lastLoginAt) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapGetters } from 'vuex';

export default {
  name: 'ProfileView',
  data() {
    return {
      showPassword: false,
      userProfile: {
        username: 'admin',
        password: 'admin123',
        userType: '管理员',
        department: '生产部',
        phone: '15367437726',
        createdAt: new Date('2024-01-15'),
        lastLoginAt: new Date('2024-06-10 08:35:24')
      }
    };
  },
  computed: {
    ...mapGetters(['currentUser']),
    userInitial() {
      return this.userProfile.username ? this.userProfile.username.charAt(0).toUpperCase() : 'A';
    },
    maskPassword() {
      return '********';
    }
  },
  created() {
    // 这里应该从后端获取用户信息
    // this.fetchUserProfile();
  },
  methods: {
    goToHome() {
      this.$router.push('/');
    },
    formatDate(date) {
      if (!date) return '';
      const d = new Date(date);
      return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
    },
    fetchUserProfile() {
      // 从后端获取用户信息
      // this.$axios.get('/api/user/profile').then(response => {
      //   this.userProfile = response.data;
      // }).catch(error => {
      //   this.$message.error('获取用户信息失败');
      // });
    },
    togglePassword() {
      this.showPassword = !this.showPassword;
    }
  }
};
</script>

<style scoped>
.profile-container {
  display: flex;
  justify-content: center;
  align-items: flex-start;
  padding: 24px;
  min-height: calc(100vh - 60px);
  background-color: #f5f7fa;
}

.profile-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0,0,0,0.1);
  width: 100%;
  max-width: 600px;
  padding: 24px;
}

.profile-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-left h2 {
  margin: 0 0 0 12px;
  color: #303133;
}

.back-btn {
  padding: 6px 10px;
}

.profile-avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin: 16px 0;
}

.user-avatar {
  background-color: #409eff;
  color: white;
  font-weight: bold;
  margin-bottom: 12px;
}

.info-item {
  display: flex;
  margin-bottom: 16px;
}

.label {
  flex-basis: 120px;
  color: #606266;
  font-weight: 500;
}

.value {
  flex: 1;
  color: #303133;
}

.password-field {
  display: flex;
  align-items: center;
  flex: 1;
}

.password-toggle {
  color: #409EFF;
  cursor: pointer;
  margin-left: 10px;
  padding: 2px 5px;
}

.password-toggle:hover {
  color: #66b1ff;
}

@media screen and (max-width: 767px) {
  .profile-container {
    padding: 16px;
  }
  
  .profile-card {
    padding: 16px;
  }
  
  .info-item {
    flex-direction: column;
  }
  
  .label {
    margin-bottom: 4px;
  }
}
</style> 