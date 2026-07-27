<template>
  <a-layout class="layout">
    <GlobalHeader />
    <a-layout-content class="content">
      <RouterView />
    </a-layout-content>
    <GlobalFooter />
  </a-layout>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/GlobalHeader.vue'
import GlobalFooter from '@/components/GlobalFooter.vue'
</script>

<style scoped>
.layout {
  /* gradient-angle: 135deg 对角线渐变，覆盖整个浏览器视口 */
  /* gradient-colors: 浅雾粉 + 浅薰衣草蓝，低饱和柔和过渡 */
  --app-soft-gradient: linear-gradient(135deg, #f9e8ef 0%, #e9edff 100%);
  min-height: 100vh;
  background: var(--app-soft-gradient);
  background-attachment: fixed;
}

.content {
  padding: 24px 50px;
  background: transparent;
}

@media (max-width: 768px) {
  .content {
    padding: 16px 20px;
  }
}
</style>
