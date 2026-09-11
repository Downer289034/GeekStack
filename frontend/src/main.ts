import { createApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'
import router from './router'
import { vReveal } from '@/directives/reveal'
import { initThemeFromStorage } from '@/stores/theme'
import './theme/tokens.css'

initThemeFromStorage()

const app = createApp(App)
app.directive('reveal', vReveal)
app.use(createPinia()).use(router).mount('#app')
