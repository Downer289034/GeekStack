import type { Directive, DirectiveBinding } from 'vue'

const REDUCED = '(prefers-reduced-motion: reduce)'

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && window.matchMedia(REDUCED).matches
}

/**
 * v-reveal：IntersectionObserver 入场（threshold 0.12，一次性）
 * 用法：v-reveal 或 v-reveal="true"（false 时立即可见，跳过观察）
 */
export const vReveal: Directive<HTMLElement, boolean | undefined> = {
  mounted(el, binding: DirectiveBinding<boolean | undefined>) {
    el.classList.add('gs-reveal')

    if (binding.value === false || prefersReducedMotion()) {
      el.classList.add('is-visible')
      return
    }

    const io = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (!entry.isIntersecting) continue
          entry.target.classList.add('is-visible')
          io.unobserve(entry.target)
        }
      },
      { threshold: 0.12 }
    )

    io.observe(el)
    ;(el as HTMLElement & { __gsRevealIo?: IntersectionObserver }).__gsRevealIo = io
  },
  unmounted(el) {
    const io = (el as HTMLElement & { __gsRevealIo?: IntersectionObserver }).__gsRevealIo
    io?.disconnect()
  }
}
