<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { message, Modal } from 'ant-design-vue'
import { deleteAppByAdmin, listAppVoByPageByAdmin, updateAppByAdmin } from '@/api/appController.ts'
import { GOOD_APP_PRIORITY, CODE_GEN_TYPE_MAP } from '@/constant/app'
import { formatTime } from '@/utils/time'

const router = useRouter()

const columns = [
  {
    title: 'id',
    dataIndex: 'id',
  },
  {
    title: '应用名称',
    dataIndex: 'appName',
  },
  {
    title: '封面',
    dataIndex: 'cover',
  },
  {
    title: '初始提示词',
    dataIndex: 'initPrompt',
    ellipsis: true,
  },
  {
    title: '生成类型',
    dataIndex: 'codeGenType',
  },
  {
    title: '优先级',
    dataIndex: 'priority',
  },
  {
    title: '创建者',
    dataIndex: 'userId',
  },
  {
    title: '创建时间',
    dataIndex: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
  },
]

// 数据
const data = ref<API.AppVO[]>([])
const total = ref(0)

// 搜索条件（管理员不限每页数量，默认 10）
const searchParams = reactive<API.AppQueryRequest>({
  pageNum: 1,
  pageSize: 10,
})

// 获取数据
const fetchData = async () => {
  const res = await listAppVoByPageByAdmin({ ...searchParams })
  if (res.data.code === 0 && res.data.data) {
    data.value = res.data.data.records ?? []
    total.value = res.data.data.totalRow ?? 0
  } else {
    message.error('获取数据失败，' + res.data.message)
  }
}

// 分页参数
const pagination = computed(() => {
  return {
    current: searchParams.pageNum ?? 1,
    pageSize: searchParams.pageSize ?? 10,
    total: total.value,
    showSizeChanger: true,
    showTotal: (total: number) => `共 ${total} 条`,
  }
})

// 搜索
const doSearch = () => {
  searchParams.pageNum = 1
  fetchData()
}

// 表格变化处理
const doTableChange = (page: any) => {
  searchParams.pageNum = page.current
  searchParams.pageSize = page.pageSize
  fetchData()
}

// 删除应用
const doDelete = (id: number) => {
  if (!id) {
    return
  }
  Modal.confirm({
    title: '确认删除',
    content: '删除后不可恢复，确定删除该应用吗？',
    okType: 'danger',
    onOk: async () => {
      const res = await deleteAppByAdmin({ id })
      if (res.data.code === 0) {
        message.success('删除成功')
        fetchData()
      } else {
        message.error('删除失败，' + res.data.message)
      }
    },
  })
}

// 编辑应用（跳转到应用信息修改页）
const doEdit = (id: number) => {
  router.push(`/app/edit/${id}`)
}

// 设为精选（优先级设为 99）
const doFeature = async (record: API.AppVO) => {
  const res = await updateAppByAdmin({
    id: record.id,
    priority: GOOD_APP_PRIORITY,
  })
  if (res.data.code === 0) {
    message.success('已设为精选')
    fetchData()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

// 取消精选（优先级恢复为 0）
const doUnfeature = async (record: API.AppVO) => {
  const res = await updateAppByAdmin({
    id: record.id,
    priority: 0,
  })
  if (res.data.code === 0) {
    message.success('已取消精选')
    fetchData()
  } else {
    message.error('操作失败，' + res.data.message)
  }
}

// 页面加载时请求一次
onMounted(() => {
  fetchData()
})
</script>

<template>
  <div id="appManagePage">
    <!-- 搜索表单 -->
    <a-form layout="inline" :model="searchParams" @finish="doSearch">
      <a-form-item label="应用名称">
        <a-input v-model:value="searchParams.appName" placeholder="输入应用名称" allow-clear />
      </a-form-item>
      <a-form-item label="创建者 id">
        <a-input v-model:value="searchParams.userId" placeholder="输入创建者 id" allow-clear />
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">搜索</a-button>
      </a-form-item>
    </a-form>
    <a-divider />
    <!-- 表格 -->
    <a-table
      :columns="columns"
      :data-source="data"
      :pagination="pagination"
      :scroll="{ x: 1200 }"
      @change="doTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.dataIndex === 'cover'">
          <a-image v-if="record.cover" :src="record.cover" :width="80" />
          <span v-else>-</span>
        </template>
        <template v-else-if="column.dataIndex === 'codeGenType'">
          <a-tag color="blue">{{ CODE_GEN_TYPE_MAP[record.codeGenType] ?? record.codeGenType }}</a-tag>
        </template>
        <template v-else-if="column.dataIndex === 'priority'">
          <a-tag v-if="record.priority === GOOD_APP_PRIORITY" color="gold">精选</a-tag>
          <span v-else>{{ record.priority ?? 0 }}</span>
        </template>
        <template v-else-if="column.dataIndex === 'createTime'">
          {{ formatTime(record.createTime) }}
        </template>
        <template v-else-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="doEdit(record.id)">编辑</a-button>
            <a-button
              v-if="record.priority !== GOOD_APP_PRIORITY"
              type="link"
              size="small"
              @click="doFeature(record)"
            >
              精选
            </a-button>
            <a-button v-else type="link" size="small" @click="doUnfeature(record)"> 取消精选 </a-button>
            <a-button type="link" size="small" danger @click="doDelete(record.id)">删除</a-button>
          </a-space>
        </template>
      </template>
    </a-table>
  </div>
</template>

<style scoped>
#appManagePage {
  padding: 24px;
}
</style>
