<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { ArrowUpOutlined } from '@ant-design/icons-vue'
import { addApp, listMyAppVoByPage, listGoodAppVoByPage } from '@/api/appController.ts'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import ACCESS_ENUM from '@/access/accessEnum'
import AppCard from '@/components/AppCard.vue'
import { CODE_GEN_TYPE_CONFIG } from '@/constant/app'

const router = useRouter()
const loginUserStore = useLoginUserStore()

// 用户输入的提示词
const initPrompt = ref('')
// 选择的代码生成类型，空字符串表示默认模式（AI 自动路由）
const selectedCodeGenType = ref('')
// 创建应用加载态
const creating = ref(false)
// 代码生成类型选项
const codeGenTypeOptions = [
  { label: '默认模式', value: '' },
  ...Object.values(CODE_GEN_TYPE_CONFIG),
]

// 提示词示例（点击快速填充）
const promptExamples = [
  {
    label: '波普风电商页面',
    prompt:
      '生成一个潮流零售电商首页，整体采用复古波普视觉语言，使用高饱和玫红、橙黄和蓝色形成强烈对比。页面加入漫画爆炸贴纸、半调网点纹理、粗黑描边和趣味插画元素，商品卡片做成海报橱窗效果，主按钮要醒目、有活力，适合潮牌服饰、设计玩具或艺术周边售卖。',
  },
  {
    label: '企业网站',
    prompt:
      '制作一个专业企业官网首页，视觉风格稳重、清晰、有商务质感。页面需要包含顶部导航、品牌主视觉、核心业务介绍、解决方案模块、企业优势、合作客户展示、用户评价和底部联系方式，整体布局要适合科技公司、咨询服务或企业级软件品牌。',
  },
  {
    label: '电商运营后台',
    prompt:
      '设计一个现代电商管理控制台，界面以白色为主，搭配蓝紫渐变作为导航和关键操作色。首页展示今日销售额、订单量、转化率、库存预警等核心指标，订单模块使用紧凑卡片列表，商品区域体现分类管理和排序能力，数据分析区域包含图表、筛选条件和趋势概览，整体强调高效、清爽、适合运营人员日常使用。',
  },
  {
    label: '暗黑话题社区',
    prompt:
      '生成一个暗色主题的话题社区首页，借鉴主流社交平台的信息流、热榜和话题广场能力，但布局和视觉不能照搬。页面包含左侧导航、中央信息流、热门话题榜、内容发布入口、用户资料摘要、点赞评论转发数据和实时趋势模块，整体氛围偏科技感和夜间阅读体验。',
  },
]

// 是否已登录
const isLogin = () => {
  const role = loginUserStore.loginUser.userRole
  return role && role !== ACCESS_ENUM.NOT_LOGIN
}

// 填充示例提示词
const fillExample = (prompt: string) => {
  initPrompt.value = prompt
}

// 创建应用
const doCreateApp = async () => {
  const prompt = initPrompt.value.trim()
  if (!prompt) {
    message.warning('请先输入你的创意提示词')
    return
  }
  // 未登录跳转登录
  if (!isLogin()) {
    message.warning('请先登录')
    router.push(`/user/login?redirect=${window.location.href}`)
    return
  }
  creating.value = true
  try {
    const res = await addApp({
      initPrompt: prompt,
      codeGenType: selectedCodeGenType.value || undefined,
    })
    if (res.data.code === 0 && res.data.data) {
      message.success('创建成功，开始生成应用')
      // 跳转到对话页，并携带 isNew 标记以自动发送初始提示词
      router.push(`/app/chat/${res.data.data}?isNew=1`)
    } else {
      message.error('创建失败，' + res.data.message)
    }
  } finally {
    creating.value = false
  }
}

// ------- 我的应用 -------
const myApps = ref<API.AppVO[]>([])
const myAppTotal = ref(0)
const myAppSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const fetchMyApps = async () => {
  // 未登录则不请求
  if (!isLogin()) {
    return
  }
  const res = await listMyAppVoByPage({ ...myAppSearchParams })
  if (res.data.code === 0 && res.data.data) {
    myApps.value = res.data.data.records ?? []
    myAppTotal.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取我的应用失败，' + res.data.message)
  }
}

const onMyAppPageChange = (page: number, pageSize: number) => {
  myAppSearchParams.pageNum = page
  myAppSearchParams.pageSize = pageSize
  fetchMyApps()
}

// ------- 精选应用 -------
const goodApps = ref<API.AppVO[]>([])
const goodAppTotal = ref(0)
const goodAppSearchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 20,
})

const fetchGoodApps = async () => {
  const res = await listGoodAppVoByPage({ ...goodAppSearchParams })
  if (res.data.code === 0 && res.data.data) {
    goodApps.value = res.data.data.records ?? []
    goodAppTotal.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取精选应用失败，' + res.data.message)
  }
}

const onGoodAppPageChange = (page: number, pageSize: number) => {
  goodAppSearchParams.pageNum = page
  goodAppSearchParams.pageSize = pageSize
  fetchGoodApps()
}

onMounted(() => {
  fetchMyApps()
  fetchGoodApps()
})
</script>

<template>
  <div id="homePage">
    <!-- 网站标题 -->
    <div class="hero">
      <h1 class="hero-title">一句话 🐱 呈所想</h1>
      <p class="hero-desc">与 AI 对话轻松创建应用和网站</p>
    </div>

    <!-- 提示词输入框 -->
    <div class="prompt-box">
      <a-textarea
        v-model:value="initPrompt"
        :auto-size="{ minRows: 4, maxRows: 8 }"
        :bordered="false"
        class="prompt-input"
      />
      <div class="prompt-footer">
        <div class="code-gen-type">
          <a-radio-group v-model:value="selectedCodeGenType" button-style="solid">
            <a-radio-button
              v-for="option in codeGenTypeOptions"
              :key="option.value"
              :value="option.value"
            >
              {{ option.label }}
            </a-radio-button>
          </a-radio-group>
        </div>
        <div class="prompt-actions">
          <a-button type="primary" shape="circle" :loading="creating" @click="doCreateApp">
            <template #icon>
              <ArrowUpOutlined />
            </template>
          </a-button>
        </div>
      </div>
    </div>

    <!-- 提示词示例 -->
    <div class="prompt-examples">
      <a-button
        v-for="example in promptExamples"
        :key="example.label"
        shape="round"
        @click="fillExample(example.prompt)"
      >
        {{ example.label }}
      </a-button>
    </div>

    <!-- 我的应用 -->
    <div v-if="myApps.length > 0" class="app-section">
      <h2 class="section-title">我的作品</h2>
      <a-row :gutter="[16, 16]">
        <a-col v-for="app in myApps" :key="app.id" :xs="24" :sm="12" :md="8" :xl="6">
          <AppCard :app="app" />
        </a-col>
      </a-row>
      <div class="pagination-wrapper">
        <a-pagination
          :current="myAppSearchParams.pageNum"
          :page-size="myAppSearchParams.pageSize"
          :total="myAppTotal"
          :show-size-changer="false"
          hide-on-single-page
          @change="onMyAppPageChange"
        />
      </div>
    </div>

    <!-- 精选应用 -->
    <div class="app-section">
      <h2 class="section-title">精选案例</h2>
      <a-row :gutter="[16, 16]">
        <a-col v-for="app in goodApps" :key="app.id" :xs="24" :sm="12" :md="8" :xl="6">
          <AppCard :app="app" />
        </a-col>
      </a-row>
      <a-empty v-if="goodApps.length === 0" description="暂无精选应用" />
      <div class="pagination-wrapper">
        <a-pagination
          :current="goodAppSearchParams.pageNum"
          :page-size="goodAppSearchParams.pageSize"
          :total="goodAppTotal"
          :show-size-changer="false"
          hide-on-single-page
          @change="onGoodAppPageChange"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
#homePage {
  max-width: 1200px;
  margin: 0 auto;
}

.hero {
  text-align: center;
  padding: 48px 0 32px;
}

.hero-title {
  font-size: 42px;
  font-weight: 700;
  margin-bottom: 12px;
  color: rgba(0, 0, 0, 0.88);
}

.hero-desc {
  font-size: 16px;
  color: rgba(0, 0, 0, 0.45);
}

.prompt-box {
  position: relative;
  max-width: 720px;
  margin: 0 auto;
  padding: 16px;
  /* panel: 半透明白用于磨砂雾面承托，不改变页面渐变底色 */
  background: rgba(255, 255, 255, 0.68);
  backdrop-filter: blur(18px);
  border-radius: 16px;
  box-shadow: none;
  border: 1px solid rgba(255, 255, 255, 0.72);
}

.prompt-input {
  font-size: 16px;
  padding: 0;
  resize: none;
}

.prompt-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 12px;
}

.prompt-actions {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

.code-gen-type {
  min-width: 0;
}

.code-gen-type :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)) {
  color: #fff;
  background: #37cbe8;
  border-color: #37cbe8;
}

.code-gen-type :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled):hover) {
  color: #fff;
  background: #37cbe8;
  border-color: #37cbe8;
}

.code-gen-type :deep(.ant-radio-button-wrapper-checked:not(.ant-radio-button-wrapper-disabled)::before) {
  background-color: #37cbe8;
}

.prompt-actions :deep(.ant-btn-primary) {
  background: #37cbe8;
  border-color: #37cbe8;
}

.prompt-actions :deep(.ant-btn-primary:not(:disabled):hover) {
  background: #37cbe8;
  border-color: #37cbe8;
}

.prompt-examples {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: 12px;
  margin: 24px auto 0;
  max-width: 720px;
}

.app-section {
  margin-top: 56px;
}

.section-title {
  font-size: 24px;
  font-weight: 600;
  margin-bottom: 24px;
  color: rgba(0, 0, 0, 0.88);
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .hero-title {
    font-size: 32px;
  }

  .code-gen-type :deep(.ant-radio-group) {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .code-gen-type :deep(.ant-radio-button-wrapper) {
    border-inline-start-width: 1px;
    border-radius: 6px;
  }

  .code-gen-type :deep(.ant-radio-button-wrapper::before) {
    display: none;
  }
}
</style>
