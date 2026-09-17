<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  NAvatar,
  NForm,
  NFormItem,
  NInput,
  NUpload,
  type FormInst,
  type FormRules,
  type UploadCustomRequestOptions,
  useMessage
} from 'naive-ui'
import { updateMe, updatePassword, type UserProfile } from '@/api/auth'
import { uploadMedia } from '@/api/media'
import { oauthLoginUrl, unbindOAuth, type OAuthProvider } from '@/api/oauth'
import GsButton from '@/components/gs/GsButton.vue'
import GsSkeleton from '@/components/gs/GsSkeleton.vue'
import { useAuthStore } from '@/stores/auth'

const AVATAR_MAX_BYTES = 2 * 1024 * 1024

const router = useRouter()
const auth = useAuthStore()
const message = useMessage()

const loading = ref(true)
const savingProfile = ref(false)
const savingPassword = ref(false)
const uploadingAvatar = ref(false)

const profileFormRef = ref<FormInst | null>(null)
const passwordFormRef = ref<FormInst | null>(null)

const profileModel = reactive({
  nickname: '',
  bio: '',
  avatarUrl: '' as string | null
})

const passwordModel = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileRules: FormRules = {
  nickname: [
    { required: true, message: '请输入昵称' },
    { max: 64, message: '昵称最长 64 字' }
  ],
  bio: [{ max: 500, message: '简介最长 500 字' }]
}

const passwordRules: FormRules = {
  newPassword: [
    { required: true, message: '请输入新密码' },
    { min: 6, message: '密码至少 6 位' }
  ],
  confirmPassword: [
    {
      required: true,
      validator: (_rule, value: string) => {
        if (!value) return new Error('请再次输入新密码')
        if (value !== passwordModel.newPassword) return new Error('两次密码不一致')
        return true
      },
      trigger: ['input', 'blur']
    }
  ]
}

const me = computed(() => auth.user as UserProfile | null)
const hasPassword = computed(() => me.value?.hasPassword !== false)
const bindings = computed(() => ({
  github: me.value?.oauthBindings?.github === true,
  gitee: me.value?.oauthBindings?.gitee === true
}))

function syncFromUser(user: UserProfile) {
  profileModel.nickname = user.nickname ?? ''
  profileModel.bio = user.bio ?? ''
  profileModel.avatarUrl = user.avatarUrl
}

async function init() {
  loading.value = true
  try {
    await auth.fetchCurrentUser()
    if (!auth.user) {
      void router.push('/login')
      return
    }
    syncFromUser(auth.user)
  } finally {
    loading.value = false
  }
}

async function onSaveProfile() {
  try {
    await profileFormRef.value?.validate()
  } catch {
    return
  }
  savingProfile.value = true
  try {
    const updated = await updateMe({
      nickname: profileModel.nickname.trim(),
      bio: profileModel.bio.trim() || null,
      avatarUrl: profileModel.avatarUrl || null
    })
    auth.user = updated
    syncFromUser(updated)
    message.success('资料已保存')
  } catch {
    // interceptor toast
  } finally {
    savingProfile.value = false
  }
}

async function onSavePassword() {
  try {
    await passwordFormRef.value?.validate()
  } catch {
    return
  }
  if (hasPassword.value && !passwordModel.oldPassword) {
    message.error('请输入当前密码')
    return
  }
  savingPassword.value = true
  try {
    await updatePassword({
      oldPassword: hasPassword.value ? passwordModel.oldPassword : null,
      newPassword: passwordModel.newPassword
    })
    message.success(hasPassword.value ? '密码已修改' : '密码已设置')
    passwordModel.oldPassword = ''
    passwordModel.newPassword = ''
    passwordModel.confirmPassword = ''
    await auth.fetchCurrentUser()
  } catch {
    // interceptor toast
  } finally {
    savingPassword.value = false
  }
}

async function customAvatarUpload(options: UploadCustomRequestOptions) {
  const file = options.file.file
  if (!file) {
    options.onError()
    return
  }
  if (file.size > AVATAR_MAX_BYTES) {
    message.error('头像不能超过 2MB')
    options.onError()
    return
  }
  uploadingAvatar.value = true
  try {
    const result = await uploadMedia(file, 'avatar')
    profileModel.avatarUrl = result.url
    options.onFinish()
    message.success('头像已上传，请点击保存资料')
  } catch {
    options.onError()
  } finally {
    uploadingAvatar.value = false
  }
}

async function onUnbind(provider: OAuthProvider) {
  try {
    await unbindOAuth(provider)
    message.success(`已解绑 ${provider === 'GITHUB' ? 'GitHub' : 'Gitee'}`)
    await auth.fetchCurrentUser()
  } catch {
    // interceptor toast（含最后绑定保护）
  }
}

onMounted(() => {
  void init()
})
</script>

<template>
  <div class="settings">
    <h2 class="settings-title font-display">个人设置</h2>

    <GsSkeleton v-if="loading" variant="detail" />

    <template v-else>
      <section class="settings-section" aria-labelledby="profile-heading">
        <h3 id="profile-heading" class="section-title font-display">资料</h3>
        <div class="avatar-row">
          <n-avatar round :size="72" :src="profileModel.avatarUrl || undefined" class="avatar">
            {{ profileModel.nickname?.charAt(0) || '?' }}
          </n-avatar>
          <n-upload
            :custom-request="customAvatarUpload"
            :show-file-list="false"
            accept="image/jpeg,image/png,image/webp,image/gif"
          >
            <GsButton variant="ghost" :disabled="uploadingAvatar">
              {{ uploadingAvatar ? '上传中…' : '上传头像' }}
            </GsButton>
          </n-upload>
          <p class="hint font-mono">jpg/png/webp/gif，≤2MB</p>
        </div>

        <n-form
          ref="profileFormRef"
          :model="profileModel"
          :rules="profileRules"
          label-placement="top"
        >
          <n-form-item label="昵称" path="nickname">
            <n-input v-model:value="profileModel.nickname" placeholder="显示名称" />
          </n-form-item>
          <n-form-item label="简介" path="bio">
            <n-input
              v-model:value="profileModel.bio"
              type="textarea"
              :rows="3"
              placeholder="介绍一下自己"
            />
          </n-form-item>
          <GsButton variant="primary" :disabled="savingProfile" @click="onSaveProfile">
            {{ savingProfile ? '保存中…' : '保存资料' }}
          </GsButton>
        </n-form>
      </section>

      <section class="settings-section" aria-labelledby="password-heading">
        <h3 id="password-heading" class="section-title font-display">
          {{ hasPassword ? '修改密码' : '设置密码' }}
        </h3>
        <p v-if="!hasPassword" class="hint">
          你当前通过第三方登录注册，尚未设置本地密码。设置后可用用户名密码登录。
        </p>
        <n-form
          ref="passwordFormRef"
          :model="passwordModel"
          :rules="passwordRules"
          label-placement="top"
        >
          <n-form-item v-if="hasPassword" label="当前密码" path="oldPassword">
            <n-input
              v-model:value="passwordModel.oldPassword"
              type="password"
              show-password-on="click"
              autocomplete="current-password"
            />
          </n-form-item>
          <n-form-item label="新密码" path="newPassword">
            <n-input
              v-model:value="passwordModel.newPassword"
              type="password"
              show-password-on="click"
              autocomplete="new-password"
            />
          </n-form-item>
          <n-form-item label="确认新密码" path="confirmPassword">
            <n-input
              v-model:value="passwordModel.confirmPassword"
              type="password"
              show-password-on="click"
              autocomplete="new-password"
            />
          </n-form-item>
          <GsButton variant="primary" :disabled="savingPassword" @click="onSavePassword">
            {{ savingPassword ? '提交中…' : hasPassword ? '修改密码' : '设置密码' }}
          </GsButton>
        </n-form>
      </section>

      <section class="settings-section" aria-labelledby="oauth-heading">
        <h3 id="oauth-heading" class="section-title font-display">账号绑定</h3>
        <div class="bind-row">
          <div>
            <strong>GitHub</strong>
            <span class="bind-status font-mono">{{ bindings.github ? '已绑定' : '未绑定' }}</span>
          </div>
          <a v-if="!bindings.github" class="bind-link" :href="oauthLoginUrl('GITHUB')">去绑定</a>
          <GsButton v-else variant="text" @click="onUnbind('GITHUB')">解绑</GsButton>
        </div>
        <div class="bind-row">
          <div>
            <strong>Gitee</strong>
            <span class="bind-status font-mono">{{ bindings.gitee ? '已绑定' : '未绑定' }}</span>
          </div>
          <a v-if="!bindings.gitee" class="bind-link" :href="oauthLoginUrl('GITEE')">去绑定</a>
          <GsButton v-else variant="text" @click="onUnbind('GITEE')">解绑</GsButton>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.settings-title {
  margin: 0 0 20px;
  font-size: 1.25rem;
  font-weight: 700;
  color: var(--text-1);
}

.settings-section {
  margin-bottom: 32px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border);
}

.section-title {
  margin: 0 0 14px;
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text-1);
}

.avatar-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.avatar {
  background: var(--accent);
  color: var(--accent-text);
  flex-shrink: 0;
}

.hint {
  margin: 0 0 12px;
  font-size: 0.75rem;
  color: var(--text-3);
}

.bind-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--border);
}

.bind-row:last-child {
  border-bottom: 1px solid var(--border);
}

.bind-status {
  margin-left: 10px;
  font-size: 0.75rem;
  color: var(--text-3);
}

.bind-link {
  color: var(--accent);
  text-decoration: none;
  font-size: 0.875rem;
}

.bind-link:hover {
  text-decoration: underline;
}
</style>
