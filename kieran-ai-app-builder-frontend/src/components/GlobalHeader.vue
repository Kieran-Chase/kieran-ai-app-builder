<template>
  <a-layout-header class="header">
    <div class="header-content">
      <div class="logo-section">
        <span class="logo">🤖</span>
        <span class="title">Kieran代码生成器</span>
      </div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        mode="horizontal"
        :items="menuItems"
        class="menu"
      />
      <div class="user-section">
        <a-button type="primary">登录</a-button>
      </div>
    </div>
  </a-layout-header>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()
const selectedKeys = ref<string[]>(['home'])

const menuItems = [
  {
    key: 'home',
    label: 'Home',
    onClick: () => router.push('/')
  },
  {
    key: 'about',
    label: 'About',
    onClick: () => router.push('/about')
  }
]
</script>

<style scoped>
.header {
  background: #fff;
  padding: 0 50px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  position: sticky;
  top: 0;
  z-index: 100;
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

.logo {
  font-size: 32px;
  line-height: 1;
}

.title {
  font-size: 18px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}

.menu {
  flex: 1;
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

  .logo {
    font-size: 28px;
  }
}
</style>
