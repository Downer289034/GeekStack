import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { initThemeFromStorage } from '@/stores/theme'
import './theme/tokens.css'

initThemeFromStorage()

createApp(App).use(createPinia()).use(router).mount('#app')
