<template>
  <a-card class="app-card" hoverable @click="handleView">
    <!-- 应用封面 -->
    <template #cover>
      <div class="cover-wrapper">
        <img v-if="app.cover" :src="app.cover" :alt="app.appName" class="cover-img" />
        <div v-else class="cover-placeholder">
          <span class="placeholder-text">{{ app.appName }}</span>
        </div>
      </div>
    </template>
    <!-- 应用信息 -->
    <a-card-meta :title="app.appName">
      <template #description>
        <div class="app-info">
          <div class="author">
            <a-avatar :size="24" :src="app.user?.userAvatar" />
            <span class="author-name">{{ app.user?.userName ?? '匿名用户' }}</span>
          </div>
          <span class="create-time">{{ formatRelativeTime(app.createTime) }}</span>
        </div>
      </template>
    </a-card-meta>
    <!-- 悬浮操作按钮 -->
    <div class="card-actions" @click.stop>
      <a-button type="primary" size="small" @click="handleView">查看对话</a-button>
      <a-button v-if="app.deployKey" size="small" @click="handlePreview">查看作品</a-button>
    </div>
  </a-card>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { formatRelativeTime } from '@/utils/time'
import { getDeployUrl } from '@/config/env'

interface Props {
  app: API.AppVO
}

const props = defineProps<Props>()
const router = useRouter()

// 跳转到应用对话页
const handleView = () => {
  router.push(`/app/chat/${props.app.id}`)
}

// 查看已部署的作品
const handlePreview = () => {
  if (props.app.deployKey) {
    window.open(getDeployUrl(props.app.deployKey), '_blank')
  }
}
</script>

<style scoped>
.app-card {
  overflow: hidden;
  transition: transform 0.2s;
}

.app-card:hover {
  transform: translateY(-4px);
}

.app-card:hover .card-actions {
  opacity: 1;
}

.cover-wrapper {
  position: relative;
  height: 180px;
  overflow: hidden;
  background: #f5f5f5;
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
  background: linear-gradient(135deg, #e6f4ff 0%, #d6f7f0 100%);
}

.placeholder-text {
  font-size: 18px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.45);
}

.app-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.author {
  display: flex;
  align-items: center;
  gap: 6px;
}

.author-name {
  font-size: 13px;
  color: rgba(0, 0, 0, 0.65);
}

.create-time {
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}

.card-actions {
  position: absolute;
  top: 12px;
  right: 12px;
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.2s;
}
</style>
