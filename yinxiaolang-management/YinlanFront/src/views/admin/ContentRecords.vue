<template>
  <div class="content-records">
    <h1>
      内容记录查询
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
        <el-form-item label="内容搜索">
          <el-input
            v-model="filterForm.keyword"
            placeholder="请输入关键词"
            clearable
            @keyup.enter.native="handleSearch">
          </el-input>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
          <el-button icon="el-icon-refresh" @click="resetFilter">重置</el-button>
          <el-button type="success" icon="el-icon-download" @click="exportRecords">导出</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 内容记录表格 -->
    <el-card class="table-card">
      <div slot="header" class="card-header">
        <el-tabs v-model="activeTab" type="card">
          <el-tab-pane label="对话记录列表" name="all"></el-tab-pane>
          <el-tab-pane label="已标注对话" name="marked">
            <span slot="label">
              已标注对话
              <el-badge v-if="markedRecords.length > 0" :value="markedRecords.length" class="mark-badge"></el-badge>
            </span>
          </el-tab-pane>
        </el-tabs>
        
        <div class="table-controls">
          <el-button 
            type="danger" 
            size="small" 
            icon="el-icon-delete" 
            :disabled="selectedRecords.length === 0"
            @click="handleBatchDelete">批量删除</el-button>
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="table">表格视图</el-radio-button>
          <el-radio-button label="detail">详细视图</el-radio-button>
        </el-radio-group>
        </div>
      </div>

      <!-- 表格视图 - 所有对话记录 -->
      <template v-if="activeTab === 'all' && viewMode === 'table'">
        <el-table
          :data="recordsList"
          style="width: 100%"
          v-loading="loading"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55"></el-table-column>
          <el-table-column prop="timestamp" label="时间" width="180" :formatter="formatDate"></el-table-column>
          <el-table-column prop="department" label="部门" width="120"></el-table-column>
          <el-table-column prop="username" label="用户" width="120"></el-table-column>
          <el-table-column prop="question" label="问题" show-overflow-tooltip></el-table-column>
          <el-table-column prop="remainingDays" label="保存时间" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.remainingDays }}天</span>
              <el-button type="text" size="mini" @click="resetSaveTime(scope.row)" class="reset-button">
                <i class="el-icon-refresh"></i>
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                @click="viewDetail(scope.row)">
                查看详情
              </el-button>
              <el-button
                size="mini"
                type="text"
                @click="markRecord(scope.row)">
                标注对话
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 表格视图 - 已标注对话 -->
      <template v-if="activeTab === 'marked' && viewMode === 'table'">
        <el-table
          :data="markedRecords"
          style="width: 100%"
          v-loading="loading"
          @selection-change="handleSelectionChange">
          <el-table-column type="selection" width="55"></el-table-column>
          <el-table-column prop="timestamp" label="时间" width="180" :formatter="formatDate"></el-table-column>
          <el-table-column prop="department" label="部门" width="120"></el-table-column>
          <el-table-column prop="username" label="用户" width="120"></el-table-column>
          <el-table-column prop="question" label="问题" show-overflow-tooltip></el-table-column>
          <el-table-column prop="markComment" label="标注内容" show-overflow-tooltip></el-table-column>
          <el-table-column prop="remainingDays" label="保存时间" width="120">
            <template slot-scope="scope">
              <span>{{ scope.row.remainingDays }}天</span>
              <el-button type="text" size="mini" @click="resetSaveTime(scope.row)" class="reset-button">
                <i class="el-icon-refresh"></i>
              </el-button>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="180" fixed="right">
            <template slot-scope="scope">
              <el-button
                size="mini"
                type="text"
                @click="viewDetail(scope.row)">
                查看详情
              </el-button>
              <el-button
                size="mini"
                type="text"
                @click="notifyUser(scope.row)">
                <i class="el-icon-bell"></i>
                提醒用户
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </template>

      <!-- 详细视图 - 所有对话记录 -->
      <template v-if="activeTab === 'all' && viewMode === 'detail'">
        <div class="detail-list">
          <div v-for="record in recordsList" :key="record.id" class="detail-item">
            <div class="detail-header">
              <div class="detail-info">
                <span class="time">{{ formatDate(null, null, record.timestamp) }}</span>
                <el-tag size="small" type="info">{{ record.department }}</el-tag>
                <span class="username">{{ record.username }}</span>
              </div>
              <div class="detail-actions">
                <el-button type="text" @click="copyContent(record)">复制内容</el-button>
                <el-button type="text" @click="markRecord(record)">标注对话</el-button>
              </div>
            </div>
            <div class="conversation">
              <div class="question">
                <div class="avatar user">用户</div>
                <div class="content">{{ record.question }}</div>
              </div>
              <div class="answer">
                <div class="avatar assistant">AI</div>
                <div class="content">{{ record.answer }}</div>
              </div>
            </div>
          </div>
        </div>
      </template>

      <!-- 详细视图 - 已标注对话 -->
      <template v-if="activeTab === 'marked' && viewMode === 'detail'">
        <div class="detail-list">
          <div v-for="record in markedRecords" :key="record.id" class="detail-item">
            <div class="detail-header">
              <div class="detail-info">
                <span class="time">{{ formatDate(null, null, record.timestamp) }}</span>
                <el-tag size="small" type="info">{{ record.department }}</el-tag>
                <span class="username">{{ record.username }}</span>
              </div>
              <div class="detail-actions">
              <el-button type="text" @click="copyContent(record)">复制内容</el-button>
                <el-button type="text" @click="notifyUser(record)">
                  <i class="el-icon-bell"></i>
                  提醒用户
                </el-button>
              </div>
            </div>
            <div class="mark-comment">
              <i class="el-icon-warning-outline"></i>
              <span>标注内容：{{ record.markComment }}</span>
            </div>
            <div class="conversation">
              <div class="question">
                <div class="avatar user">用户</div>
                <div class="content">{{ record.question }}</div>
              </div>
              <div class="answer">
                <div class="avatar assistant">AI</div>
                <div class="content">{{ record.answer }}</div>
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
          :page-sizes="[10, 20, 50, 100]"
          :page-size="pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="total">
        </el-pagination>
      </div>
    </el-card>

    <!-- 详情对话框 -->
    <el-dialog
      title="对话详情"
      :visible.sync="dialogVisible"
      width="600px"
      custom-class="detail-dialog">
      <template v-if="currentRecord">
        <div class="dialog-header-info">
          <div class="info-item">
            <span class="label">时间：</span>
            <span>{{ formatDate(null, null, currentRecord.timestamp) }}</span>
          </div>
          <div class="info-item">
            <span class="label">部门：</span>
            <span>{{ currentRecord.department }}</span>
          </div>
          <div class="info-item">
            <span class="label">用户：</span>
            <span>{{ currentRecord.username }}</span>
          </div>
          <div class="info-item" v-if="currentRecord.isMarked">
            <span class="label">标注内容：</span>
            <span>{{ currentRecord.markComment }}</span>
          </div>
        </div>
        <div class="dialog-conversation">
          <div class="dialog-message user">
            <div class="message-header">问题：</div>
            <div class="message-content">{{ currentRecord.question }}</div>
          </div>
          <div class="dialog-message assistant">
            <div class="message-header">回答：</div>
            <div class="message-content">{{ currentRecord.answer }}</div>
          </div>
        </div>
      </template>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">关闭</el-button>
        <template v-if="currentRecord && !currentRecord.isMarked">
          <el-button type="primary" @click="markRecord(currentRecord)">标注对话</el-button>
        </template>
        <template v-else-if="currentRecord && currentRecord.isMarked">
          <el-button type="warning" @click="notifyUser(currentRecord)">
            <i class="el-icon-bell"></i> 提醒用户
          </el-button>
        </template>
      </div>
    </el-dialog>

    <!-- 标注对话框 -->
    <el-dialog
      title="标注对话"
      :visible.sync="markDialogVisible"
      width="500px">
      <template v-if="currentRecord">
        <div class="mark-info">
          <div>用户：{{ currentRecord.username }}</div>
          <div>问题：{{ currentRecord.question }}</div>
        </div>
        <el-form :model="markForm" ref="markForm" label-width="80px">
          <el-form-item label="标注内容" prop="comment" required>
            <el-input
              type="textarea"
              v-model="markForm.comment"
              :rows="4"
              placeholder="请输入标注内容，说明问题所在...">
            </el-input>
          </el-form-item>
        </el-form>
      </template>
      <div slot="footer" class="dialog-footer">
        <el-button @click="markDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitMark">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
export default {
  name: 'ContentRecords',
  data() {
    return {
      loading: false,
      userSearchLoading: false,
      viewMode: 'table',
      activeTab: 'all',
      currentPage: 1,
      pageSize: 10,
      total: 0,
      dialogVisible: false,
      markDialogVisible: false,
      currentRecord: null,
      markForm: {
        comment: ''
      },
      filterForm: {
        dateRange: [],
        department: [],
        userId: '',
        keyword: ''
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
      recordsList: [
        {
          id: 1,
          timestamp: '2024-03-20 10:30:00',
          department: '研发部-前端组',
          username: '张三',
          question: '如何优化React组件的性能？',
          answer: '优化React组件性能可以从以下几个方面入手：1. 使用React.memo()避免不必要的重渲染 2. 合理使用useMemo和useCallback 3. 使用虚拟列表处理长列表 4. 避免内联对象和函数...',
          isMarked: false,
          markComment: '',
          remainingDays: 25
        },
        {
          id: 2,
          timestamp: '2024-03-20 11:15:00',
          department: '研发部-后端组',
          username: '李四',
          question: '如何处理Node.js中的内存泄漏问题？',
          answer: '处理Node.js内存泄漏可以：1. 使用heapdump分析内存快照 2. 避免闭包造成的引用无法释放 3. 及时清理定时器和事件监听器 4. 使用WeakMap/WeakSet处理对象引用...',
          isMarked: false,
          markComment: '',
          remainingDays: 18
        },
        {
          id: 3,
          timestamp: '2024-03-20 14:05:00',
          department: '产品部',
          username: '王五',
          question: '茵浪AI系统与其他AI产品相比有什么优势？',
          answer: '茵浪AI系统的主要优势包括：1. 支持多种AI模型集成，可根据需求灵活切换 2. 针对企业内部知识库深度优化，提供更准确的答案 3. 完善的权限管理和数据安全机制 4. 支持自定义训练和微调，更贴合特定行业需求...',
          isMarked: false,
          markComment: '',
          remainingDays: 22
        },
        {
          id: 4,
          timestamp: '2024-03-21 09:20:00',
          department: '市场部',
          username: '赵六',
          question: '如何利用AI改进我们的市场营销策略？',
          answer: 'AI可以通过以下方式改进市场营销：1. 精准客户分群，提供个性化内容推荐 2. 预测市场趋势和消费者行为 3. 智能内容生成，提高创意效率 4. 全渠道数据分析，优化营销投入...',
          isMarked: false,
          markComment: '',
          remainingDays: 30
        }
      ],
      markedRecords: [],
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
      },
      selectedRecords: []
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
        keyword: ''
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
    viewDetail(record) {
      this.currentRecord = record;
      this.dialogVisible = true;
    },
    copyContent(record) {
      const content = `问题：${record.question}\n\n回答：${record.answer}`;
      navigator.clipboard.writeText(content).then(() => {
        this.$message.success('内容已复制到剪贴板');
      }).catch(() => {
        this.$message.error('复制失败，请手动复制');
      });
    },
    async remoteSearchUsers(query) {
      if (query) {
        this.userSearchLoading = true;
        // 模拟API调用
        setTimeout(() => {
          this.userOptions = [
            { id: 1, name: '张三', email: 'zhangsan@example.com' },
            { id: 2, name: '李四', email: 'lisi@example.com' }
          ].filter(user => user.name.includes(query));
          this.userSearchLoading = false;
        }, 500);
      }
    },
    fetchRecords() {
      this.loading = true;
      // 模拟API调用
      setTimeout(() => {
        this.total = this.recordsList.length;
        this.loading = false;
      }, 500);
    },
    exportRecords() {
      this.$message({
        message: '导出功能开发中...',
        type: 'info'
      });
    },
    markRecord(record) {
      this.currentRecord = record;
      this.markForm.comment = record.markComment || '';
      this.markDialogVisible = true;
    },
    submitMark() {
      if (!this.markForm.comment.trim()) {
        this.$message.warning('请输入标注内容');
        return;
      }

      // 查找记录并标记
      const index = this.recordsList.findIndex(r => r.id === this.currentRecord.id);
      if (index !== -1) {
        const updatedRecord = {
          ...this.recordsList[index],
          isMarked: true,
          markComment: this.markForm.comment
        };
        
        // 更新记录列表中的项
        this.$set(this.recordsList, index, updatedRecord);
        
        // 检查是否已经在已标注列表中
        const markedIndex = this.markedRecords.findIndex(r => r.id === updatedRecord.id);
        if (markedIndex !== -1) {
          // 更新已存在的标注记录
          this.$set(this.markedRecords, markedIndex, updatedRecord);
        } else {
          // 添加到已标注列表
          this.markedRecords.push(updatedRecord);
        }
        
        this.$message.success('标注成功');
        this.markDialogVisible = false;
      }
    },
    notifyUser(record) {
      this.$confirm(`确定要提醒用户"${record.username}"关于此对话的问题吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        this.$message({
          type: 'success',
          message: `已成功提醒用户"${record.username}"！`
        });
      }).catch(() => {});
    },
    handleSelectionChange(selection) {
      this.selectedRecords = selection;
    },
    handleBatchDelete() {
      if (this.selectedRecords.length === 0) {
        this.$message.warning('请选择要删除的记录');
        return;
      }
      
      this.$confirm(`确认要批量删除选中的 ${this.selectedRecords.length} 条记录吗？`, '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        // 获取选中行的ID
        const idsToDelete = this.selectedRecords.map(record => record.id);
        
        // 从数据中过滤掉要删除的行
        if (this.activeTab === 'all') {
          this.recordsList = this.recordsList.filter(record => !idsToDelete.includes(record.id));
        } else {
          this.markedRecords = this.markedRecords.filter(record => !idsToDelete.includes(record.id));
        }
        
        this.$message.success(`成功删除${idsToDelete.length}条记录`);
        this.selectedRecords = [];
      }).catch(() => {
        // 取消删除操作
      });
    },
    resetSaveTime(record) {
      this.$confirm('确认要重置此对话的保存时间吗？', '提示', {
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
.content-records {
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

.table-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
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

/* 详细视图样式 */
.detail-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.detail-item {
  border: 1px solid #EBEEF5;
  border-radius: 4px;
  padding: 15px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.detail-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.time {
  color: #909399;
}

.username {
  font-weight: 500;
}

.conversation {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.question, .answer {
  display: flex;
  gap: 10px;
}

.avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  font-size: 14px;
  flex-shrink: 0;
}

.avatar.user {
  background-color: #409EFF;
}

.avatar.assistant {
  background-color: #67C23A;
}

.content {
  flex: 1;
  padding: 12px;
  background: #F5F7FA;
  border-radius: 4px;
  line-height: 1.5;
}

/* 详情对话框样式 */
.detail-dialog {
  .dialog-header-info {
    margin-bottom: 20px;
    padding-bottom: 15px;
    border-bottom: 1px solid #EBEEF5;
  }

  .info-item {
    margin-bottom: 8px;
    
    .label {
      color: #909399;
      margin-right: 10px;
    }
  }

  .dialog-conversation {
    display: flex;
    flex-direction: column;
    gap: 15px;
  }

  .dialog-message {
    .message-header {
      font-weight: 500;
      margin-bottom: 8px;
    }

    .message-content {
      background: #F5F7FA;
      padding: 12px;
      border-radius: 4px;
      line-height: 1.5;
    }
  }
}

@media screen and (max-width: 768px) {
  .content-records {
    padding: 10px;
  }

  .filter-form {
    flex-direction: column;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 10px;
    width: 100%;
  }

  .el-date-picker, .el-cascader, .el-select {
    width: 100%;
  }

  .detail-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}

.mark-badge >>> .el-badge__content {
  background-color: #F56C6C;
}

.mark-comment {
  background-color: #FEF0F0;
  padding: 10px;
  border-radius: 4px;
  margin-bottom: 15px;
  color: #F56C6C;
  font-size: 14px;
  display: flex;
  align-items: center;
  gap: 5px;
}

.mark-info {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #F5F7FA;
  border-radius: 4px;
  line-height: 1.5;
}

.detail-actions {
  display: flex;
  gap: 10px;
}

.table-controls {
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
</style> 