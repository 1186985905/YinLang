<template>
  <div class="prompt-config">
    <h1>
      提示词配置
      <div class="header-buttons">
        <el-button type="default" size="small" icon="el-icon-back" @click="goBack">返回管理面板</el-button>
        <el-button type="primary" size="small" icon="el-icon-s-home" @click="goHome">返回主页</el-button>
      </div>
    </h1>

    <el-row :gutter="20">
      <!-- 左侧部门列表 -->
      <el-col :span="6">
        <el-card class="department-card">
          <div slot="header" class="card-header">
            <span>部门列表</span>
            <el-button type="text" icon="el-icon-plus" @click="showAddDeptDialog">添加部门</el-button>
          </div>
          <div class="departments-list">
            <div 
              v-for="dept in departments" 
              :key="dept.id" 
              class="department-item"
              :class="{ active: currentDept && currentDept.id === dept.id }"
              @click="handleDeptSelect(dept)"
            >
              <span class="dept-name">{{ dept.name }}</span>
              <div class="dept-actions">
                <el-button
                  type="text"
                  size="mini"
                  @click.stop="editDepartment(dept)">
                  <i class="el-icon-edit"></i>
                </el-button>
                <el-button
                  type="text"
                  size="mini"
                  @click.stop="removeDepartment(dept)">
                  <i class="el-icon-delete"></i>
                </el-button>
              </div>
            </div>
            <div v-if="departments.length === 0" class="empty-dept">
              <i class="el-icon-document"></i>
              <p>暂无部门，请点击上方"添加部门"按钮添加</p>
            </div>
          </div>
        </el-card>
      </el-col>

      <!-- 右侧提示词列表 -->
      <el-col :span="18">
        <el-card class="prompt-card">
          <div slot="header" class="card-header">
            <span>{{ currentDept ? currentDept.name + ' - 提示词模板列表' : '请选择部门' }}</span>
            <el-button 
              type="primary" 
              size="small" 
              icon="el-icon-plus" 
              @click="showAddPromptDialog"
              :disabled="!currentDept">
              添加提示词模板
            </el-button>
          </div>

          <el-table
            v-if="currentDept"
            :data="promptList"
            style="width: 100%"
            v-loading="loading">
            <el-table-column prop="title" label="标题" width="200"></el-table-column>
            <el-table-column prop="content" label="提示词内容">
              <template slot-scope="scope">
                <div class="prompt-content">{{ scope.row.content }}</div>
              </template>
            </el-table-column>
            <el-table-column prop="category" label="分类" width="120">
              <template slot-scope="scope">
                <el-tag size="medium">{{ scope.row.category }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" fixed="right">
              <template slot-scope="scope">
                <el-button
                  size="mini"
                  type="primary"
                  plain
                  @click="editPrompt(scope.row)">编辑</el-button>
                <el-button
                  size="mini"
                  type="danger"
                  plain
                  @click="removePrompt(scope.row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <div v-else class="empty-state">
            <i class="el-icon-document"></i>
            <p>请先选择左侧部门来管理提示词模板</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加/编辑部门对话框 -->
    <el-dialog
      :title="deptDialogType === 'add' ? '添加部门' : '编辑部门'"
      :visible.sync="deptDialogVisible"
      width="500px">
      <el-form :model="deptForm" :rules="deptRules" ref="deptForm" label-width="80px">
        <el-form-item label="部门名称" prop="name">
          <el-input v-model="deptForm.name" placeholder="请输入部门名称"></el-input>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="deptForm.remark" type="textarea" :rows="3" placeholder="请输入部门备注信息（可选）"></el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="deptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveDepartment">确定</el-button>
      </div>
    </el-dialog>

    <!-- 添加/编辑提示词对话框 -->
    <el-dialog
      :title="promptDialogType === 'add' ? '添加提示词模板' : '编辑提示词模板'"
      :visible.sync="promptDialogVisible"
      width="650px">
      <el-form :model="promptForm" :rules="promptRules" ref="promptForm" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="promptForm.title" placeholder="请输入提示词模板标题"></el-input>
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="promptForm.category" placeholder="请选择分类" style="width: 100%">
            <el-option label="常用提示" value="常用提示"></el-option>
            <el-option label="工作流程" value="工作流程"></el-option>
            <el-option label="专业知识" value="专业知识"></el-option>
            <el-option label="其他" value="其他"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            type="textarea"
            v-model="promptForm.content"
            :rows="6"
            placeholder="请输入提示词模板内容">
          </el-input>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="promptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrompt">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getDepartmentList,
  createDepartment,
  updateDepartment,
  deleteDepartment,
  getPromptList,
  addPrompt,
  updatePrompt,
  deletePrompt
} from '@/api/department'

export default {
  name: 'PromptConfig',
  data() {
    return {
      loading: false,
      departments: [],
      currentDept: null,
      promptList: [],
      // 部门对话框
      deptDialogVisible: false,
      deptDialogType: 'add',
      deptForm: {
        name: '',
        remark: ''
      },
      deptRules: {
        name: [
          { required: true, message: '请输入部门名称', trigger: 'blur' },
          { min: 2, max: 60, message: '长度在 2 到 60 个字符', trigger: 'blur' }
        ],
        remark: [
          { max: 255, message: '备注最多 255 个字符', trigger: 'blur' }
        ]
      },
      // 提示词对话框
      promptDialogVisible: false,
      promptDialogType: 'add',
      promptForm: {
        title: '',
        content: '',
        department_id: null
      },
      promptRules: {
        title: [
          { required: true, message: '请输入提示词模板标题', trigger: 'blur' },
          { min: 2, max: 60, message: '长度在 2 到 60 个字符', trigger: 'blur' }
        ],
        content: [
          { required: true, message: '请输入提示词模板内容', trigger: 'blur' },
          { min: 2, max: 360, message: '长度在 2 到 360 个字符', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    goBack() {
      if (this.$route.path !== '/admin') {
        this.$router.push('/admin');
      }
    },
    goHome() {
      if (this.$route.path !== '/') {
        this.$router.push('/');
      }
    },
    // 获取部门列表
    async fetchDepartments() {
      try {
        this.loading = true
        const res = await getDepartmentList()
        // 确保返回的数据是数组
        this.departments = Array.isArray(res.data) ? res.data : []
        
        // 如果有部门，默认选中第一个
        if (this.departments.length > 0 && !this.currentDept) {
          this.handleDeptSelect(this.departments[0])
        }
      } catch (error) {
        console.error('获取部门列表失败:', error)
        this.$message.error('获取部门列表失败')
        this.departments = [] // 确保失败时设置为空数组
      } finally {
        this.loading = false
      }
    },

    // 获取提示词列表
    async fetchPromptList() {
      if (!this.currentDept) {
        this.promptList = []
        return
      }
      
      try {
        this.loading = true
        const res = await getPromptList(this.currentDept.id)
        // 确保返回的数据是数组
        this.promptList = Array.isArray(res.data) ? res.data : []
      } catch (error) {
        console.error('获取提示词列表失败:', error)
        this.$message.error('获取提示词列表失败')
        this.promptList = [] // 确保失败时设置为空数组
      } finally {
        this.loading = false
      }
    },

    // 选择部门
    async handleDeptSelect(dept) {
      if (!dept) return
      this.currentDept = dept
      await this.fetchPromptList()
    },

    // 显示添加部门对话框
    showAddDeptDialog() {
      this.deptDialogType = 'add'
      this.deptForm = {
        name: '',
        remark: ''
      }
      this.deptDialogVisible = true
    },

    // 编辑部门
    editDepartment(dept) {
      this.deptDialogType = 'edit'
      this.deptForm = {
        id: dept.id,
        name: dept.name,
        remark: dept.remark || ''
      }
      this.deptDialogVisible = true
    },

    // 删除部门
    async removeDepartment(dept) {
      try {
        await this.$confirm('确认删除该部门吗？相关的提示词模板也会被删除', '提示', {
          type: 'warning'
        })
        
        await deleteDepartment(dept.id)
        this.$message.success('删除部门成功')
        
        if (this.currentDept && this.currentDept.id === dept.id) {
          this.currentDept = null
          this.promptList = []
        }
        
        await this.fetchDepartments()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除部门失败:', error)
          this.$message.error(error.message || '删除失败')
        }
      }
    },

    // 保存部门
    async saveDepartment() {
      try {
        const valid = await this.$refs.deptForm.validate()
        if (!valid) return;
        
        this.loading = true;
        if (this.deptDialogType === 'add') {
          await createDepartment({
            name: this.deptForm.name,
            description: this.deptForm.remark
          });
          this.$message.success('添加部门成功');
        } else {
          await updateDepartment(this.deptForm.id, {
            name: this.deptForm.name,
            description: this.deptForm.remark
          });
          this.$message.success('更新部门成功');
        }
        
        this.deptDialogVisible = false
        this.resetDeptForm()
        await this.fetchDepartments()
      } catch (error) {
        console.error('保存部门失败:', error)
        this.$message.error(error.message || '操作失败')
      } finally {
        this.loading = false
      }
    },

    // 显示添加提示词对话框
    showAddPromptDialog() {
      if (!this.currentDept) {
        this.$message.warning('请先选择部门')
        return
      }
      
      this.promptDialogType = 'add'
      this.promptForm = {
        title: '',
        content: '',
        department_id: this.currentDept.id
      }
      this.promptDialogVisible = true
    },

    // 编辑提示词
    editPrompt(prompt) {
      this.promptDialogType = 'edit'
      this.promptForm = { ...prompt }
      this.promptDialogVisible = true
    },

    // 删除提示词
    async removePrompt(prompt) {
      try {
        await this.$confirm('确认删除该提示词模板吗？', '提示', {
          type: 'warning'
        })
        
        await deletePrompt(prompt.id, this.currentDept.id)
        this.$message.success('删除提示词模板成功')
        await this.fetchPromptList()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('删除提示词失败:', error)
          this.$message.error(error.message || '删除失败')
        }
      }
    },

    // 保存提示词
    async savePrompt() {
      try {
        await this.$refs.promptForm.validate()
        
        if (!this.currentDept) {
          this.$message.warning('请先选择部门')
          return
        }
        
        // 确保设置了department_id
        this.promptForm.department_id = this.currentDept.id
        
        if (this.promptDialogType === 'add') {
          await addPrompt(this.promptForm)
          this.$message.success('添加提示词模板成功')
        } else {
          await updatePrompt(this.promptForm.id, this.promptForm)
          this.$message.success('更新提示词模板成功')
        }
        
        this.promptDialogVisible = false
        this.resetPromptForm()
        await this.fetchPromptList()
      } catch (error) {
        console.error('保存提示词失败:', error)
        this.$message.error(error.message || '操作失败')
      }
    },

    // 重置表单
    resetDeptForm() {
      this.$refs.deptForm.resetFields()
      this.deptForm = {
        name: '',
        remark: ''
      }
    },

    resetPromptForm() {
      this.$refs.promptForm.resetFields()
      this.promptForm = {
        title: '',
        content: '',
        department_id: this.currentDept ? this.currentDept.id : null
      }
    }
  },
  created() {
    this.fetchDepartments()
  }
}
</script>

<style scoped>
.prompt-config {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
}

h1 {
  font-size: 24px;
  color: #333;
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-buttons {
  display: flex;
  gap: 10px;
}

.department-card, .prompt-card {
  margin-bottom: 20px;
}

.department-card {
  min-height: 400px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.departments-list {
  max-height: 550px;
  overflow-y: auto;
}

.department-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 15px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  transition: background-color 0.3s;
}

.department-item:hover {
  background-color: #f5f7fa;
}

.department-item.active {
  background-color: #ecf5ff;
}

.dept-name {
  font-size: 14px;
  color: #303133;
}

.dept-actions {
  display: flex;
  gap: 5px;
  opacity: 0;
  transition: opacity 0.2s;
}

.department-item:hover .dept-actions {
  opacity: 1;
}

.empty-dept {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.empty-dept i {
  font-size: 40px;
  margin-bottom: 10px;
}

.prompt-content {
  max-height: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.empty-state {
  text-align: center;
  padding: 40px 0;
  color: #909399;
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 10px;
}

@media screen and (max-width: 768px) {
  .prompt-config {
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