<template>
  <el-dialog
    title="共享历史记录"
    :visible.sync="localVisible"
    width="500px"
    class="share-dialog"
  >
    <div class="share-content">
      <p class="share-text">共享链接: {{ shareLink }}</p>
      <el-button type="primary" @click="copyLink">复制链接</el-button>
    </div>
  </el-dialog>
</template>

<script>
export default {
  name: 'ShareDialog',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    shareLink: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      localVisible: this.visible
    }
  },
  watch: {
    visible(val) {
      this.localVisible = val;
    },
    localVisible(val) {
      this.$emit('update:visible', val);
      if (!val) {
        this.$emit('close');
      }
    }
  },
  methods: {
    copyLink() {
      // 复制链接到剪贴板
      const input = document.createElement('input');
      input.value = this.shareLink;
      document.body.appendChild(input);
      input.select();
      document.execCommand('copy');
      document.body.removeChild(input);
      
      this.$message({
        message: '链接已复制到剪贴板',
        type: 'success',
        duration: 2000
      });
    }
  }
}
</script>

<style scoped>
.share-dialog {
  text-align: center;
}

.share-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
  padding: 10px;
}

.share-text {
  font-size: 16px;
  word-break: break-all;
  max-width: 100%;
  margin: 0;
}
</style> 