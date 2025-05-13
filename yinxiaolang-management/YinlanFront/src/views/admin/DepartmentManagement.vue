<template>
  <div class="department-management">
    <h1>
      部门管理
      <div class="header-buttons">
        <el-button type="default" size="small" icon="el-icon-back" @click="goToAdminPanel">返回管理面板</el-button>
        <el-button type="primary" size="small" icon="el-icon-s-home" @click="goToHome">返回主页</el-button>
      </div>
    </h1>

    <!-- 搜索和操作 -->
    <div class="operations">
      <el-button type="primary" @click="handleAdd">添加新部门</el-button>
    </div>
      
    <!-- 部门列表 -->
    <el-table :data="departmentList" style="width: 100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="80"></el-table-column>
      <el-table-column prop="name" label="部门名称"></el-table-column>
      <el-table-column prop="description" label="部门描述"></el-table-column>
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

    <!-- 创建/编辑部门对话框 -->
    <el-dialog
      :title="dialogType === 'create' ? '添加新部门' : '编辑部门'"
      :visible.sync="dialogVisible"
      width="500px">
      <el-form 
        :model="departmentForm" 
        :rules="departmentFormRules" 
        ref="departmentFormRef"
        label-width="100px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="departmentForm.name"></el-input>
        </el-form-item>
        <el-form-item label="部门描述" prop="description">
          <el-input type="textarea" v-model="departmentForm.description" rows="3"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getDepartmentList, createDepartment, updateDepartment, deleteDepartment } from '@/api/department'

export default {
  name: 'DepartmentManagement',
  data() {
    return {
      loading: false,
      departmentList: [],
      dialogVisible: false,
      dialogType: 'create',
      departmentForm: {
        name: '',
        description: ''
      },
      departmentFormRules: {
        name: [
          { required: true, message: '请输入部门名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        description: [
          { max: 200, message: '长度不能超过 200 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.fetchDepartmentList()
  },
  methods: {
    async fetchDepartmentList() {
      this.loading = true
      try {
        const response = await getDepartmentList()
        
        if (response && response.data) {
          this.departmentList = response.data.map(dept => ({
            ...dept,
            createdAt: dept.createdAt ? new Date(dept.createdAt).toLocaleString('zh-CN') : ''
          }))
        } else {
          this.departmentList = []
          this.$message.warning('暂无数据')
        }
      } catch (error) {
        console.error('获取部门列表失败:', error)
        this.$message.error('获取部门列表失败，请检查网络连接')
        this.departmentList = []
      } finally {
        this.loading = false
      }
    },
    handleAdd() {
      this.dialogType = 'create'
      this.departmentForm = {
        name: '',
        description: ''
      }
      this.dialogVisible = true
      this.$nextTick(() => {
        this.$refs.departmentFormRef?.resetFields()
      })
    },
    handleEdit(row) {
      this.dialogType = 'edit'
      this.departmentForm = {
        id: row.id,
        name: row.name,
        description: row.description
      }
      this.dialogVisible = true
    },
    async handleSubmit() {
      if (!this.$refs.departmentFormRef) return
      
      try {
        await this.$refs.departmentFormRef.validate()
        
        if (this.dialogType === 'create') {
          await createDepartment(this.departmentForm)
          this.$message.success('部门创建成功')
        } else {
          const { id, name, description } = this.departmentForm
          await updateDepartment(id, { name, description })
          this.$message.success('部门更新成功')
        }
        
        this.dialogVisible = false
        this.fetchDepartmentList()
      } catch (error) {
        console.error('操作失败:', error)
        this.$message.error(error.response?.data?.message || '操作失败')
      }
    },
    handleDelete(row) {
      this.$confirm('确认删除该部门吗？这可能会影响关联的用户', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        try {
          await deleteDepartment(row.id)
          this.$message.success('删除成功')
          this.fetchDepartmentList()
        } catch (error) {
          this.$message.error(error.message || '删除失败')
        }
      })
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
.department-management {
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

.operations {
  margin-bottom: 20px;
  display: flex;
  justify-content: flex-end;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

@media screen and (max-width: 768px) {
  .department-management {
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
}
</style> 