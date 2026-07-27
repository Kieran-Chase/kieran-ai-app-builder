<script setup lang="ts">
import { ref, reactive, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import {
  SendOutlined,
  CloudUploadOutlined,
  InfoCircleOutlined,
  EditOutlined,
  DeleteOutlined,
  DownloadOutlined,
} from '@ant-design/icons-vue'
import { getAppVoById, deployApp, deleteApp, downloadAppCode } from '@/api/appController.ts'
import { listAppChatHistory } from '@/api/chatHistoryController.ts'
import useVisualEditor from '@/composables/useVisualEditor'
import { formatElementInfoForDisplay, formatElementInfoForPrompt } from '@/utils/elementSerializer'
import { formatTime } from '@/utils/time'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { API_BASE_URL, getStaticPreviewUrl } from '@/config/env'
import { renderMarkdown } from '@/utils/markdown'
import { toAppId } from '@/utils/appId'
import { CODE_GEN_TYPE_MAP } from '@/constant/app'
import 'highlight.js/styles/atom-one-light.css'

interface ChatMessage {
  role: 'user' | 'ai'
  content: string
  loading?: boolean
  /** 消息创建时间，来自后端；用于游标分页。新发送的消息无此字段 */
  createTime?: string
}

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用 id
const appId = computed(() => route.params.id as string)
// 应用信息
const app = ref<API.AppVO>()

// 应用详情弹窗
const detailModalOpen = ref(false)
// 生成类型展示文本
const codeGenTypeText = computed(() => {
  const codeGenType = app.value?.codeGenType
  if (!codeGenType) {
    return '未知'
  }
  return CODE_GEN_TYPE_MAP[codeGenType] ?? codeGenType
})
// 是否有权修改/删除（本人或管理员）
const canEdit = computed(() => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser.id || !app.value) {
    return false
  }
  return app.value.userId === loginUser.id || loginUser.userRole === 'admin'
})
// 是否为应用所有者
const isOwner = computed(() => {
  const loginUser = loginUserStore.loginUser
  if (!loginUser.id || !app.value) {
    return false
  }
  return app.value.userId === loginUser.id
})

// 消息列表（按时间升序展示）
const messages = ref<ChatMessage[]>([])
// 是否还有更早的历史消息（游标分页）
const hasMore = ref(false)
// 是否正在加载更早的消息
const loadingMore = ref(false)
// 历史消息是否已加载完成（首次加载）
const historyLoaded = ref(false)
// 用户输入
const userInput = ref('')
// AI 是否正在生成
const generating = ref(false)
// 消息区域引用（用于自动滚动）
const messagesRef = ref<HTMLElement>()
// 预览 iframe 引用（用于可视化编辑）
const previewIframeRef = ref<HTMLIFrameElement | null>(null)

const {
  visualEditEnabled,
  selectedElement,
  enterEditMode,
  exitEditMode,
  clearSelection,
  handlePreviewLoad,
} = useVisualEditor(previewIframeRef)
const selectedElementDescription = computed(() =>
  selectedElement.value ? formatElementInfoForDisplay(selectedElement.value) : '',
)

// 网页预览地址（生成完成后展示）
const previewUrl = ref('')
// 下载相关
const downloading = ref(false)
// 部署相关
const deploying = ref(false)
const deployUrl = ref('')
const deployModalOpen = ref(false)

// 当前 SSE 连接
let eventSource: EventSource | null = null

// 在新窗口打开预览
const openPreview = () => {
  if (previewUrl.value) {
    window.open(previewUrl.value, '_blank')
  }
}

// 获取应用信息
const fetchApp = async () => {
  const res = await getAppVoById({ id: toAppId(appId.value) })
  if (res.data.code === 0 && res.data.data) {
    app.value = res.data.data
  } else {
    message.error('获取应用信息失败，' + res.data.message)
  }
}

// 加载对话历史（游标分页）
const loadChatHistory = async (cursor?: string) => {
  const res = await listAppChatHistory({
    appId: toAppId(appId.value),
    pageSize: 10,
    ...(cursor ? { lastCreateTime: cursor } : {}),
  })
  if (res.data.code === 0 && res.data.data) {
    const records = res.data.data.records ?? []
    // 按创建时间升序排列（旧 → 新）
    const sorted = [...records].sort((a, b) =>
      (a.createTime || '').localeCompare(b.createTime || ''),
    )
    const historyMessages: ChatMessage[] = sorted.map((r) => ({
      role: r.messageType === 'user' ? 'user' : 'ai',
      content: r.message || '',
      createTime: r.createTime,
    }))

    if (cursor) {
      // 加载更早的消息：前置插入
      messages.value = [...historyMessages, ...messages.value]
    } else {
      // 首次加载
      messages.value = historyMessages
    }

    // 返回满一页说明可能还有更多
    hasMore.value = records.length >= 10
  } else {
    message.error('加载对话历史失败，' + res.data.message)
  }
}

// 加载更早的历史消息
const loadMore = async () => {
  if (messages.value.length === 0 || loadingMore.value) {
    return
  }
  // 以当前列表中最旧消息的 createTime 作为游标
  const cursor = messages.value[0]?.createTime
  if (!cursor) {
    hasMore.value = false
    return
  }

  loadingMore.value = true
  try {
    // 记录加载前滚动高度，便于加载后保持滚动位置
    const el = messagesRef.value
    const oldScrollHeight = el?.scrollHeight ?? 0

    await loadChatHistory(cursor)

    // 恢复滚动位置，让用户看到连续的消息
    await nextTick()
    if (el) {
      el.scrollTop = el.scrollHeight - oldScrollHeight
    }
  } finally {
    loadingMore.value = false
  }
}

// 滚动到消息底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

// 展示生成的网页
const showPreview = () => {
  if (app.value?.codeGenType) {
    previewUrl.value = getStaticPreviewUrl(app.value.codeGenType, appId.value)
  }
}

// 从响应头中解析下载文件名
const getDownloadFileName = (contentDisposition?: string) => {
  if (!contentDisposition) {
    return `app-${appId.value}`
  }

  const utf8Match = contentDisposition.match(/filename\*=UTF-8''([^;]+)/i)
  if (utf8Match?.[1]) {
    try {
      return decodeURIComponent(utf8Match[1].trim())
    } catch {
      return utf8Match[1].trim()
    }
  }

  const fileNameMatch = contentDisposition.match(/filename="?([^";]+)"?/i)
  if (fileNameMatch?.[1]) {
    return fileNameMatch[1].trim()
  }

  return `app-${appId.value}`
}

// 下载应用代码 ZIP
const doDownload = async () => {
  downloading.value = true
  try {
    const res = await downloadAppCode(
      { appId: toAppId(appId.value) },
      { responseType: 'blob' },
    )
    if (res.status !== 200) {
      message.error('下载失败')
      return
    }

    const contentDisposition = Array.isArray(res.headers['content-disposition'])
      ? res.headers['content-disposition'][0]
      : res.headers['content-disposition']
    const fileName = getDownloadFileName(contentDisposition)
    const blob = new Blob([res.data], { type: 'application/zip' })
    const objectUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = objectUrl
    link.download = fileName.endsWith('.zip') ? fileName : `${fileName}.zip`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.setTimeout(() => window.URL.revokeObjectURL(objectUrl), 0)
    message.success('下载成功')
  } catch {
    message.error('下载代码失败')
  } finally {
    downloading.value = false
  }
}

// 切换可视化编辑模式
const toggleVisualEdit = () => {
  if (visualEditEnabled.value) {
    exitEditMode()
    return
  }
  enterEditMode()
}

// 发送消息并通过 SSE 接收 AI 回复
const sendMessage = (content: string, backendContent = content) => {
  if (generating.value) {
    return false
  }
  const text = content.trim()
  if (!text) {
    message.warning('请输入内容')
    return false
  }
  // 追加用户消息
  messages.value.push({ role: 'user', content: text })
  // 追加一个空的 AI 消息用于流式填充
  const aiMessage = reactive<ChatMessage>({ role: 'ai', content: '', loading: true })
  messages.value.push(aiMessage)
  scrollToBottom()

  generating.value = true
  userInput.value = ''

  // 构造 SSE 请求地址（GET 请求，携带 cookie）
  const url = `${API_BASE_URL}/app/chat/gen/code?appId=${appId.value}&message=${encodeURIComponent(backendContent)}`
  eventSource = new EventSource(url, { withCredentials: true })
  let streamCompleted = false

  // 接收消息片段
  eventSource.onmessage = (event) => {
    if (streamCompleted) {
      return
    }
    if (!event.data) {
      return
    }
    try {
      const parsed = JSON.parse(event.data)
      if (parsed.d) {
        aiMessage.content += parsed.d
        aiMessage.loading = false
        scrollToBottom()
      }
    } catch {
      // 忽略无法解析的数据
    }
  }

  // 生成结束事件
  eventSource.addEventListener('done', () => {
    if (streamCompleted) {
      return
    }
    streamCompleted = true
    closeSSE()
    generating.value = false
    aiMessage.loading = false
    // 生成完成后展示网页
    showPreview()
    // 刷新应用信息（可能更新了 codeGenType 等）
    fetchApp()
  })

  // 处理业务错误事件（如后端限流）
  eventSource.addEventListener('business-error', (event: MessageEvent) => {
    if (streamCompleted) {
      return
    }
    try {
      const errorData = JSON.parse(event.data)
      console.error('SSE 业务错误事件:', errorData)
      const errorMessage = errorData.message || '生成过程中出现错误'
      aiMessage.content = `❌ ${errorMessage}`
      aiMessage.loading = false
      message.error(errorMessage)
      scrollToBottom()
    } catch (parseError) {
      console.error('解析错误事件失败:', parseError, '原始数据:', event.data)
      aiMessage.content = '❌ 服务器返回错误'
      aiMessage.loading = false
      message.error('服务器返回错误')
      scrollToBottom()
    } finally {
      streamCompleted = true
      generating.value = false
      closeSSE()
    }
  })

  // 出错处理
  eventSource.onerror = () => {
    if (streamCompleted) {
      return
    }
    streamCompleted = true
    closeSSE()
    generating.value = false
    aiMessage.loading = false
    if (!aiMessage.content) {
      aiMessage.content = '生成失败，请稍后重试'
    }
    message.error('生成过程中出现异常')
  }

  return true
}

// 关闭 SSE 连接
const closeSSE = () => {
  if (eventSource) {
    eventSource.close()
    eventSource = null
  }
}

// 用户点击发送
const handleSend = () => {
  const text = userInput.value.trim()
  const backendPrompt = selectedElement.value
    ? `${text}${formatElementInfoForPrompt(selectedElement.value)}`
    : text

  if (sendMessage(text, backendPrompt)) {
    exitEditMode()
  }
}

// 部署应用
const doDeploy = async () => {
  deploying.value = true
  try {
    const res = await deployApp({ appId: toAppId(appId.value) })
    if (res.data.code === 0 && res.data.data) {
      deployUrl.value = res.data.data
      deployModalOpen.value = true
      message.success('部署成功')
    } else {
      message.error('部署失败，' + res.data.message)
    }
  } finally {
    deploying.value = false
  }
}

// 打开部署后的网站
const openDeployUrl = () => {
  if (deployUrl.value) {
    window.open(deployUrl.value, '_blank')
  }
}

// 跳转到应用信息修改页
const goEdit = () => {
  detailModalOpen.value = false
  router.push(`/app/edit/${appId.value}`)
}

// 删除应用
const doDelete = () => {
  Modal.confirm({
    title: '确认删除',
    content: '删除后不可恢复，确定删除该应用吗？',
    okType: 'danger',
    onOk: async () => {
      const res = await deleteApp({ id: toAppId(appId.value) })
      if (res.data.code === 0) {
        message.success('删除成功')
        detailModalOpen.value = false
        router.push('/')
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

onMounted(async () => {
  // 并行获取应用信息与对话历史
  await Promise.all([fetchApp(), loadChatHistory()])
  historyLoaded.value = true

  if (!app.value) {
    return
  }

  // 如果是自己的应用，且没有对话历史，才自动发送初始提示词
  if (isOwner.value && messages.value.length === 0 && app.value.initPrompt) {
    sendMessage(app.value.initPrompt)
  } else if (messages.value.length >= 2) {
    // 有至少 2 条对话记录时，展示对应的网站
    showPreview()
  }
})

onUnmounted(() => {
  closeSSE()
})
</script>

<template>
  <div id="appChatPage">
    <!-- 顶部栏 -->
    <div class="header-bar">
      <div class="app-name">
        <a-avatar class="app-icon">🐱</a-avatar>
        <span class="name-text">{{ app?.appName ?? '加载中...' }}</span>
        <a-tag v-if="app?.codeGenType" color="blue">{{ codeGenTypeText }}</a-tag>
      </div>
      <div class="header-actions">
        <a-button class="header-action-button" @click="detailModalOpen = true">
          <template #icon>
            <InfoCircleOutlined />
          </template>
          应用详情
        </a-button>
        <a-button class="header-action-button" type="primary" :loading="downloading" @click="doDownload">
          <template #icon>
            <DownloadOutlined />
          </template>
          下载代码
        </a-button>
        <a-button class="header-action-button" type="primary" :loading="deploying" @click="doDeploy">
          <template #icon>
            <CloudUploadOutlined />
          </template>
          部署
        </a-button>
      </div>
    </div>

    <!-- 核心内容区域 -->
    <div class="main-content">
      <!-- 左侧对话区域 -->
      <div class="chat-panel">
        <div ref="messagesRef" class="messages">
          <!-- 加载更早消息 -->
          <div v-if="hasMore" class="load-more-area">
            <a-button :loading="loadingMore" size="small" block @click="loadMore">
              加载更早的对话
            </a-button>
          </div>
          <div
            v-for="(msg, index) in messages"
            :key="index"
            class="message-item"
            :class="msg.role === 'user' ? 'message-user' : 'message-ai'"
          >
            <a-avatar v-if="msg.role === 'ai'" class="msg-avatar">🐱</a-avatar>
            <div class="message-bubble">
              <a-spin v-if="msg.loading" size="small" />
              <!-- AI 消息按 Markdown 渲染（带代码高亮），用户消息保持纯文本 -->
              <div
                v-else-if="msg.role === 'ai'"
                class="message-content markdown-body"
                v-html="renderMarkdown(msg.content)"
              ></div>
              <div v-else class="message-content">{{ msg.content }}</div>
            </div>
            <a-avatar
              v-if="msg.role === 'user'"
              class="msg-avatar"
              :src="loginUserStore.loginUser.userAvatar"
            />
          </div>
          <a-empty
            v-if="messages.length === 0 && historyLoaded"
            description="开始和 AI 对话生成应用吧"
          />
        </div>

        <!-- 用户消息输入框 -->
        <div class="input-area">
          <a-alert
            v-if="selectedElement"
            class="selected-element-alert"
            type="info"
            show-icon
            closable
            :message="`已选中元素：${selectedElementDescription}`"
            @close="clearSelection"
          />
          <a-textarea
            v-model:value="userInput"
            placeholder="描述越详细，页面越具体，可以一步一步完善生成效果"
            :auto-size="{ minRows: 2, maxRows: 5 }"
            :disabled="generating"
            @press-enter.prevent="handleSend"
          />
          <div class="input-actions">
            <a-button
              :type="visualEditEnabled ? 'primary' : 'default'"
              :disabled="!previewUrl || generating"
              @click="toggleVisualEdit"
            >
              {{ visualEditEnabled ? '退出编辑' : '编辑模式' }}
            </a-button>
            <a-button
              type="primary"
              shape="circle"
              :loading="generating"
              :disabled="!userInput.trim()"
              @click="handleSend"
            >
              <template #icon>
                <SendOutlined />
              </template>
            </a-button>
          </div>
        </div>
      </div>

      <!-- 右侧网页展示区域 -->
      <div class="preview-panel">
        <div class="preview-header">
          <span>生成后的网页展示</span>
          <a-button v-if="previewUrl" type="link" size="small" @click="openPreview">
            新窗口打开
          </a-button>
        </div>
        <div class="preview-body">
          <iframe
            v-if="previewUrl"
            ref="previewIframeRef"
            :src="previewUrl"
            class="preview-iframe"
            @load="handlePreviewLoad"
          />
          <div v-else class="preview-placeholder">
            <a-empty description="网站文件生成完成后将在此展示" />
          </div>
        </div>
      </div>
    </div>

    <!-- 部署成功弹窗 -->
    <a-modal v-model:open="deployModalOpen" title="部署成功" :footer="null">
      <p>你的应用已成功部署，可通过以下地址访问：</p>
      <a-typography-link @click="openDeployUrl">{{ deployUrl }}</a-typography-link>
    </a-modal>

    <!-- 应用详情弹窗 -->
    <a-modal v-model:open="detailModalOpen" title="应用详情" :footer="null">
      <a-descriptions :column="1" class="detail-desc">
        <a-descriptions-item label="创建者">
          <a-space>
            <a-avatar :size="24" :src="app?.user?.userAvatar" />
            <span>{{ app?.user?.userName ?? '无名' }}</span>
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">
          {{ formatTime(app?.createTime) }}
        </a-descriptions-item>
        <a-descriptions-item label="生成类型">
          <a-tag color="blue">{{ codeGenTypeText }}</a-tag>
        </a-descriptions-item>
      </a-descriptions>
      <a-divider style="margin: 12px 0" />
      <a-space v-if="canEdit">
        <a-button type="primary" @click="goEdit">
          <template #icon><EditOutlined /></template>
          修改
        </a-button>
        <a-button danger @click="doDelete">
          <template #icon><DeleteOutlined /></template>
          删除
        </a-button>
      </a-space>
    </a-modal>
  </div>
</template>

<style scoped>
:global(.content:has(#appChatPage)) {
  background: #fff;
}

#appChatPage {
  display: flex;
  flex-direction: column;
  height: calc(100vh - 64px - 70px);
  min-height: 500px;
  background: #fff;
}

.header-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid #f0f0f0;
  background: #fff;
}

.app-name {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.header-action-button {
  width: 112px;
  justify-content: center;
}

.app-icon {
  background: #52c41a;
}

.name-text {
  font-size: 16px;
  font-weight: 500;
}

.main-content {
  display: flex;
  flex: 1;
  overflow: hidden;
  gap: 16px;
  padding: 16px 24px;
}

.chat-panel {
  display: flex;
  flex-direction: column;
  width: 40%;
  min-width: 360px;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.messages {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.load-more-area {
  text-align: center;
  margin-bottom: 12px;
}

.message-item {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.message-user {
  justify-content: flex-end;
}

.msg-avatar {
  flex-shrink: 0;
  background: #52c41a;
}

.message-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 8px;
  background: #f5f5f5;
}

/* AI 消息可能包含代码块，需要更宽的展示空间 */
.message-ai .message-bubble {
  max-width: 90%;
}

.message-user .message-bubble {
  background: #e6f4ff;
}

.message-content {
  word-break: break-word;
  line-height: 1.6;
}

/* 用户纯文本消息保留换行 */
.message-user .message-content {
  white-space: pre-wrap;
}

/* ---------- Markdown 渲染内容样式 ---------- */
.markdown-body :deep(p) {
  margin: 0 0 8px;
}

.markdown-body :deep(p:last-child) {
  margin-bottom: 0;
}

.markdown-body :deep(h1),
.markdown-body :deep(h2),
.markdown-body :deep(h3),
.markdown-body :deep(h4) {
  margin: 12px 0 8px;
  font-weight: 600;
  line-height: 1.4;
}

.markdown-body :deep(ul),
.markdown-body :deep(ol) {
  padding-left: 20px;
  margin: 8px 0;
}

.markdown-body :deep(li) {
  margin: 4px 0;
}

/* 行内代码 */
.markdown-body :deep(code:not(.hljs)) {
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(0, 0, 0, 0.06);
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

/* 代码块容器 */
.markdown-body :deep(.hljs-pre) {
  position: relative;
  margin: 10px 0;
  border-radius: 8px;
  background: #fafafa;
  border: 1px solid #eee;
  overflow: hidden;
}

/* 代码块语言标签 */
.markdown-body :deep(.hljs-lang) {
  padding: 4px 12px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
  background: #f0f0f0;
  border-bottom: 1px solid #eee;
  text-transform: uppercase;
}

/* 代码块内容 */
.markdown-body :deep(.hljs) {
  display: block;
  padding: 12px;
  overflow-x: auto;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
  line-height: 1.6;
  background: transparent;
}

.input-area {
  border-top: 1px solid #f0f0f0;
  padding: 12px;
}

.selected-element-alert {
  margin-bottom: 8px;
}

.input-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-top: 8px;
}

.preview-panel {
  display: flex;
  flex-direction: column;
  flex: 1;
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}

.preview-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid #f0f0f0;
  color: rgba(0, 0, 0, 0.65);
}

.preview-body {
  flex: 1;
  overflow: hidden;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

.preview-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}
</style>
