<template>
  <div class="user-profile">
    <div class="header">
      <h1>用户管理</h1>
      <div class="header-buttons">
        <el-button type="default" size="small" icon="el-icon-back" @click="goBack">返回管理面板</el-button>
        <el-button type="primary" size="small" icon="el-icon-s-home" @click="goHome">返回主页</el-button>
      </div>
    </div>

    <!-- 搜索区域 -->
    <div class="search-area">
      <el-form :inline="true" :model="queryParams" class="search-form">
        <el-form-item label="用户名">
          <el-input v-model="queryParams.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="queryParams.departmentId" placeholder="请选择部门" clearable>
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="创建时间">
          <el-date-picker
            v-model="queryParams.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd">
          </el-date-picker>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleQuery">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">创建新用户</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 用户列表 -->
    <el-table
      :data="tableData"
      v-loading="loading"
      border
      style="width: 100%">
      <el-table-column type="selection" width="55"></el-table-column>
      <el-table-column prop="username" label="用户名"></el-table-column>
      <el-table-column prop="departmentName" label="所属部门">
        <template slot-scope="scope">
          {{ getDepartmentName(scope.row.departmentId) }}
        </template>
      </el-table-column>
      <el-table-column prop="createdTime" label="创建时间"></el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="primary"
            @click="handleEdit(scope.row)">编辑</el-button>
          <el-button
            size="mini"
            type="danger"
            @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-container">
      <span class="total-count">共 {{ total }} 条</span>
      <el-pagination
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        :current-page="queryParams.pageNum"
        :page-sizes="[10, 20, 50, 100]"
        :page-size="queryParams.pageSize"
        layout="prev, pager, next, jumper"
        :total="total">
      </el-pagination>
      <span class="page-info">前往 {{ queryParams.pageNum }} 页</span>
    </div>

    <!-- 用户表单对话框 -->
    <el-dialog
      :title="dialogType === 'add' ? '创建新用户' : '编辑用户'"
      :visible.sync="dialogVisible"
      width="500px"
      @closed="resetForm">
      <el-form
        ref="form"
        :model="formData"
        :rules="rules"
        label-width="80px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名"></el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="dialogType === 'add'">
          <el-input v-model="formData.password" type="password" placeholder="请输入密码" show-password></el-input>
        </el-form-item>
        <el-form-item label="所属部门" prop="departmentId">
          <el-select v-model="formData.departmentId" placeholder="请选择部门">
            <el-option v-for="dept in departments" :key="dept.id" :label="dept.name" :value="dept.id"></el-option>
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确定</el-button>
      </div>
    </el-dialog>

    <!-- 删除确认对话框 -->
    <el-dialog
      title="提示"
      :visible.sync="deleteDialogVisible"
      width="300px">
      <div class="delete-confirm">
        <i class="el-icon-warning"></i>
        <span>确认删除该用户吗？</span>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="deleteDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmDelete">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user'
import { getDepartmentList } from '@/api/department'

export default {
  name: 'UserProfile',
  data() {
    return {
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        username: '',
        departmentId: undefined,
        dateRange: []
      },
      // 表格数据
      tableData: [],
      loading: false,
      total: 0,

      // 部门数据
      departments: [],

      // 对话框相关
      dialogVisible: false,
      dialogType: 'add',
      deleteDialogVisible: false,
      deleteUserId: null,

      // 表单数据
      formData: {
        username: '',
        password: '',
        departmentId: undefined
      },

      // 表单校验规则
      rules: {
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
      }
    }
  },
  methods: {
    goBack() {
      if (this.$route.path !== '/admin') {
        this.$router.push('/admin')
      }
    },
    goHome() {
      if (this.$route.path !== '/') {
        this.$router.push('/')
      }
    },
    // 获取部门列表
    async fetchDepartments() {
      try {
        const res = await getDepartmentList()
        this.departments = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        console.error('获取部门列表失败:', error)
        this.$message.error('获取部门列表失败')
      }
    },

    // 获取用户列表
    async fetchUserList() {
      this.loading = true
      try {
        const res = await getUserList(this.queryParams)
        this.tableData = res.list || []
        this.total = res.total || 0
      } catch (error) {
        console.error('获取用户列表失败:', error)
        this.$message.error('获取用户列表失败')
      } finally {
        this.loading = false
      }
    },

    // 查询
    handleQuery() {
      this.queryParams.pageNum = 1
      this.fetchUserList()
    },

    // 重置查询
    resetQuery() {
      this.queryParams = {
        pageNum: 1,
        pageSize: 10,
        username: '',
        departmentId: undefined,
        dateRange: []
      }
      this.fetchUserList()
    },

    // 处理分页
    handleSizeChange(val) {
      this.queryParams.pageSize = val
      this.fetchUserList()
    },

    handleCurrentChange(val) {
      this.queryParams.pageNum = val
      this.fetchUserList()
    },

    // 新增用户
    handleAdd() {
      this.dialogType = 'add'
      this.formData = {
        username: '',
        password: '',
        departmentId: undefined
      }
      this.dialogVisible = true
    },

    // 编辑用户
    handleEdit(row) {
      this.dialogType = 'edit'
      this.formData = {
        id: row.id,
        username: row.username,
        departmentId: row.departmentId
      }
      this.dialogVisible = true
    },

    // 删除用户
    handleDelete(row) {
      this.deleteUserId = row.id
      this.deleteDialogVisible = true
    },

    // 确认删除
    async confirmDelete() {
      try {
        await deleteUser(this.deleteUserId)
        this.$message.success('删除成功')
        this.deleteDialogVisible = false
        this.fetchUserList()
      } catch (error) {
        console.error('删除失败:', error)
        this.$message.error('删除失败')
      }
    },

    // 提交表单
    async submitForm() {
      try {
        await this.$refs.form.validate()
        
        if (this.dialogType === 'add') {
          await createUser(this.formData)
          this.$message.success('创建成功')
        } else {
          const { id, ...updateData } = this.formData
          await updateUser(id, updateData)
          this.$message.success('更新成功')
        }
        
        this.dialogVisible = false
        this.fetchUserList()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error('操作失败')
      }
    },

    // 重置表单
    resetForm() {
      if (this.$refs.form) {
        this.$refs.form.resetFields()
      }
      this.formData = {
        username: '',
        password: '',
        departmentId: undefined
      }
    },

    // 获取部门名称
    getDepartmentName(departmentId) {
      if (!departmentId) return '未分配'
      const department = this.departments.find(d => d.id === departmentId)
      return department ? department.name : '未知部门'
    }
  },
  created() {
    this.fetchDepartments()
    this.fetchUserList()
  }
}
</script>

<style scoped>
.user-profile {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header h1 {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.search-area {
  background-color: #fff;
  padding: 20px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
}

.search-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.pagination-container {
  margin-top: 20px;
  padding: 10px 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  border-radius: 4px;
}

.total-count {
  color: #606266;
  font-size: 13px;
}

.page-info {
  color: #606266;
  font-size: 13px;
}

.delete-confirm {
  text-align: center;
  padding: 20px 0;
}

.delete-confirm i {
  font-size: 24px;
  color: #e6a23c;
  margin-right: 10px;
}

.delete-confirm span {
  font-size: 16px;
  color: #606266;
}

@media screen and (max-width: 768px) {
  .search-form {
    flex-direction: column;
  }

  .search-form .el-form-item {
    margin-right: 0;
  }

  .pagination-container {
    flex-direction: column;
    gap: 10px;
    align-items: center;
  }
}
</style> 