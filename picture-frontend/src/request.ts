import axios from 'axios'
import { message } from 'ant-design-vue'

// 服务器配置
const DEV_BASE_URL = ""; // 使用相对路径，通过 Vite 代理转发
const PROD_BASE_URL = ""; // 生产环境使用相对路径，通过 nginx 代理转发

// 导出服务器URL，用于生成分享链接
export const SERVER_URL = import.meta.env.MODE === 'development'
  ? `http://localhost:5173`
  : window.location.origin;

const myAxios = axios.create({
  baseURL: import.meta.env.MODE === 'development' ? DEV_BASE_URL : PROD_BASE_URL,
  timeout: 60000,
  withCredentials: true,
})

//全局请求拦截器
myAxios.interceptors.request.use(
  function (config) {
    return config
  },
  function (error) {
    return Promise.reject(error)
  },
)
//全局响应拦截器
myAxios.interceptors.response.use(
  function (response) {
    const { data } = response
    //未登录
    if (data.code === 40100) {
      //不是获取用户信息的请求，并且用户目前不是已经在用户登录页面，则跳转到登录页面
      if (
        !response.request.responseURL.includes('user/get/login') &&
        !window.location.pathname.includes('/user/login')
      ) {
        message.warning('请先登录')
        window.location.href = `/user/login?redirect=${window.location.href}`
      }
    }
    return response
  },
  function (error) {
    return Promise.reject(error)
  },
)

export default myAxios
