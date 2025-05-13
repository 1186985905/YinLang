<template>
  <div class="model-card" :class="{ 'is-experimental': status === 'experimental' }">
    <div class="card-header">
      <h3>{{ title }}</h3>
      <el-tag 
        :type="status === 'stable' ? 'success' : 'warning'"
        size="small"
      >
        {{ status === 'stable' ? '稳定版' : '实验版' }}
      </el-tag>
    </div>
    <p class="description">{{ description }}</p>
  </div>
</template>

<script>
export default {
  name: 'AppModelCard',
  props: {
    title: {
      type: String,
      required: true
    },
    status: {
      type: String,
      default: 'stable',
      validator: value => ['stable', 'experimental'].includes(value)
    },
    description: {
      type: String,
      required: true
    }
  }
}
</script>

<style scoped>
.model-card {
  padding: 20px;
  border-radius: 8px;
  background: white;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 16px;
  transition: all 0.3s;
}

.model-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px 0 rgba(0, 0, 0, 0.15);
}

.card-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;
}

.card-header h3 {
  margin: 0;
  font-size: 16px;
  color: #303133;
}

.description {
  margin: 0;
  font-size: 14px;
  color: #606266;
  line-height: 1.5;
}

.is-experimental {
  border-left: 4px solid #E6A23C;
}
</style> 