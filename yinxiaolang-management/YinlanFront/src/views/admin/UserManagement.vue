<template>
  <div class="user-management">
    <h1>
      用户管理
      <div class="header-buttons">
        <el-button type="default" size="small" icon="el-icon-back" @click="goToAdminPanel">返回管理面板</el-button>
        <el-button type="primary" size="small" icon="el-icon-s-home" @click="goToHome">返回主页</el-button>
      </div>
    </h1>

    <!-- 搜索表单 -->
    <div class="search-form">
      <el-form :inline="true" :model="searchForm" class="demo-form-inline">
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable></el-input>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="searchForm.departmentId" placeholder="请选择部门" clearable>
            <el-option v-for="dept in departments" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id">
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="searchForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            format="yyyy-MM-dd HH:mm:ss"
            value-format="yyyy-MM-dd HH:mm:ss"
            clearable>
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="resetSearch">重置</el-button>
          <el-button type="primary" @click="showCreateDialog">+ 创建新用户</el-button>
        </el-form-item>
      </el-form>
    </div>
      
    <!-- 用户列表 -->
    <el-table :data="userList" style="width: 100%" v-loading="loading">
      <el-table-column prop="username" label="用户名"></el-table-column>
      <el-table-column prop="department" label="所属部门">
          <template slot-scope="scope">
            <span v-if="scope.row.department">{{ scope.row.department.name }}</span>
            <span v-else>未分配</span>
          </template>
        </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="180">
        <template slot-scope="scope">
          {{ formatDateTime(scope.row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
          <template slot-scope="scope">
            <el-button
            size="small" 
              type="primary"
              @click="handleEdit(scope.row)">编辑</el-button>
            <el-button
            size="small" 
              type="danger"
              @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

    <!-- 分页 -->
    <div class="pagination">
        <el-pagination
        background
        layout="total, sizes, prev, pager, next, jumper"
        :total="total"
        :page-size="searchForm.pageSize"
        :current-page.sync="searchForm.page"
        :page-sizes="[10, 20, 50]"
          @size-change="handleSizeChange"
        @current-change="handleCurrentChange">
        </el-pagination>
      </div>

    <!-- 创建/编辑用户对话框 -->
    <el-dialog
      :title="dialogType === 'create' ? '创建新用户' : '编辑用户'"
      :visible.sync="dialogVisible"
      width="500px">
      <el-form 
        :model="userForm" 
        :rules="userFormRules" 
        ref="userFormRef"
        label-width="100px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="userForm.username" :disabled="dialogType === 'edit'"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" :rules="dialogType === 'create' ? userFormRules.password : []">
          <el-input v-model="userForm.password" type="password" show-password placeholder="不修改请留空"></el-input>
        </el-form-item>
        <el-form-item label="所属部门" prop="departmentId">
          <el-select v-model="userForm.departmentId" placeholder="请选择部门">
            <el-option v-for="dept in departments" 
              :key="dept.id" 
              :label="dept.name" 
              :value="dept.id">
            </el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>

    <!-- 删除用户确认对话框 -->
    <el-dialog
      title="删除用户"
      :visible.sync="deleteDialogVisible"
      width="400px">
      <div class="delete-confirm">
        <i class="el-icon-warning-outline" style="color: #E6A23C; font-size: 24px; margin-right: 10px;"></i>
        <span>确认删除用户"{{ userToDelete.username }}"吗？此操作不可恢复。</span>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="confirmDelete">确认删除</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user'
import { getDepartmentList } from '@/api/department'

export default {
  name: 'UserManagement',
  data() {
    return {
      loading: false,
      userList: [],
      total: 0,
      dialogVisible: false,
      dialogType: 'create',
      deleteDialogVisible: false,
      userToDelete: {},
      searchForm: {
        username: '',
        departmentId: null,
        timeRange: [],
        page: 1,
        pageSize: 10
      },
      userForm: {
        username: '',
        password: '',
        departmentId: null
      },
      userFormRules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 2, max: 20, message: '长度在 2 到 20 个字符', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
        ],
        departmentId: [
          { required: true, message: '请选择部门', trigger: 'change' }
        ]
      },
      departmentMap: {},
      departments: []
    }
  },
  created() {
    this.fetchDepartments()
    this.fetchUserList()
  },
  methods: {
    async fetchDepartments() {
      try {
        const response = await getDepartmentList()
        if (response && response.data) {
          this.departments = response.data
          // 构建部门映射
          this.departmentMap = {}
          response.data.forEach(dept => {
            this.departmentMap[dept.id] = dept.name
          })
        }
      } catch (error) {
        console.error('获取部门列表失败:', error)
        this.$message.error('获取部门列表失败')
      }
    },
    async fetchUserList() {
      this.loading = true
      try {
        const response = await getUserList({
          ...this.searchForm,
          page: this.searchForm.page,
          pageSize: this.searchForm.pageSize
        })
        
        if (response && response.data) {
          this.userList = response.data.map(user => ({
            ...user,
            createdAt: user.createdAt ? new Date(user.createdAt).toLocaleString('zh-CN') : ''
          }))
          this.total = response.data.length
        } else {
          this.userList = []
          this.total = 0
          this.$message.warning('暂无数据')
        }
      } catch (error) {
        console.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败，请检查网络连接')
        this.userList = []
        this.total = 0
      } finally {
        this.loading = false
      }
    },
    handleSearch() {
      this.searchForm.page = 1
      this.fetchUserList()
    },
    resetSearch() {
      this.searchForm = {
        username: '',
        departmentId: null,
        timeRange: [],
        page: 1,
        pageSize: 10
      }
      this.fetchUserList()
    },
    showCreateDialog() {
      this.dialogType = 'create'
      this.userForm = {
        username: '',
        password: '',
        departmentId: null
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.userFormRef?.resetFields()
      })
    },
    handleEdit(row) {
      this.dialogType = 'edit'
      this.userForm = {
        id: row.id,
        username: row.username,
        password: '', // 编辑时密码字段为空，表示不修改
        departmentId: row.department ? row.department.id : null
      }
      this.dialogVisible = true
    },
    async handleSubmit() {
      if (!this.$refs.userFormRef) return
      
      try {
        const valid = await this.$refs.userFormRef.validate()
        if (!valid) return

        this.loading = true
        
        if (this.dialogType === 'create') {
          await createUser(this.userForm)
          this.$message({
            type: 'success',
            message: '创建用户成功',
            duration: 2000
          })
        } else {
          const { id, departmentId, password } = this.userForm
          const updateData = { departmentId }
          
          // 只有当密码不为空时才更新密码
          if (password && password.trim() !== '') {
            updateData.password = password
          }
          
          await updateUser(id, updateData)
          this.$message({
            type: 'success',
            message: '更新用户成功',
            duration: 2000
          })
        }
        
        this.dialogVisible = false
        this.fetchUserList() // 刷新用户列表
      } catch (error) {
        console.error('操作失败:', error)
        this.$message({
          type: 'error',
          message: error.response?.data?.message || '操作失败，请稍后重试',
          duration: 3000
        })
      } finally {
        this.loading = false
      }
    },
    handleDelete(row) {
      this.userToDelete = row;
      this.deleteDialogVisible = true;
    },
    async confirmDelete() {
      try {
        this.loading = true
        await deleteUser(this.userToDelete.id)
        this.$message({
          type: 'success',
          message: `用户 "${this.userToDelete.username}" 已成功删除`,
          duration: 2000
        })
        this.deleteDialogVisible = false
        this.fetchUserList() // 刷新用户列表
      } catch (error) {
        console.error('删除失败:', error)
        this.$message({
          type: 'error',
          message: error.response?.data?.message || '删除失败，请稍后重试',
          duration: 3000
        })
      } finally {
        this.loading = false
      }
    },
    handleSizeChange(val) {
      this.searchForm.pageSize = val
      this.fetchUserList()
    },
    handleCurrentChange(val) {
      this.searchForm.page = val
      this.fetchUserList()
    },
    getDepartmentName(id) {
      if (!id) return '未分配'
      return this.departmentMap[id] || '未知部门'
    },
    formatDateTime(datetime) {
      if (!datetime) return '';
      const date = new Date(datetime);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    },
    goToAdminPanel() {
      this.$router.replace('/admin');
    },
    goToHome() {
      this.$router.replace('/');
    }
  }
}
</script>

<style>
.user-management {
  padding: 20px;
}

h1 {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.search-form {
  margin-bottom: 20px;
  padding: 20px;
  background-color: #fff;
  border-radius: 4px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.delete-confirm {
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

@media screen and (max-width: 768px) {
  .user-management {
    padding: 10px;
  }

  h1 {
    font-size: 20px;
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .header-buttons {
    width: 100%;
    justify-content: flex-start;
  }

  .header-buttons .el-button {
    flex: 1;
  }
  
  .delete-confirm {
    flex-direction: column;
    gap: 10px;
  }
  
  .el-table .cell {
    white-space: nowrap;
  }
}
</style> 