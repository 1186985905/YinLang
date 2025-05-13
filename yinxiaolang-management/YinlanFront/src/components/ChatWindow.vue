<template>
  <div class="chat-container">
    <div class="background-overlay"></div>
    <div class="chat-wrapper">
      <div class="chat-header">
        <h1>AI 智能助手</h1>
      </div>
      <iframe
        src="http://10.232.84.51/chatbot/0GB0xUtjOyCmPMep"
        class="chat-iframe"
        frameborder="0"
        allow="microphone">
      </iframe>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ChatWindow',
  data() {
    return {
      loading: false
    }
  },
  mounted() {
    // 可以在这里添加 iframe 加载状态处理
    const iframe = document.querySelector('.chat-iframe');
    if (iframe) {
      iframe.onload = () => {
        this.loading = false;
      };
    }
  }
}
</script>

<style scoped>
.chat-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  background-image: url('../assets/TouHo1.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  font-family: "Microsoft YaHei", "PingFang SC", sans-serif;
}

.background-overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
}

.chat-wrapper {
  width: 95%;
  max-width: 1200px;
  height: 95vh;
  position: relative;
  z-index: 1;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 20px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.15);
  backdrop-filter: blur(12px);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 20px 30px;
  background: linear-gradient(135deg, #409EFF, #36D1DC);
  color: white;
  text-align: center;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
}

.chat-header h1 {
  margin: 0;
  font-size: 2.2em;
  font-weight: 600;
  text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.2);
  letter-spacing: 1px;
}

.chat-iframe {
  width: 100%;
  height: calc(100% - 80px);
  min-height: 700px;
  border: none;
  background: white;
}

@media screen and (max-width: 768px) {
  .chat-wrapper {
    width: 100%;
    height: 100vh;
    border-radius: 0;
  }
  
  .chat-header h1 {
    font-size: 1.8em;
  }
  
  .chat-iframe {
    height: calc(100% - 60px);
  }
}
</style> 