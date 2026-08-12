export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'focusflow-theme'

export function getStoredTheme(): Theme | null {
  if (typeof window === 'undefined') {
    return null
  }
  const value = localStorage.getItem(STORAGE_KEY)
  if (value === 'light' || value === 'dark') {
    return value
  }
  return null
}

export function setStoredTheme(theme: Theme): void {
  localStorage.setItem(STORAGE_KEY, theme)
}

export function getSystemTheme(): Theme {
  if (typeof window === 'undefined') {
    return 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches
    ? 'dark'
    : 'light'
}

export function resolveInitialTheme(): Theme {
  return getStoredTheme() ?? getSystemTheme()
}

export function applyThemeToDocument(theme: Theme): void {
  document.documentElement.classList.toggle('dark', theme === 'dark')
}
