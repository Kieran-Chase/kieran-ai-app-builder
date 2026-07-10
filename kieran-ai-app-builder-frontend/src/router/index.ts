import { createRouter, createWebHistory } from 'vue-router'
import HomePage from '../pages/HomePage.vue'
import UserLoginPage from '../pages/user/UserLoginPage.vue'
import UserRegisterPage from '../pages/user/UserRegisterPage.vue'
import UserManagePage from '../pages/admin/UserManagePage.vue'
import AppManagePage from '../pages/admin/AppManagePage.vue'
import AppChatPage from '../pages/app/AppChatPage.vue'
import AppEditPage from '../pages/app/AppEditPage.vue'
import NoAuth from '../pages/NoAuth.vue'
import ACCESS_ENUM from '@/access/accessEnum'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '主页',
      component: HomePage,
    },
    {
      path: '/user/login',
      name: '用户登录',
      component: UserLoginPage,
    },
    {
      path: '/user/register',
      name: '用户注册',
      component: UserRegisterPage,
    },
    {
      path: '/admin/userManage',
      name: '用户管理',
      component: UserManagePage,
      meta: {
        access: ACCESS_ENUM.ADMIN,
      },
    },
    {
      path: '/admin/appManage',
      name: '应用管理',
      component: AppManagePage,
      meta: {
        access: ACCESS_ENUM.ADMIN,
      },
    },
    {
      path: '/app/chat/:id',
      name: '应用对话',
      component: AppChatPage,
      meta: {
        access: ACCESS_ENUM.USER,
        hideInMenu: true,
      },
    },
    {
      path: '/app/edit/:id',
      name: '应用信息修改',
      component: AppEditPage,
      meta: {
        access: ACCESS_ENUM.USER,
        hideInMenu: true,
      },
    },
    {
      path: '/noAuth',
      name: '无权限',
      component: NoAuth,
    },
  ],
})

export default router
