<template>
  <el-dialog
    title="偏好设置"
    :visible="visible"
    @close="handleClose"
    width="600px"
    class="preferences-dialog"
  >
    <el-tabs v-model="activeTab">
      <el-tab-pane label="常规设置" name="general">
        <el-form :model="preferences" label-width="100px" size="small">
          <el-form-item label="默认模型">
            <el-select v-model="preferences.defaultModel" placeholder="选择默认模型">
              <el-option label="GPT-4o" value="GPT-4o"></el-option>
              <el-option label="deepseek-v3" value="deepseek-v3"></el-option>
              <el-option label="Qwen2.5-Max" value="Qwen2.5-Max"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="主题">
            <el-radio-group v-model="preferences.theme">
              <el-radio-button label="light">明亮模式</el-radio-button>
              <el-radio-button label="dark">暗黑模式</el-radio-button>
              <el-radio-button label="auto">跟随系统</el-radio-button>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="语言">
            <el-radio-group v-model="preferences.language">
              <el-radio-button label="zh-CN">中文</el-radio-button>
              <el-radio-button label="en-US">English</el-radio-button>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="字体设置" name="font">
        <el-form :model="preferences.font" label-width="100px" size="small">
          <el-form-item label="字体样式">
            <el-select v-model="preferences.font.family" placeholder="选择字体">
              <el-option label="系统默认" value=""></el-option>
              <el-option label="微软雅黑" value="Microsoft YaHei"></el-option>
              <el-option label="宋体" value="SimSun"></el-option>
              <el-option label="黑体" value="SimHei"></el-option>
              <el-option label="Arial" value="Arial"></el-option>
              <el-option label="Roboto" value="Roboto"></el-option>
            </el-select>
          </el-form-item>
          <el-form-item label="字体大小">
            <el-slider
              v-model="preferences.font.size"
              :min="12"
              :max="20"
              :step="1"
              show-stops
              show-input
              :format-tooltip="val => `${val}px`"
            ></el-slider>
          </el-form-item>
          <el-form-item label="预览">
            <div class="font-preview" :style="previewStyle">
              这是字体预览文本。这是一段用于展示所选字体样式和大小效果的示例文本。
            </div>
          </el-form-item>
        </el-form>
      </el-tab-pane>
      
      <el-tab-pane label="常用语" name="prompts">
        <div class="custom-prompts">
          <div class="prompts-header">
            <h3>我的常用语</h3>
            <el-button type="primary" size="small" icon="el-icon-plus" @click="addNewPrompt">添加</el-button>
          </div>
          
          <div class="prompt-list">
            <el-empty v-if="preferences.customPrompts.length === 0" description="暂无常用语"></el-empty>
            <div v-for="(prompt, index) in preferences.customPrompts" :key="index" class="prompt-item">
              <div class="prompt-content">
                <el-input 
                  type="textarea" 
                  v-model="prompt.content" 
                  :rows="2"
                  resize="none"
                  placeholder="输入常用语内容"
                ></el-input>
              </div>
              <div class="prompt-actions">
                <el-button 
                  type="danger" 
                  size="mini" 
                  icon="el-icon-delete" 
                  circle
                  @click="deletePrompt(index)"
                ></el-button>
              </div>
            </div>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
    
    <span slot="footer" class="dialog-footer">
      <el-button @click="cancel">取消</el-button>
      <el-button type="primary" @click="savePreferences">保存</el-button>
    </span>
  </el-dialog>
</template>

<script>
export default {
  name: 'UserPreferences',
  props: {
    visible: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      activeTab: 'general',
      preferences: {
        defaultModel: 'GPT-4o',
        theme: 'light',
        language: 'zh-CN',
        font: {
          family: '',
          size: 14
        },
        customPrompts: []
      }
    }
  },
  computed: {
    previewStyle() {
      return {
        fontFamily: this.preferences.font.family || 'inherit',
        fontSize: `${this.preferences.font.size}px`
      }
    }
  },
  watch: {
    visible(newVal) {
      if (newVal) {
        this.loadPreferences()
      }
    }
  },
  methods: {
    loadPreferences() {
      // 从localStorage加载配置
      const savedPrefs = localStorage.getItem('userPreferences')
      if (savedPrefs) {
        try {
          const parsedPrefs = JSON.parse(savedPrefs)
          this.preferences = { ...this.preferences, ...parsedPrefs }
          
          // 应用字体设置到整个页面
          this.applyFontSettings()
        } catch (e) {
          console.error('Error loading preferences:', e)
        }
      }
    },
    savePreferences() {
      // 保存到localStorage
      localStorage.setItem('userPreferences', JSON.stringify(this.preferences))
      
      // 应用字体设置到整个页面
      this.applyFontSettings()
      
      this.$emit('save', this.preferences)
      this.$emit('update:visible', false)
      
      // 显示保存成功提示
      this.$message({
        message: '偏好设置已保存',
        type: 'success',
        duration: 2000
      })
    },
    applyFontSettings() {
      // 应用字体设置到整个页面
      const fontFamily = this.preferences.font.family || 'inherit'
      const fontSize = this.preferences.font.size || 14
      
      // 创建或更新CSS变量
      document.documentElement.style.setProperty('--app-font-family', fontFamily)
      document.documentElement.style.setProperty('--app-font-size', `${fontSize}px`)
      
      // 为聊天区域和历史对话区域应用字体大小
      const cssStyle = document.getElementById('user-font-style') || document.createElement('style')
      cssStyle.id = 'user-font-style'
      cssStyle.textContent = `
        .chat-messages .message-content,
        .chat-messages .message-text,
        .history-item .history-title,
        .prompt-item,
        .greeting-subtitle,
        .chat-input .el-textarea__inner {
          font-size: ${fontSize}px !important;
          font-family: ${fontFamily} !important;
        }
        
        .message-header .model-name,
        .message-header .response-time,
        .message-header .send-time {
          font-size: ${Math.max(fontSize - 2, 10)}px !important;
        }
        
        .greeting-title {
          font-size: ${fontSize + 6}px !important;
        }
      `
      if (!document.getElementById('user-font-style')) {
        document.head.appendChild(cssStyle)
      }
    },
    cancel() {
      this.$emit('update:visible', false)
    },
    handleClose() {
      this.$emit('update:visible', false)
    },
    addNewPrompt() {
      this.preferences.customPrompts.push({
        content: '',
        createdAt: new Date().toISOString()
      })
    },
    deletePrompt(index) {
      this.preferences.customPrompts.splice(index, 1)
    }
  },
  mounted() {
    // 页面加载时应用已保存的字体设置
    this.loadPreferences()
  }
}
</script>

<style scoped>
.preferences-dialog {
  max-height: 80vh;
}

.static-model-info {
  display: flex;
  align-items: center;
  color: #409eff;
  font-size: 14px;
}

.static-model-info i {
  margin-right: 5px;
}

.font-preview {
  padding: 15px;
  border: 1px solid #eaeaea;
  border-radius: 4px;
  background-color: #f9f9f9;
  margin-bottom: 10px;
  line-height: 1.5;
}

.custom-prompts {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.prompts-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}

.prompts-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.prompt-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.prompt-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  padding: 10px;
  border: 1px solid #eaeaea;
  border-radius: 4px;
  background-color: #f9f9f9;
}

.prompt-content {
  flex: 1;
}

.prompt-actions {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
</style> 