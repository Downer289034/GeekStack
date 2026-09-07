import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      component: MainLayout,
      children: [
        {
          path: '',
          name: 'home',
          component: () => import('@/views/home/HomeView.vue'),
          meta: { title: '首页' }
        },
        {
          path: 'shares',
          name: 'share-plaza',
          component: () => import('@/views/share/SharePlazaView.vue'),
          meta: { title: '项目广场' }
        },
        {
          path: 'shares/new',
          name: 'share-publish',
          component: () => import('@/views/share/SharePublishView.vue'),
          meta: { title: '发布项目', requiresAuth: true }
        },
        {
          path: 'shares/edit/:id',
          name: 'share-edit',
          component: () => import('@/views/share/SharePublishView.vue'),
          meta: { title: '编辑项目', requiresAuth: true }
        },
        {
          path: 'shares/:id',
          name: 'share-detail',
          component: () => import('@/views/share/ShareDetailView.vue'),
          meta: { title: '项目详情' }
        },
        {
          path: 'forum',
          name: 'forum',
          component: () => import('@/views/forum/ForumListView.vue'),
          meta: { title: '技术论坛' }
        },
        {
          path: 'forum/posts/:id',
          name: 'post-detail',
          component: () => import('@/views/forum/PostDetailView.vue'),
          meta: { title: '帖子详情' }
        },
        {
          path: 'users/:username',
          name: 'profile',
          component: () => import('@/views/profile/ProfileView.vue'),
          meta: { title: '个人主页' }
        }
      ]
    },
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/auth/LoginView.vue'),
      meta: { title: '登录 / 注册' }
    }
  ]
})

router.beforeEach((to) => {
  if (to.meta.requiresAuth) {
    const auth = useAuthStore()
    if (!auth.isLoggedIn) {
      return { name: 'login', query: { redirect: to.fullPath } }
    }
  }
  return true
})

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · 极客栈 GeekStack` : '极客栈 GeekStack'
})

export default router
