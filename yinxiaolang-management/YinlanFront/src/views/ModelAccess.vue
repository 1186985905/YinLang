<template>
  <div class="model-access">
    <div class="page-header">
      <h2>模型接入</h2>
      <p class="subtitle">选择适合您需求的模型版本</p>
    </div>

    <!-- 模型卡片列表 -->
    <div class="model-list">
      <ModelCard
        v-for="model in models"
        :key="model.id"
        :title="model.title"
        :status="model.status"
        :description="model.desc"
      />
    </div>

    <!-- 升级按钮 -->
    <el-button type="primary" class="upgrade-btn">
      Gemini Advanced 升级
    </el-button>
  </div>
</template>

<script>
import ModelCard from "@/components/ModelCard.vue";

export default {
  name: "ModelAccessView",
  components: {
    ModelCard,
  },
  data() {
    return {
      models: [
        {
          id: 1,
          title: "2.0 Flash",
          status: "stable",
          desc: "获得日常帮助",
        },
        {
          id: 2,
          title: "2.5 Pro (experimental)",
          status: "experimental",
          desc: "处理复杂任务效果最好",
        },
      ],
    };
  },
};
</script>

<style scoped>
.model-access {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.page-header {
  margin-bottom: 30px;
}

.page-header h2 {
  margin: 0;
  font-size: 24px;
  color: #303133;
}

.subtitle {
  margin: 8px 0 0;
  color: #909399;
  font-size: 14px;
}

.model-list {
  margin-bottom: 30px;
}

.upgrade-btn {
  width: 100%;
  padding: 12px 0;
  font-size: 16px;
  border-radius: 8px;
}

@media screen and (max-width: 768px) {
  .model-access {
    padding: 15px;
  }

  .page-header h2 {
    font-size: 20px;
  }
}
</style> 