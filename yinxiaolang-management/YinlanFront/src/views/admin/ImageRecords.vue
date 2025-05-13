<template>
  <div class="image-records">
    <h1>
      生成图片查询
      <div class="header-buttons">
        <el-button type="default" size="small" icon="el-icon-back" @click="goBack">返回管理面板</el-button>
        <el-button type="primary" size="small" icon="el-icon-s-home" @click="goHome">返回主页</el-button>
      </div>
    </h1>

    <!-- 搜索条件 -->
    <el-card class="filter-card">
      <el-form :inline="true" :model="filterForm" class="filter-form" size="small">
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="filterForm.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="yyyy-MM-dd"
            :picker-options="pickerOptions">
          </el-date-picker>
        </el-form-item>
        <el-form-item label="部门">
          <el-cascader
            v-model="filterForm.department"
            :options="departmentOptions"
            :props="{ checkStrictly: true }"
            clearable
            placeholder="请选择部门">
          </el-cascader>
        </el-form-item>
        <el-form-item label="用户">
          <el-select
            v-model="filterForm.userId"
            filterable
            remote
            reserve-keyword
            placeholder="请输入用户名"
            :remote-method="remoteSearchUsers"
            :loading="userSearchLoading"
            clearable>
            <el-option
              v-for="user in userOptions"
              :key="user.id"
              :label="user.name"
              :value="user.id">
              <span>{{ user.name }}</span>
              <span class="user-email">{{ user.email }}</span>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="提示词">
          <el-input
            v-model="filterForm.prompt"
            placeholder="请输入关键词"
            clearable
            @keyup.enter.native="handleSearch">
          </el-input>
        </el-form-item>
        <el-form-item label="风格">
          <el-select v-model="filterForm.style" placeholder="请选择风格" clearable>
            <el-option label="写实风格" value="realistic"></el-option>
            <el-option label="卡通风格" value="cartoon"></el-option>
            <el-option label="油画风格" value="oil"></el-option>
            <el-option label="水彩风格" value="watercolor"></el-option>
            <el-option label="素描风格" value="sketch"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetFilter">重置</el-button>
          <el-button type="success" icon="el-icon-download" @click="exportRecords">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 内容展示 -->
    <el-card class="content-card">
      <div slot="header" class="card-header">
        <span>图片生成列表</span>
        <div class="card-controls">
          <el-button 
            type="danger" 
            size="small" 
            icon="el-icon-delete" 
            :disabled="selectedImages.length === 0"
            @click="handleBatchDelete">批量删除</el-button>
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button label="table">表格视图</el-radio-button>
            <el-radio-button label="grid">网格视图</el-radio-button>
          </el-radio-group>
        </div>
      </div>

      <!-- 表格视图 -->
      <template v-if="viewMode === 'table'">
        <el-table
          :data="imageRecords"
          style="width: 100%"
          v-loading="loading"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55"></el-table-column>
          <el-table-column prop="timestamp" label="生成时间" width="180" :formatter="formatDate"></el-table-column>
          <el-table-column prop="username" label="用户" width="120"></el-table-column>
          <el-table-column prop="department" label="部门" width="120"></el-table-column>
          <el-table-column prop="prompt" label="提示词" show-overflow-tooltip></el-table-column>
          <el-table-column prop="model" label="模型" width="120"></el-table-column>
          <el-table-column prop="style" label="风格" width="100">
            <template slot-scope="scope">
              <el-tag>{{ getStyleText(scope.row.style) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="remainingDays" label="保存时间" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.remainingDays }}天</span>
              <el-button type="text" size="mini" @click="resetSaveTime(scope.row)" class="reset-button">
                <i class="el-icon-refresh"></i>
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="预览" width="80">
            <template slot-scope="scope">
              <el-button type="text" @click="previewImage(scope.row)">
                <i class="el-icon-picture-outline"></i> 预览
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                @click="downloadImage(scope.row)">
                <i class="el-icon-download"></i> 下载
              </el-button>
              <el-button
                size="mini"
                type="text"
                class="danger"
                @click="deleteImage(scope.row)">
                <i class="el-icon-delete"></i> 删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 网格视图 -->
      <template v-else>
        <div class="image-grid">
          <div v-for="record in imageRecords" :key="record.id" class="image-item">
            <div class="image-container" @click="previewImage(record)">
              <img :src="record.imageUrl" alt="生成图片" class="thumbnail">
              <div class="image-overlay">
                <i class="el-icon-zoom-in"></i>
              </div>
            </div>
            <div class="image-info">
              <div class="image-prompt" :title="record.prompt">{{ record.prompt }}</div>
              <div class="image-meta">
                <span>{{ record.username }}</span>
                <span>{{ formatDate(null, null, record.timestamp) }}</span>
              </div>
              <div class="image-actions">
                <el-tag size="mini" :type="getStatusType(record.status)">
                  {{ getStatusText(record.status) }}
                </el-tag>
                <div class="action-buttons">
                  <el-button type="text" size="mini" @click.stop="downloadImage(record)">
                    <i class="el-icon-download"></i>
                  </el-button>
                  <el-button type="text" size="mini" class="danger" @click.stop="deleteImage(record)">
                    <i class="el-icon-delete"></i>
                  </el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 分页 -->
      <div class="pagination-container">
        <el-pagination
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
          :current-page="currentPage"
          :page-sizes="[12, 24, 48, 96]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 图片预览对话框 -->
    <el-dialog
      :title="currentRecord ? currentRecord.prompt : '图片预览'"
      :visible.sync="previewVisible"
      width="800px"
      center>
      <div class="preview-container" v-if="currentRecord">
        <img :src="currentRecord.imageUrl" alt="生成图片" class="preview-image">
        <div class="preview-info">
          <div class="info-item">
            <span class="label">生成时间：</span>
            <span>{{ formatDate(null, null, currentRecord.timestamp) }}</span>
          </div>
          <div class="info-item">
            <span class="label">用户：</span>
            <span>{{ currentRecord.username }}</span>
          </div>
          <div class="info-item">
            <span class="label">部门：</span>
            <span>{{ currentRecord.department }}</span>
          </div>
          <div class="info-item">
            <span class="label">模型：</span>
            <span>{{ currentRecord.model }}</span>
          </div>
          <div class="info-item">
            <span class="label">分辨率：</span>
            <span>{{ currentRecord.resolution }}</span>
          </div>
          <div class="info-item">
            <span class="label">状态：</span>
            <el-tag :type="getStatusType(currentRecord.status)">
              {{ getStatusText(currentRecord.status) }}
            </el-tag>
          </div>
          <div class="info-item full-width">
            <span class="label">提示词：</span>
            <div class="prompt-text">{{ currentRecord.prompt }}</div>
          </div>
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="previewVisible = false">关闭</el-button>
        <el-button type="primary" @click="downloadImage(currentRecord)">下载图片</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ImageRecords',
  data() {
    return {
      loading: false,
      userSearchLoading: false,
      viewMode: 'grid',
      currentPage: 1,
      pageSize: 12,
      total: 0,
      previewVisible: false,
      currentRecord: null,
      selectedImages: [],
      filterForm: {
        dateRange: [],
        department: [],
        userId: '',
        prompt: '',
        style: ''
      },
      departmentOptions: [
        {
          value: 1,
          label: '研发部',
          children: [
            {
              value: 4,
              label: '前端组'
            },
            {
              value: 5,
              label: '后端组'
            }
          ]
        },
        {
          value: 2,
          label: '产品部'
        },
        {
          value: 3,
          label: '运营部'
        }
      ],
      userOptions: [],
      imageRecords: [
        {
          id: 1,
          timestamp: '2024-03-22 10:15:00',
          username: '张三',
          department: '研发部-前端组',
          prompt: '一只可爱的机器猫，科技风格，未来主义，高清细节',
          model: 'DALL-E 3',
          resolution: '1024x1024',
          status: 'saved',
          style: 'realistic',
          remainingDays: 22,
          imageUrl: 'https://images.unsplash.com/photo-1560807707-8cc77767d783'
        },
        {
          id: 2,
          timestamp: '2024-03-22 11:30:00',
          username: '李四',
          department: '研发部-后端组',
          prompt: '未来城市全景，霓虹灯，科幻风格，雨天，高楼大厦',
          model: 'Midjourney',
          resolution: '1792x1024',
          status: 'liked',
          style: 'cartoon',
          remainingDays: 17,
          imageUrl: 'https://images.unsplash.com/photo-1480796927426-f609979314bd'
        },
        {
          id: 3,
          timestamp: '2024-03-22 14:20:00',
          username: '王五',
          department: '产品部',
          prompt: '茵浪AI系统产品展示，高科技，简约设计，专业风格',
          model: 'DALL-E 3',
          resolution: '1024x1024',
          status: 'saved',
          style: 'oil',
          remainingDays: 28,
          imageUrl: 'https://images.unsplash.com/photo-1535223289827-42f1e9919769'
        },
        {
          id: 4,
          timestamp: '2024-03-23 09:45:00',
          username: '赵六',
          department: '市场部',
          prompt: '商业团队讨论AI技术，办公环境，专业人士，现代办公室',
          model: 'Midjourney',
          resolution: '1024x1792',
          status: 'liked',
          style: 'realistic',
          remainingDays: 15,
          imageUrl: 'https://images.unsplash.com/photo-1522071820081-009f0129c71c'
        },
        {
          id: 5,
          timestamp: '2024-03-23 13:10:00',
          username: '张三',
          department: '研发部-前端组',
          prompt: '人工智能芯片特写，高科技，微电子，蓝色光效',
          model: 'DALL-E 3',
          resolution: '1024x1024',
          status: 'deleted',
          style: 'watercolor',
          remainingDays: 24,
          imageUrl: 'https://images.unsplash.com/photo-1518770660439-4636190af475'
        },
        {
          id: 6,
          timestamp: '2024-03-23 15:30:00',
          username: '李四',
          department: '研发部-后端组',
          prompt: '数据中心，服务器机房，科技感，蓝色光效，网络连接',
          model: 'Midjourney',
          resolution: '1024x1024',
          status: 'saved',
          style: 'sketch',
          remainingDays: 30,
          imageUrl: 'https://images.unsplash.com/photo-1558494949-ef010cbdcc31'
        }
      ],
      pickerOptions: {
        shortcuts: [{
          text: '最近一周',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 7);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '最近一个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 30);
            picker.$emit('pick', [start, end]);
          }
        }, {
          text: '最近三个月',
          onClick(picker) {
            const end = new Date();
            const start = new Date();
            start.setTime(start.getTime() - 3600 * 1000 * 24 * 90);
            picker.$emit('pick', [start, end]);
          }
        }]
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
    handleSearch() {
      this.currentPage = 1;
      this.fetchRecords();
    },
    resetFilter() {
      this.filterForm = {
        dateRange: [],
        department: [],
        userId: '',
        prompt: '',
        style: ''
      };
      this.handleSearch();
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.fetchRecords();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.fetchRecords();
    },
    formatDate(row, column, cellValue) {
      return cellValue;
    },
    async remoteSearchUsers(query) {
      if (query) {
        this.userSearchLoading = true;
        // 模拟API调用
        setTimeout(() => {
          this.userOptions = [
            { id: 1, name: '张三', email: 'zhangsan@example.com' },
            { id: 2, name: '李四', email: 'lisi@example.com' },
            { id: 3, name: '王五', email: 'wangwu@example.com' },
            { id: 4, name: '赵六', email: 'zhaoliu@example.com' }
          ].filter(user => user.name.includes(query));
          this.userSearchLoading = false;
        }, 500);
      }
    },
    fetchRecords() {
      this.loading = true;
      // 模拟API调用
      setTimeout(() => {
        // 对图片记录进行过滤
        let filteredRecords = [...this.imageRecords];
        
        // 按提示词筛选
        if (this.filterForm.prompt) {
          filteredRecords = filteredRecords.filter(record => 
            record.prompt.toLowerCase().includes(this.filterForm.prompt.toLowerCase())
          );
        }
        
        // 按用户ID筛选
        if (this.filterForm.userId) {
          const user = this.userOptions.find(u => u.id === this.filterForm.userId);
          if (user) {
            filteredRecords = filteredRecords.filter(record => 
              record.username === user.name
            );
          }
        }
        
        // 按风格筛选
        if (this.filterForm.style) {
          filteredRecords = filteredRecords.filter(record => 
            record.style === this.filterForm.style
          );
        }
        
        // 按创建时间筛选
        if (this.filterForm.dateRange && this.filterForm.dateRange.length === 2) {
          const startDate = new Date(this.filterForm.dateRange[0]);
          const endDate = new Date(this.filterForm.dateRange[1]);
          endDate.setHours(23, 59, 59, 999); // 设置为当天结束时间
          
          filteredRecords = filteredRecords.filter(record => {
            const createTime = new Date(record.timestamp);
            return createTime >= startDate && createTime <= endDate;
          });
        }
        
        this.total = filteredRecords.length;
        this.loading = false;
      }, 500);
    },
    previewImage(record) {
      this.currentRecord = record;
      this.previewVisible = true;
    },
    downloadImage(record) {
      if (!record) return;
      
      // 模拟下载操作
      this.$message.success('图片下载中...');
      // 在实际应用中，这里会调用API将图片作为下载文件提供
    },
    deleteImage(record) {
      this.$confirm(`确定要删除"${record.username}"的这张图片吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 模拟删除操作
        const index = this.imageRecords.findIndex(r => r.id === record.id);
        if (index !== -1) {
          // 在实际应用中应该调用API删除，这里只是将状态改为已删除
          this.$set(this.imageRecords[index], 'status', 'deleted');
          this.$message.success('删除成功');
        }
      }).catch(() => {});
    },
    exportRecords() {
      this.$message({
        message: '导出功能开发中...',
        type: 'info'
      });
    },
    getStatusType(status) {
      const types = {
        'saved': 'success',
        'liked': 'primary',
        'deleted': 'danger'
      };
      return types[status] || 'info';
    },
    getStatusText(status) {
      const texts = {
        'saved': '已保存',
        'liked': '已点赞',
        'deleted': '已删除'
      };
      return texts[status] || status;
    },
    getStyleText(style) {
      const styleMap = {
        'realistic': '写实风格',
        'cartoon': '卡通风格',
        'oil': '油画风格',
        'watercolor': '水彩风格',
        'sketch': '素描风格'
      };
      return styleMap[style] || style;
    },
    handleSelectionChange(selection) {
      this.selectedImages = selection;
    },
    handleBatchDelete() {
      if (this.selectedImages.length === 0) {
        this.$message.warning('请选择要删除的图片');
        return;
      }
      
      this.$confirm(`确认要批量删除选中的 ${this.selectedImages.length} 张图片吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 获取选中行的ID
        const idsToDelete = this.selectedImages.map(image => image.id);
        
        // 从数据中过滤掉要删除的行
        this.imageRecords = this.imageRecords.filter(image => !idsToDelete.includes(image.id));
        
        this.$message.success(`成功删除${idsToDelete.length}张图片`);
        this.selectedImages = [];
      }).catch(() => {
        // 取消删除操作
      });
    },
    resetSaveTime(record) {
      this.$confirm('确认要重置此图片的保存时间吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info'
      }).then(() => {
        // 重置为30天
        record.remainingDays = 30;
        this.$message.success('保存时间已重置为30天');
      }).catch(() => {
        // 取消操作
      });
    }
  },
  created() {
    this.fetchRecords();
  }
}
</script>

<style scoped>
.image-records {
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

.filter-card {
  margin-bottom: 20px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.content-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-controls {
  display: flex;
  gap: 10px;
  align-items: center;
}

.reset-button {
  margin-left: 5px;
  padding: 0;
}

.reset-button i {
  font-size: 14px;
}

.pagination-container {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

.user-email {
  float: right;
  color: #909399;
  font-size: 13px;
}

/* 网格视图样式 */
.image-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 20px;
  margin-top: 20px;
}

.image-item {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  overflow: hidden;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  transition: transform 0.3s;
}

.image-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.image-container {
  height: 0;
  padding-bottom: 100%;
  position: relative;
  overflow: hidden;
  cursor: pointer;
}

.thumbnail {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}

.image-container:hover .thumbnail {
  transform: scale(1.05);
}

.image-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.3s;
}

.image-container:hover .image-overlay {
  opacity: 1;
}

.image-overlay i {
  font-size: 30px;
  color: white;
}

.image-info {
  padding: 12px;
}

.image-prompt {
  font-size: 14px;
  line-height: 1.4;
  margin-bottom: 8px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.image-meta {
  display: flex;
  justify-content: space-between;
  color: #909399;
  font-size: 12px;
  margin-bottom: 8px;
}

.image-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.action-buttons {
  display: flex;
  gap: 8px;
}

/* 自定义样式 */
.danger {
  color: #F56C6C;
}

.danger:hover {
  color: #F78989;
}

/* 预览对话框样式 */
.preview-container {
  display: flex;
  flex-direction: column;
}

.preview-image {
  max-width: 100%;
  max-height: 500px;
  object-fit: contain;
  margin-bottom: 20px;
}

.preview-info {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.info-item {
  width: calc(33.33% - 7px);
  display: flex;
  align-items: center;
}

.full-width {
  width: 100%;
  flex-direction: column;
  align-items: flex-start;
}

.label {
  color: #909399;
  margin-right: 5px;
}

.prompt-text {
  margin-top: 5px;
  background-color: #f5f7fa;
  padding: 10px;
  border-radius: 4px;
  width: 100%;
  line-height: 1.5;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .image-records {
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
  }
  
  .filter-form {
    flex-direction: column;
  }
  
  .filter-form .el-form-item {
    margin-right: 0;
    width: 100%;
  }
  
  .image-grid {
    grid-template-columns: repeat(auto-fill, minmax(150px, 1fr));
    gap: 10px;
  }
  
  .card-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
  
  .info-item {
    width: 100%;
  }
}
</style> 