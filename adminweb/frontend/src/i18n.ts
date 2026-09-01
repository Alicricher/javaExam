import { useEffect, useState } from 'react'

export type AdminLang = 'uz' | 'ru'

const LANG_KEY = 'admin_lang'
const LANG_EVENT = 'admin-lang-change'

export function getAdminLang(): AdminLang {
  return (localStorage.getItem(LANG_KEY) as AdminLang) || 'uz'
}

export function setAdminLang(lang: AdminLang) {
  localStorage.setItem(LANG_KEY, lang)
  window.dispatchEvent(new Event(LANG_EVENT))
}

/** Re-renders the calling component whenever the admin language changes. */
export function useLang(): AdminLang {
  const [lang, setLang] = useState<AdminLang>(getAdminLang)
  useEffect(() => {
    const onChange = () => setLang(getAdminLang())
    window.addEventListener(LANG_EVENT, onChange)
    window.addEventListener('storage', onChange)
    return () => {
      window.removeEventListener(LANG_EVENT, onChange)
      window.removeEventListener('storage', onChange)
    }
  }, [])
  return lang
}

/** pick(lang, "Uzbek text", "Russian text") */
export function pick(lang: AdminLang, uz: string, ru: string): string {
  return lang === 'ru' ? ru : uz
}
