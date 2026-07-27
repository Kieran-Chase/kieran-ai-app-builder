<template>
  <a-layout-header class="header" :class="{ 'chat-header': isChatPage }">
    <div class="header-content">
      <div class="logo-section">
        <span class="title">Kieran代码生成器</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="menu"
        @click="handleMenuClick"
      />
      <div class="user-section">
        <div v-if="loginUserStore.loginUser.id">
          <a-dropdown>
            <a-space style="cursor: pointer">
              <a-avatar :src="loginUserStore.loginUser.userAvatar" />
              {{ loginUserStore.loginUser.userName ?? '无名' }}
            </a-space>
            <template #overlay>
              <a-menu>
                <a-menu-item @click="doLogout">
                  <LogoutOutlined />
                  退出登录
                </a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
        <div v-else>
          <a-button type="primary" @click="router.push('/user/login')">登录</a-button>
        </div>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref, h, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import type { MenuProps } from 'ant-design-vue'
import { HomeOutlined, LogoutOutlined } from '@ant-design/icons-vue'
import { useLoginUserStore } from '@/stores/loginUser.ts'
import { userLogout } from '@/api/userController.ts'
import checkAccess from '@/access/checkAccess'

//获取登录用户状态
const loginUserStore = useLoginUserStore()

const router = useRouter()
const route = useRoute()
const selectedKeys = ref<string[]>(['home'])
const isChatPage = computed(() => route.path.startsWith('/app/chat/'))

// 用户退出登录
const doLogout = async () => {
  const res = await userLogout()
  if (res.data.code === 0) {
    loginUserStore.setLoginUser({
      userName: '未登录',
    })
    message.success('退出登录成功')
    await router.push('/user/login')
  } else {
    message.error('退出登录失败，' + res.data.message)
  }
}

// 菜单配置项
const originItems = [
  {
    key: '/',
    icon: () => h(HomeOutlined),
    label: '主页',
    title: '主页',
  },
  {
    key: '/admin/userManage',
    label: '用户管理',
    title: '用户管理',
  },
  {
    key: '/admin/appManage',
    label: '应用管理',
    title: '应用管理',
  },
  {
    key: '/admin/chatHistoryManage',
    label: '对话管理',
    title: '对话管理',
  },
]

// 根据菜单 key 找到对应的路由项
const menuToRouteItem = (menuKey: string) => {
  return router
    .getRoutes()
    .find((route) => route.path === menuKey)
}

// 过滤菜单项
const filterMenus = (menus = [] as MenuProps['items']) => {
  return menus?.filter((menu) => {
    const menuKey = menu?.key as string
    const routeItem = menuToRouteItem(menuKey)
    // 路由配置中标记了 hideInMenu 则不展示
    if (routeItem?.meta?.hideInMenu) {
      return false
    }
    // 根据权限过滤，有权限则保留该菜单
    if (routeItem?.meta?.access) {
      return checkAccess(
        loginUserStore.loginUser,
        routeItem.meta.access as string,
      )
    }
    // 没有配置 access 的菜单默认展示
    return true
  })
}

// 展示在菜单的路由数组
const menuItems = computed<MenuProps['items']>(() => filterMenus(originItems))

// 菜单点击
const handleMenuClick = ({ key }: { key: string }) => {
  if (key.startsWith('/')) {
    router.push(key)
  }
}
</script>

<style scoped>
.header {
  /* background: 复用主体页面同一 135deg 柔雾浅渐变，保持上下无缝衔接 */
  background: var(--app-soft-gradient);
  background-attachment: fixed;
  padding: 0 50px;
  box-shadow: none;
  position: sticky;
  top: 0;
  z-index: 100;
}

.chat-header {
  background: #fff;
  background-attachment: initial;
}

.header-content {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.logo-section {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}

.title {
  font-size: 18px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}

.menu {
  flex: 1;
  background: transparent;
  border: none;
  margin: 0 40px;
}

.user-section {
  display: flex;
  align-items: center;
  flex-shrink: 0;
}

@media (max-width: 768px) {
  .header {
    padding: 0 20px;
  }

  .menu {
    margin: 0 20px;
  }

  .title {
    font-size: 14px;
  }

}
</style>
