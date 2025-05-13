import Vue from 'vue'
import VueRouter from 'vue-router'
import Chat from '../views/Chat.vue'
import ModelAccess from '../views/ModelAccess.vue'
import AdminPanel from '../views/AdminPanel.vue'
import UserLogin from '../views/Login.vue'
import UserProfile from '@/views/UserProfile.vue'
import store from '../store'

Vue.use(VueRouter)

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: UserLogin,
    meta: { isPublic: true }
  },
  {
    path: '/',
    name: 'Chat',
    component: Chat,
    meta: { requiresAuth: true }
  },
  {
    path: '/share/:shareId',
    name: 'SharedChat',
    component: () => import('../views/SharedChat.vue'),
    meta: { isPublic: true, noRedirect: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: UserProfile,
    meta: { requiresAuth: true }
  },
  {
    path: '/model-access',
    name: 'ModelAccess',
    component: ModelAccess,
    meta: { requiresAuth: true }
  },
  {
    path: '/admin',
    name: 'AdminPanel',
    component: AdminPanel,
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/user-management',
    name: 'UserManagement',
    component: () => import('../views/admin/UserManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/logs',
    name: 'LogViewer',
    component: () => import('../views/admin/LogViewer.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/prompt-config',
    name: 'PromptConfig',
    component: () => import('../views/admin/PromptConfig.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/content-records',
    name: 'ContentRecords',
    component: () => import('../views/admin/ContentRecords.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/image-records',
    name: 'ImageRecords',
    component: () => import('../views/admin/ImageRecords.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/model-config',
    name: 'ModelConfig',
    component: () => import('@/views/admin/ModelConfig.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  },
  {
    path: '/admin/department-management',
    name: 'DepartmentManagement',
    component: () => import('@/views/admin/DepartmentManagement.vue'),
    meta: { requiresAuth: true, requiresAdmin: true }
  }
]

const router = new VueRouter({
  mode: 'hash',
  base: process.env.BASE_URL,
  routes
})

// 全局变量，跟踪应用是否是通过共享链接访问的
window.isShareRoute = false;

// 路由守卫
router.beforeEach((to, from, next) => {
  // 从localStorage检查token
  const token = localStorage.getItem('token');
  const isAuthenticated = store.getters.isLoggedIn && token;
  const isAdmin = store.getters.isAdmin;

  // 检查是否是共享路由
  if (to.name === 'SharedChat') {
    window.isShareRoute = true;
    next();
    return;
  }

  // 如果是共享模式访问其他页面(比如资源文件)，也放行
  if (window.isShareRoute && !to.matched.some(record => record.meta.requiresAuth)) {
    next();
    return;
  }

  // 如果是公开页面，直接放行
  if (to.matched.some(record => record.meta.isPublic)) {
    // 如果已登录且尝试访问登录页，重定向到首页
    if (isAuthenticated && to.path === '/login') {
      next({ path: '/' });
      return;
    }
    next();
    return;
  }

  // 需要认证的页面
  if (to.matched.some(record => record.meta.requiresAuth)) {
    if (!isAuthenticated) {
      // 未登录，重定向到登录页并保存目标路径
      next({
        path: '/login',
        query: { redirect: to.fullPath }
      });
    } else if (to.matched.some(record => record.meta.requiresAdmin) && !isAdmin) {
      // 需要管理员权限但用户不是管理员
      next({ path: '/' });
    } else {
      // 认证通过
      next();
    }
  } else {
    // 不需要认证的页面
    next();
  }
});

export default router
