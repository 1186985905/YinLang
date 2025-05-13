<template>
  <div class="admin-panel">
    <div class="admin-header">
      <h1 class="admin-title">管理员权限面板</h1>
      <el-button
        type="primary"
        size="small"
        icon="el-icon-back"
        @click="backToHome"
        class="back-btn"
        >返回主页</el-button
      >
    </div>

    <el-card class="admin-description">
      <div slot="header" class="card-header">
        <h3>管理员权限范围</h3>
      </div>
      <el-table :data="adminRights" style="width: 100%" size="small">
        <el-table-column prop="role" label="角色" width="120"></el-table-column>
        <el-table-column prop="permissions" label="权限范围"></el-table-column>
      </el-table>
    </el-card>

    <div class="admin-functions">
      <h2>系统管理功能</h2>
      <div class="feature-grid">
        <el-card 
          v-for="(feature, index) in adminFeatures" 
          :key="index"
          class="feature-card" 
          shadow="hover"
        >
          <div class="card-content">
            <div class="feature-icon">
              <i :class="feature.icon"></i>
            </div>
            <div class="feature-info">
              <h3>{{ feature.title }}</h3>
              <p>{{ feature.description }}</p>
              <el-button
                type="primary"
                size="small"
                @click="navigateToFeature(feature.route)"
                >进入管理</el-button
              >
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "AdminPanel",
  data() {
    return {
      adminRights: [
        {
          role: "管理员",
          permissions: "模型配置，用户管理，部门管理，日志查看，提示词配置，内容记录查询，图片记录查询",
        },
      ],
      adminFeatures: [
        {
          title: "模型配置",
          icon: "el-icon-cpu",
          description: "管理系统可用的AI模型、接口配置和调用参数",
          route: "/admin/model-config",
        },
        {
          title: "用户管理",
          icon: "el-icon-user",
          description: "管理系统用户账号和部门分配",
          route: "/admin/user-management",
        },
        {
          title: "部门管理",
          icon: "el-icon-office-building",
          description: "管理系统部门结构和信息",
          route: "/admin/department-management",
        },
        {
          title: "日志查看",
          icon: "el-icon-document",
          description: "查看系统操作日志和用户使用记录",
          route: "/admin/logs",
        },
        {
          title: "提示词配置",
          icon: "el-icon-chat-line-square",
          description: "管理系统预设提示词和对话模板",
          route: "/admin/prompt-config",
        },
        {
          title: "内容记录查询",
          icon: "el-icon-search",
          description: "查询和审核系统内的对话内容",
          route: "/admin/content-records",
        },
        {
          title: "图片记录查询",
          icon: "el-icon-picture-outline",
          description: "查询和管理用户生成的图片内容",
          route: "/admin/image-records",
        },
      ],
    };
  },
  methods: {
    navigateToFeature(route) {
      if (this.$route.path !== route) {
        this.$router.push(route);
      }
    },
    backToHome() {
      this.$router.push("/");
    },
  },
};
</script>

<style scoped>
.admin-panel {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.admin-title {
  font-size: 24px;
  color: #333;
  margin: 0;
}

.admin-description {
  margin-bottom: 20px;
}

.admin-functions {
  margin-top: 30px;
}

.admin-functions h2 {
  font-size: 20px;
  color: #333;
  margin-bottom: 20px;
}

.feature-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.feature-card {
  height: 100%;
  transition: transform 0.3s;
}

.feature-card:hover {
  transform: translateY(-5px);
}

.card-content {
  display: flex;
  align-items: flex-start;
  gap: 15px;
}

.feature-icon {
  font-size: 24px;
  color: #409EFF;
  padding: 15px;
  background-color: #ecf5ff;
  border-radius: 8px;
  min-width: 24px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 5px;
}

.feature-info {
  flex: 1;
}

.feature-info h3 {
  font-size: 18px;
  color: #333;
  margin: 0 0 10px 0;
}

.feature-info p {
  color: #666;
  font-size: 14px;
  margin: 0 0 15px 0;
}

/* 移动端适配 */
@media screen and (max-width: 768px) {
  .admin-panel {
    padding: 10px;
  }

  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .admin-title {
    font-size: 20px;
  }
  
  .feature-grid {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .card-content {
    flex-direction: column;
    align-items: center;
    text-align: center;
  }
  
  .feature-icon {
    margin-bottom: 10px;
  }

  .feature-info h3 {
    font-size: 16px;
  }

  .feature-info p {
    font-size: 13px;
  }
}
</style> 