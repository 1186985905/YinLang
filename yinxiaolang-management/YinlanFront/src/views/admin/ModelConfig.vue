<template>
  <div class="model-config">
    <div class="header">
      <h1>模型配置</h1>
      <div class="header-actions">
        <el-button icon="el-icon-back" @click="backToAdmin">返回管理面板</el-button>
        <el-button type="primary" icon="el-icon-plus" @click="showAddDialog">添加新模型</el-button>
      </div>
    </div>

    <el-card class="model-table-container">
      <div slot="header" class="card-header">
        <h3>可用模型列表</h3>
        <div class="search-input">
          <el-input
            placeholder="搜索模型名称"
            v-model="searchQuery"
            prefix-icon="el-icon-search"
            clearable
          ></el-input>
        </div>
      </div>

      <el-table 
        :data="filteredModels" 
        stripe 
        style="width: 100%" 
        v-loading="loading"
        border
      >
        <el-table-column prop="name" label="模型名称" width="150"></el-table-column>
        <el-table-column prop="description" label="描述" show-overflow-tooltip></el-table-column>
        <el-table-column prop="provider" label="厂商" width="120"></el-table-column>
        <el-table-column prop="endpoint" label="调用地址" show-overflow-tooltip></el-table-column>
        <el-table-column prop="apiKey" label="API Key" width="120">
          <template slot-scope="scope">
            <span>{{ maskApiKey(scope.row.apiKey) }}</span>
            <el-button 
              type="text" 
              icon="el-icon-view" 
              @click="toggleApiKeyVisibility(scope.row)" 
              size="mini"
            ></el-button>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isActive ? 'success' : 'info'">
              {{ scope.row.isActive ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template slot-scope="scope">
            <el-button 
              type="text" 
              icon="el-icon-edit" 
              @click="editModel(scope.row)"
            >编辑</el-button>
            <el-button 
              type="text" 
              icon="el-icon-delete" 
              @click="deleteModel(scope.row)"
              class="delete-btn"
            >删除</el-button>
            <el-switch
              v-model="scope.row.isActive"
              active-color="#13ce66"
              inactive-color="#ff4949"
              @change="toggleModelStatus(scope.row)"
            ></el-switch>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑模型对话框 -->
    <el-dialog
      :title="isEditing ? '编辑模型' : '添加新模型'"
      :visible.sync="dialogVisible"
      width="600px"
    >
      <el-form ref="modelForm" :model="currentModel" :rules="rules" label-width="100px">
        <el-form-item label="模型名称" prop="name">
          <el-input v-model="currentModel.name" placeholder="输入模型名称"></el-input>
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input 
            type="textarea" 
            v-model="currentModel.description" 
            :rows="3"
            placeholder="输入模型描述"
          ></el-input>
        </el-form-item>
        <el-form-item label="厂商" prop="provider">
          <el-input v-model="currentModel.provider" placeholder="输入模型厂商"></el-input>
        </el-form-item>
        <el-form-item label="调用地址" prop="endpoint">
          <el-input v-model="currentModel.endpoint" placeholder="输入API调用地址"></el-input>
        </el-form-item>
        <el-form-item label="API Key" prop="apiKey">
          <el-input 
            v-model="currentModel.apiKey" 
            placeholder="输入API Key" 
            show-password
          ></el-input>
        </el-form-item>
        <el-form-item label="最大Token" prop="maxTokens">
          <el-input-number v-model="currentModel.maxTokens" :min="1" :max="100000"></el-input-number>
        </el-form-item>
        <el-form-item label="温度" prop="temperature">
          <el-slider
            v-model="currentModel.temperature"
            :min="0"
            :max="2"
            :step="0.1"
            show-input
          ></el-slider>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch
            v-model="currentModel.isActive"
            active-text="启用"
            inactive-text="禁用"
          ></el-switch>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveModel">保存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ModelConfig',
  data() {
    return {
      loading: false,
      models: [
        {
          id: 1,
          name: 'GPT-4o',
          description: 'OpenAI的最新多模态模型，可以处理文字、图片和音频输入，支持多种任务。',
          provider: 'OpenAI',
          endpoint: 'https://api.openai.com/v1/chat/completions',
          apiKey: 'sk-1234567890abcdefghijkl',
          isApiKeyVisible: false,
          maxTokens: 8192,
          temperature: 0.7,
          isActive: true
        },
        {
          id: 2,
          name: 'deepseek-v3',
          description: '国产大语言模型，拥有较强的中文处理能力和代码生成能力。',
          provider: 'DeepSeek',
          endpoint: 'https://api.deepseek.com/v1/chat',
          apiKey: 'dsk_98765432100abcdefgh',
          isApiKeyVisible: false,
          maxTokens: 8000,
          temperature: 0.8,
          isActive: true
        },
        {
          id: 3,
          name: 'Qwen2.5-Max',
          description: '阿里通义千问最新模型，拥有超强的中文和编程能力，特别适合国内场景。',
          provider: '阿里云',
          endpoint: 'https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation',
          apiKey: 'sk_qwen_25987654321abcdefgh',
          isApiKeyVisible: false,
          maxTokens: 10000,
          temperature: 0.9,
          isActive: true
        }
      ],
      searchQuery: '',
      dialogVisible: false,
      isEditing: false,
      currentModel: {
        id: null,
        name: '',
        description: '',
        provider: '',
        endpoint: '',
        apiKey: '',
        maxTokens: 4096,
        temperature: 0.7,
        isActive: true
      },
      rules: {
        name: [
          { required: true, message: '请输入模型名称', trigger: 'blur' },
          { min: 2, max: 50, message: '长度在 2 到 50 个字符', trigger: 'blur' }
        ],
        description: [
          { required: true, message: '请输入模型描述', trigger: 'blur' }
        ],
        provider: [
          { required: true, message: '请输入厂商名称', trigger: 'blur' }
        ],
        endpoint: [
          { required: true, message: '请输入调用地址', trigger: 'blur' }
        ],
        apiKey: [
          { required: true, message: '请输入API Key', trigger: 'blur' }
        ]
      }
    }
  },
  computed: {
    filteredModels() {
      if (!this.searchQuery) {
        return this.models;
      }
      
      const query = this.searchQuery.toLowerCase();
      return this.models.filter(model => {
        return model.name.toLowerCase().includes(query) || 
               model.description.toLowerCase().includes(query) ||
               model.provider.toLowerCase().includes(query);
      });
    }
  },
  methods: {
    backToAdmin() {
      this.$router.push('/admin');
    },
    
    maskApiKey(apiKey) {
      if (!apiKey) return '';
      // 仅显示前4位和后4位
      return apiKey.substring(0, 4) + '••••••••••••' + apiKey.substring(apiKey.length - 4);
    },
    
    toggleApiKeyVisibility(model) {
      model.isApiKeyVisible = !model.isApiKeyVisible;
      this.$set(model, 'apiKey', model.isApiKeyVisible ? model.apiKey : this.maskApiKey(model.apiKey));
    },
    
    showAddDialog() {
      this.isEditing = false;
      this.currentModel = {
        id: null,
        name: '',
        description: '',
        provider: '',
        endpoint: '',
        apiKey: '',
        maxTokens: 4096,
        temperature: 0.7,
        isActive: true
      };
      this.dialogVisible = true;
    },
    
    editModel(model) {
      this.isEditing = true;
      this.currentModel = JSON.parse(JSON.stringify(model)); // 深拷贝
      this.dialogVisible = true;
    },
    
    deleteModel(model) {
      this.$confirm(`确定要删除模型 "${model.name}" 吗?`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 实际应用中这里应该调用API
        this.models = this.models.filter(m => m.id !== model.id);
        this.$message({
          type: 'success',
          message: '删除成功!'
        });
      }).catch(() => {
        this.$message({
          type: 'info',
          message: '已取消删除'
        });          
      });
    },
    
    toggleModelStatus(model) {
      const status = model.isActive ? '启用' : '禁用';
      this.$message({
        type: 'success',
        message: `已${status}模型 "${model.name}"`
      });
      
      // 实际应用中这里应该调用API更新状态
    },
    
    saveModel() {
      this.$refs.modelForm.validate((valid) => {
        if (valid) {
          // 实际应用中这里应该调用API
          if (this.isEditing) {
            // 更新现有模型
            const index = this.models.findIndex(m => m.id === this.currentModel.id);
            if (index !== -1) {
              this.$set(this.models, index, {...this.currentModel});
            }
            this.$message.success('模型更新成功');
          } else {
            // 添加新模型
            const newId = this.models.length > 0 ? Math.max(...this.models.map(m => m.id)) + 1 : 1;
            this.models.push({...this.currentModel, id: newId});
            this.$message.success('模型添加成功');
          }
          this.dialogVisible = false;
        } else {
          return false;
        }
      });
    }
  }
}
</script>

<style scoped>
.model-config {
  padding: 20px;
  max-width: 1200px;
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

.header-actions {
  display: flex;
  gap: 10px;
}

.model-table-container {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-input {
  width: 300px;
}

.card-header h3 {
  margin: 0;
  font-size: 18px;
  color: #333;
}

.delete-btn {
  margin-right: 10px;
  color: #f56c6c;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .model-config {
    padding: 10px;
  }
  
  .header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .header-actions {
    width: 100%;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .search-input {
    width: 100%;
  }
}
</style> 