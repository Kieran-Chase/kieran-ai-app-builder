import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getLoginUser } from '@/api/userController.ts'
import ACCESS_ENUM from '@/access/accessEnum'

/**
 * 登录用户信息管理
 */
export const useLoginUserStore = defineStore('loginUser', () => {
  //默认值
  const loginUser = ref<API.LoginUserVO>({
    userName: '未登录',
  })

  //获取登录用户信息
  async function fetchLoginUser() {
    const res = await getLoginUser()
    if (res.data.code === 0 && res.data.data) {
      loginUser.value = res.data.data
    } else {
      // 未登录也标记，避免重复请求
      loginUser.value = { userName: '未登录', userRole: ACCESS_ENUM.NOT_LOGIN }
    }
  }

  //更新登录用户信息
  function setLoginUser(newLoginUser: API.LoginUserVO) {
    loginUser.value = newLoginUser
  }

  return { loginUser, fetchLoginUser, setLoginUser }
})
