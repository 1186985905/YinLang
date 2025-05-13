<template>
  <el-dialog 
    :visible.sync="localVisible" 
    width="500px"
    class="account-modal"
    :show-close="true"
    append-to-body
    title="个人信息"
    custom-class="account-dialog-custom"
  >
    <div v-if="!isEditing" class="user-profile-info">
      <el-avatar :size="80" class="user-avatar">{{ userInitial }}</el-avatar>
      <h3 class="username">{{ currentUser.username || '未登录' }}</h3>
      
      <div class="info-items">
        <div class="info-item">
          <span class="info-label">密码：</span>
          <span class="info-value">••••••••</span>
        </div>
        <div class="info-item">
          <span class="info-label">部门：</span>
          <span class="info-value">{{ currentUser.department || '未分配' }}</span>
        </div>
        <div class="info-item">
          <span class="info-label">权限：</span>
          <span class="info-value">
            <el-tag type="primary" size="small" v-if="isAdmin">超级管理员</el-tag>
            <el-tag size="small" v-else>普通用户</el-tag>
          </span>
        </div>
        <div class="info-item">
          <span class="info-label">创建时间：</span>
          <span class="info-value">{{ currentUser.createdAt || '2024-01-01 12:00:00' }}</span>
        </div>
      </div>

      <div class="account-actions">
        <el-button type="primary" icon="el-icon-edit" @click="startEditing">编辑信息</el-button>
        <el-button type="danger" icon="el-icon-switch-button" @click="logout">退出登录</el-button>
      </div>
    </div>

    <!-- 编辑表单 -->
    <el-form 
      v-else 
      :model="userForm" 
      :rules="rules" 
      ref="userForm" 
      label-width="80px" 
      class="edit-form"
    >
      <el-form-item label="用户名" prop="username">
        <el-input v-model="userForm.username"></el-input>
      </el-form-item>
      <el-form-item label="密码" prop="password">
        <el-input type="password" v-model="userForm.password" placeholder="留空表示不修改" show-password></el-input>
      </el-form-item>
      <el-form-item label="确认密码" prop="confirmPassword" v-if="userForm.password">
        <el-input type="password" v-model="userForm.confirmPassword" show-password></el-input>
      </el-form-item>
      <el-form-item label="部门" prop="department">
        <el-select v-model="userForm.department" placeholder="请选择部门">
          <el-option
            v-for="dept in departments"
            :key="dept.id"
            :label="dept.name"
            :value="dept.id"
          ></el-option>
        </el-select>
      </el-form-item>
      
      <div class="form-actions">
        <el-button @click="cancelEdit">取消</el-button>
        <el-button type="primary" @click="saveUserInfo">保存</el-button>
      </div>
    </el-form>
  </el-dialog>
</template>

<script>
import { mapGetters, mapActions } from 'vuex';

export default {
  name: 'AccountModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    // 密码确认验证函数
    const validatePass = (rule, value, callback) => {
      if (value !== this.userForm.password) {
        callback(new Error('两次输入密码不一致!'));
      } else {
        callback();
      }
    };
    
    return {
      localVisible: this.visible,
      isEditing: false,
      departments: [
        { id: 1, name: '技术部' },
        { id: 2, name: '市场部' },
        { id: 3, name: '运营部' },
        { id: 4, name: '产品部' },
        { id: 5, name: '客服部' }
      ],
      userForm: {
        username: '',
        password: '',
        confirmPassword: '',
        department: null
      },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { min: 6, message: '密码长度不能少于6个字符', trigger: 'blur' }
        ],
        confirmPassword: [
          { validator: validatePass, trigger: 'blur' }
        ],
        department: [
          { required: true, message: '请选择部门', trigger: 'change' }
        ]
      }
    }
  },
  computed: {
    ...mapGetters([
      'currentUser',
      'isAdmin'
    ]),
    userInitial() {
      return this.currentUser && this.currentUser.username
        ? this.currentUser.username.charAt(0).toUpperCase()
        : '?';
    }
  },
  watch: {
    visible(val) {
      this.localVisible = val;
      if (val) {
        this.isEditing = false;
      }
    },
    localVisible(val) {
      this.$emit('update:visible', val);
      if (!val) {
        this.isEditing = false;
      }
    },
    currentUser: {
      handler(newVal) {
        if (newVal) {
          this.userForm.username = newVal.username || '';
          this.userForm.department = newVal.departmentId || null;
        }
      },
      immediate: true
    }
  },
  methods: {
    ...mapActions(['logout', 'updateUserProfile']),
    startEditing() {
      // 重置表单为当前用户信息
      this.userForm = {
        username: this.currentUser.username || '',
        password: '',
        confirmPassword: '',
        department: this.currentUser.departmentId || null
      };
      this.isEditing = true;
    },
    cancelEdit() {
      this.isEditing = false;
    },
    saveUserInfo() {
      this.$refs.userForm.validate(valid => {
        if (valid) {
          // 在实际应用中，这里应该调用API保存用户信息
          const updatedUser = {
            ...this.currentUser,
            username: this.userForm.username,
            departmentId: this.userForm.department
          };
          
          // 如果有设置新密码
          if (this.userForm.password) {
            updatedUser.password = this.userForm.password;
          }
          
          // 更新用户信息
          this.updateUserProfile(updatedUser)
            .then(() => {
              this.$message.success('个人信息更新成功');
              this.isEditing = false;
            })
            .catch(err => {
              this.$message.error('更新失败: ' + err.message);
            });
        } else {
          return false;
        }
      });
    }
  }
}
</script>

<style>
.account-dialog-custom {
  border-radius: 8px !important;
  box-shadow: 0 4px 20px rgba(0,0,0,0.15) !important;
}

.account-dialog-custom .el-dialog__header {
  padding: 20px 20px 10px;
  text-align: center;
}

.account-dialog-custom .el-dialog__body {
  padding: 0 20px 20px;
}
</style>

<style scoped>
.user-profile-info {
  text-align: center;
  padding: 10px 0 20px;
}

.user-avatar {
  background-color: #409eff;
  color: white;
  font-weight: bold;
  font-size: 30px;
  margin-bottom: 15px;
}

.username {
  font-size: 20px;
  font-weight: 500;
  margin: 0 0 20px;
  color: #303133;
}

.info-items {
  text-align: left;
  margin: 20px auto;
  max-width: 350px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
}

.info-item {
  display: flex;
  padding: 12px 20px;
  border-bottom: 1px solid #ebeef5;
}

.info-item:last-child {
  border-bottom: none;
}

.info-label {
  flex: 0 0 80px;
  color: #909399;
  font-weight: 500;
}

.info-value {
  flex: 1;
  color: #303133;
}

.account-actions {
  display: flex;
  justify-content: center;
  gap: 15px;
  margin-top: 25px;
}

.edit-form {
  padding: 10px 20px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
  gap: 10px;
}
</style> 