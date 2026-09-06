import { createRouter, createWebHistory } from 'vue-router'
import MainLayout from '@/layouts/MainLayout.vue'

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

router.afterEach((to) => {
  const title = to.meta.title as string | undefined
  document.title = title ? `${title} · Java & AI 交流社区` : 'Java & AI 交流社区'
})

export default router
