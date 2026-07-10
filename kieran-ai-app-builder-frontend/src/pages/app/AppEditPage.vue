<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import {
  getAppVoById,
  getAppVoByIdByAdmin,
  updateApp,
  updateAppByAdmin,
} from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import ACCESS_ENUM from '@/access/accessEnum'
import { CODE_GEN_TYPE_MAP } from '@/constant/app'
import { getDeployUrl } from '@/config/env'
import { formatTime } from '@/utils/time'
import { toAppId } from '@/utils/appId'

const route = useRoute()
const router = useRouter()
const loginUserStore = useLoginUserStore()

// 应用 id
const appId = computed(() => route.params.id as string)
// 当前用户是否为管理员
const isAdmin = computed(() => loginUserStore.loginUser.userRole === ACCESS_ENUM.ADMIN)

// 完整应用信息（用于展示只读字段和应用信息区块）
const app = ref<API.AppVO>()

// 表单数据（管理员可编辑更多字段）
const formState = reactive<API.AppAdminUpdateRequest>({
  appName: '',
  cover: '',
  priority: undefined,
})

// 生成类型的中文文案
const codeGenTypeText = computed(
  () => CODE_GEN_TYPE_MAP[app.value?.codeGenType ?? ''] ?? app.value?.codeGenType ?? '-',
)

// 加载态
const submitting = ref(false)

// 用表单初始值回填（重置用）
const resetForm = () => {
  formState.appName = app.value?.appName
  formState.cover = app.value?.cover
  formState.priority = app.value?.priority
}

// 获取应用信息
const fetchApp = async () => {
  const id = toAppId(appId.value)
  // 管理员使用管理员接口，可查看任意应用
  const res = isAdmin.value
    ? await getAppVoByIdByAdmin({ id })
    : await getAppVoById({ id })
  if (res.data.code === 0 && res.data.data) {
    app.value = res.data.data
    resetForm()
  } else {
    message.error('获取应用信息失败，' + res.data.message)
  }
}

// 提交表单
const handleSubmit = async () => {
  submitting.value = true
  try {
    const id = toAppId(appId.value)
    // 管理员可更新名称、封面、优先级；普通用户仅更新名称
    const res = isAdmin.value
      ? await updateAppByAdmin({
          id,
          appName: formState.appName,
          cover: formState.cover,
          priority: formState.priority,
        })
      : await updateApp({ id, appName: formState.appName })
    if (res.data.code === 0) {
      message.success('保存成功')
      router.back()
    } else {
      message.error('保存失败，' + res.data.message)
    }
  } finally {
    submitting.value = false
  }
}

// 进入对话页
const goChat = () => {
  router.push(`/app/chat/${appId.value}`)
}

// 打开部署后的网站预览
const openDeployUrl = () => {
  if (app.value?.deployKey) {
    window.open(getDeployUrl(app.value.deployKey), '_blank')
  }
}

onMounted(() => {
  fetchApp()
})
</script>

<template>
  <div id="appEditPage">
    <h2 class="title">编辑应用信息</h2>

    <!-- 基本信息 -->
    <a-card title="基本信息" class="section-card">
      <a-form :model="formState" layout="vertical" @finish="handleSubmit">
        <a-form-item
          label="应用名称"
          name="appName"
          :rules="[{ required: true, message: '请输入应用名称' }]"
        >
          <a-input
            v-model:value="formState.appName"
            placeholder="请输入应用名称"
            :maxlength="50"
            show-count
            allow-clear
          />
        </a-form-item>

        <!-- 初始提示词（不可修改） -->
        <a-form-item label="初始提示词">
          <a-textarea
            :value="app?.initPrompt"
            :rows="4"
            :maxlength="1000"
            show-count
            disabled
          />
          <div class="form-tip">初始提示词不可修改</div>
        </a-form-item>

        <!-- 生成类型（不可修改） -->
        <a-form-item label="生成类型">
          <a-input :value="codeGenTypeText" disabled />
          <div class="form-tip">生成类型不可修改</div>
        </a-form-item>

        <!-- 部署密钥（不可修改） -->
        <a-form-item v-if="app?.deployKey" label="部署密钥">
          <a-input :value="app.deployKey" disabled />
          <div class="form-tip">部署密钥不可修改</div>
        </a-form-item>

        <!-- 仅管理员可编辑封面和优先级 -->
        <template v-if="isAdmin">
          <a-form-item label="应用封面" name="cover">
            <a-input v-model:value="formState.cover" placeholder="请输入封面图片地址" allow-clear />
          </a-form-item>
          <a-form-item label="优先级" name="priority">
            <a-input-number
              v-model:value="formState.priority"
              :min="0"
              placeholder="设置为 99 即为精选应用"
              style="width: 100%"
            />
          </a-form-item>
        </template>

        <a-form-item>
          <a-space>
            <a-button type="primary" html-type="submit" :loading="submitting">保存修改</a-button>
            <a-button @click="resetForm">重置</a-button>
            <a-button type="link" @click="goChat">进入对话</a-button>
          </a-space>
        </a-form-item>
      </a-form>
    </a-card>

    <!-- 应用信息 -->
    <a-card title="应用信息" class="section-card">
      <a-descriptions bordered :column="2" size="middle">
        <a-descriptions-item label="应用 ID">{{ app?.id ?? '-' }}</a-descriptions-item>
        <a-descriptions-item label="创建者">
          <a-space>
            <a-avatar :size="24" :src="app?.user?.userAvatar" />
            <span>{{ app?.user?.userName ?? '无名' }}</span>
          </a-space>
        </a-descriptions-item>
        <a-descriptions-item label="创建时间">{{ formatTime(app?.createTime) }}</a-descriptions-item>
        <a-descriptions-item label="更新时间">{{ formatTime(app?.updateTime) }}</a-descriptions-item>
        <a-descriptions-item label="部署时间">
          {{ app?.deployedTime ? formatTime(app.deployedTime) : '未部署' }}
        </a-descriptions-item>
        <a-descriptions-item label="访问链接">
          <a-typography-link v-if="app?.deployKey" @click="openDeployUrl">查看预览</a-typography-link>
          <span v-else>未部署</span>
        </a-descriptions-item>
      </a-descriptions>
    </a-card>
  </div>
</template>

<style scoped>
#appEditPage {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 16px;
}

.title {
  margin-bottom: 24px;
}

.section-card {
  margin-bottom: 24px;
}

.form-tip {
  margin-top: 4px;
  font-size: 12px;
  color: rgba(0, 0, 0, 0.45);
}
</style>
