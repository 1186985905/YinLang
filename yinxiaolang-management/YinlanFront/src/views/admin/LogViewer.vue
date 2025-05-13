<template>
  <div class="log-viewer">
    <h1>
      系统日志查看
      <div class="header-buttons">
        <el-button type="default" size="small" icon="el-icon-back" @click="goBack">返回管理面板</el-button>
        <el-button type="primary" size="small" icon="el-icon-s-home" @click="goHome">返回主页</el-button>
      </div>
    </h1>

    <el-card class="filter-card">
      <div class="filter-section">
        <el-form :inline="true" :model="filterForm" class="filter-form">
          <el-form-item label="时间范围">
            <el-date-picker
              v-model="filterForm.dateRange"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              :picker-options="pickerOptions"
              @change="handleDateChange">
            </el-date-picker>
          </el-form-item>
          <el-form-item label="用户名">
            <el-input
              v-model="filterForm.username"
              placeholder="请输入用户名"
              clearable
              @clear="handleFilter"
              @keyup.enter.native="handleFilter">
            </el-input>
          </el-form-item>
          <el-form-item label="操作类型">
            <el-select v-model="filterForm.actionType" placeholder="请选择" clearable @change="handleFilter">
              <el-option label="登录/登出" value="login_logout"></el-option>
              <el-option label="文本对话" value="text_chat"></el-option>
              <el-option label="图像生成" value="image_generation"></el-option>
              <el-option label="模型配置" value="model_config"></el-option>
              <el-option label="用户管理" value="user_management"></el-option>
              <el-option label="对话记录删除" value="chat_delete"></el-option>
              <el-option label="图片删除" value="image_delete"></el-option>
              <el-option label="提示词配置" value="prompt_config"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleFilter">搜索</el-button>
            <el-button icon="el-icon-refresh" @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-card>

    <el-card class="log-table-card">
      <div slot="header" class="card-header">
        <span>操作日志列表</span>
        <div class="header-actions">
          <el-button type="primary" size="small" icon="el-icon-download" @click="exportLogs">导出日志</el-button>
        </div>
      </div>
      
      <el-table
        :data="logData"
        style="width: 100%"
        v-loading="loading"
        border>
        <el-table-column
          prop="timestamp"
          label="时间"
          width="180"
          :formatter="formatDate">
        </el-table-column>
        <el-table-column
          prop="username"
          label="用户名"
          width="150">
        </el-table-column>
        <el-table-column
          prop="action"
          label="操作"
          width="100">
          <template slot-scope="scope">
            <el-tag :type="getActionTagType(scope.row.action)">
              {{ getActionName(scope.row.action) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="ip"
          label="IP地址"
          width="150">
        </el-table-column>
        <el-table-column
          prop="device"
          label="设备信息">
        </el-table-column>
      </el-table>

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
  </div>
</template>

<script>
export default {
  name: 'LogViewer',
  data() {
    return {
      loading: false,
      filterForm: {
        dateRange: [],
        username: '',
        actionType: ''
      },
      currentPage: 1,
      pageSize: 10,
      total: 0,
      logData: [
        {
          id: 1,
          timestamp: '2024-03-20 09:30:00',
          username: 'admin',
          action: 'login',
          ip: '192.168.1.100',
          device: 'Chrome 122.0.0 / Windows 10'
        },
        {
          id: 2,
          timestamp: '2024-03-20 10:15:00',
          username: 'user1',
          action: 'text_chat',
          ip: '192.168.1.101',
          device: 'Safari / MacOS'
        },
        {
          id: 3,
          timestamp: '2024-03-20 11:20:00',
          username: 'user2',
          action: 'image_generation',
          ip: '192.168.1.102',
          device: 'Firefox / Ubuntu'
        },
        {
          id: 4,
          timestamp: '2024-03-20 12:45:00',
          username: 'admin',
          action: 'model_add',
          ip: '192.168.1.100',
          device: 'Chrome 122.0.0 / Windows 10'
        },
        {
          id: 5,
          timestamp: '2024-03-20 13:30:00',
          username: 'admin',
          action: 'user_update',
          ip: '192.168.1.100',
          device: 'Chrome 122.0.0 / Windows 10'
        },
        {
          id: 6,
          timestamp: '2024-03-20 15:20:00',
          username: 'admin',
          action: 'logout',
          ip: '192.168.1.100',
          device: 'Chrome 122.0.0 / Windows 10'
        },
        {
          id: 7,
          timestamp: '2024-03-20 16:10:00',
          username: 'user1',
          action: 'prompt_config',
          ip: '192.168.1.101',
          device: 'Safari / MacOS'
        },
        {
          id: 8,
          timestamp: '2024-03-20 17:00:00',
          username: 'user2',
          action: 'image_delete',
          ip: '192.168.1.102',
          device: 'Firefox / Ubuntu'
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
      this.$router.push('/admin');
    },
    goHome() {
      this.$router.push('/');
    },
    formatDate(row) {
      return row.timestamp;
    },
    handleFilter() {
      this.currentPage = 1;
      this.fetchLogs();
    },
    resetFilter() {
      this.filterForm = {
        dateRange: [],
        username: '',
        actionType: ''
      };
      this.handleFilter();
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.fetchLogs();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.fetchLogs();
    },
    handleDateChange() {
      this.handleFilter();
    },
    fetchLogs() {
      this.loading = true;
      
      // 模拟API调用
      setTimeout(() => {
        let filteredLogs = [...this.logData];
        
        if (this.filterForm.username) {
          filteredLogs = filteredLogs.filter(log => 
            log.username.toLowerCase().includes(this.filterForm.username.toLowerCase())
          );
        }
        
        if (this.filterForm.actionType) {
          // 处理合并后的操作类型过滤
          if (this.filterForm.actionType === 'login_logout') {
            filteredLogs = filteredLogs.filter(log => 
              log.action === 'login' || log.action === 'logout'
            );
          } else if (this.filterForm.actionType === 'model_config') {
            filteredLogs = filteredLogs.filter(log => 
              log.action === 'model_add' || log.action === 'model_update' || log.action === 'model_delete'
            );
          } else if (this.filterForm.actionType === 'user_management') {
            filteredLogs = filteredLogs.filter(log => 
              log.action === 'user_add' || log.action === 'user_update' || log.action === 'user_delete'
            );
          } else {
            filteredLogs = filteredLogs.filter(log => log.action === this.filterForm.actionType);
          }
        }
        
        if (this.filterForm.dateRange && this.filterForm.dateRange.length === 2) {
          const startDate = new Date(this.filterForm.dateRange[0]);
          const endDate = new Date(this.filterForm.dateRange[1]);
          endDate.setHours(23, 59, 59, 999); // 设置为当天结束时间
          
          filteredLogs = filteredLogs.filter(log => {
            const logDate = new Date(log.timestamp);
            return logDate >= startDate && logDate <= endDate;
          });
        }
        
        this.total = filteredLogs.length;
        this.logData = filteredLogs;
        this.loading = false;
      }, 500);
    },
    exportLogs() {
      // 实现导出日志功能
      this.$message({
        message: '日志导出功能开发中...',
        type: 'info'
      });
    },
    getActionTagType(action) {
      const types = {
        login: 'success',
        logout: 'success',
        text_chat: 'primary',
        image_generation: 'warning',
        model_add: 'success',
        model_update: 'warning',
        model_delete: 'danger',
        user_add: 'success',
        user_update: 'warning',
        user_delete: 'danger',
        chat_delete: 'danger',
        image_delete: 'danger',
        prompt_config: 'info'
      };
      return types[action] || 'info';
    },
    getActionName(action) {
      const names = {
        login: '登录/登出',
        logout: '登录/登出',
        text_chat: '文本对话',
        image_generation: '图像生成',
        model_add: '模型配置',
        model_update: '模型配置',
        model_delete: '模型配置',
        user_add: '用户管理',
        user_update: '用户管理',
        user_delete: '用户管理',
        chat_delete: '对话记录删除',
        image_delete: '图片删除',
        prompt_config: '提示词配置'
      };
      return names[action] || action;
    }
  },
  created() {
    this.fetchLogs();
  }
}
</script>

<style scoped>
.log-viewer {
  padding: 20px;
  max-width: 1200px;
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

.filter-section {
  padding: 10px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.log-table-card {
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

@media screen and (max-width: 768px) {
  .log-viewer {
    padding: 10px;
  }

  .filter-form {
    flex-direction: column;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 10px;
  }

  .el-date-picker {
    width: 100%;
  }
}
</style> 