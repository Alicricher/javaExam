import { useEffect, useState } from 'react'
import { Layout, Menu, Button, Segmented, Tag } from 'antd'
import {
  TeamOutlined, FileTextOutlined, SolutionOutlined,
  BookOutlined, LogoutOutlined, UserOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation, Navigate } from 'react-router-dom'
import { getMe, logout } from '../api/api'

const { Sider, Content } = Layout

export type AdminRole = 'SUPER_ADMIN' | 'ZAV_KAFEDRA' | 'PROFESSOR'
export type AdminLang = 'uz' | 'ru'

const LANG_KEY = 'admin_lang'

export function getAdminLang(): AdminLang {
  return (localStorage.getItem(LANG_KEY) as AdminLang) || 'uz'
}

const roleMeta: Record<AdminRole, { label: string; color: string }> = {
  SUPER_ADMIN:  { label: 'Super Admin', color: 'red' },
  ZAV_KAFEDRA:  { label: 'Zav. Kafedra', color: 'blue' },
  PROFESSOR:    { label: 'Professor', color: 'green' },
}

const allMenuItems = [
  { key: '/students',           icon: <TeamOutlined />,        label: 'Talabalar',              minRole: 'PROFESSOR' },
  { key: '/results/tests',      icon: <FileTextOutlined />,    label: 'Test natijalari',        minRole: 'PROFESSOR' },
  { key: '/results/situational',icon: <SolutionOutlined />,    label: 'Vaziyatli topshiriqlar', minRole: 'PROFESSOR' },
  { key: '/content',            icon: <BookOutlined />,        label: 'Kontent',                minRole: 'ZAV_KAFEDRA' },
  { key: '/admin-users',        icon: <UserOutlined />,        label: 'Foydalanuvchilar',       minRole: 'SUPER_ADMIN' },
]

const roleOrder: AdminRole[] = ['PROFESSOR', 'ZAV_KAFEDRA', 'SUPER_ADMIN']

function hasAccess(userRole: AdminRole, minRole: string): boolean {
  return roleOrder.indexOf(userRole) >= roleOrder.indexOf(minRole as AdminRole)
}

export default function AppLayout() {
  const [authed, setAuthed] = useState<boolean | null>(null)
  const [role, setRole] = useState<AdminRole>('PROFESSOR')
  const [username, setUsername] = useState('')
  const [lang, setLang] = useState<AdminLang>(getAdminLang)
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    getMe()
      .then(res => {
        setAuthed(true)
        setRole(res.data.role as AdminRole)
        setUsername(res.data.username)
      })
      .catch(() => setAuthed(false))
  }, [])

  if (authed === null) return null
  if (authed === false) return <Navigate to="/login" replace />

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  const handleLangChange = (val: string | number) => {
    const newLang = val as AdminLang
    setLang(newLang)
    localStorage.setItem(LANG_KEY, newLang)
    window.dispatchEvent(new Event('admin-lang-change'))
  }

  const menuItems = allMenuItems
    .filter(item => hasAccess(role, item.minRole))
    .map(({ key, icon, label }) => ({ key, icon, label }))

  const meta = roleMeta[role] ?? roleMeta.PROFESSOR

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} theme="dark" breakpoint="lg" collapsedWidth={0}>
        <div style={{ padding: '16px 16px 4px', color: '#fff', fontWeight: 700, fontSize: 15 }}>
          Admin panel
        </div>
        <div style={{ padding: '0 16px 12px' }}>
          <Tag color={meta.color} style={{ fontSize: 11 }}>{meta.label}</Tag>
          <span style={{ color: 'rgba(255,255,255,0.6)', fontSize: 12 }}>{username}</span>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
        <div style={{ position: 'absolute', bottom: 56, left: 16, right: 16 }}>
          <Segmented
            value={lang}
            options={[{ label: 'UZ', value: 'uz' }, { label: 'RU', value: 'ru' }]}
            onChange={handleLangChange}
            style={{ width: '100%', background: 'rgba(255,255,255,0.1)' }}
          />
        </div>
        <div style={{ position: 'absolute', bottom: 16, left: 16, right: 16 }}>
          <Button
            danger
            icon={<LogoutOutlined />}
            onClick={handleLogout}
            style={{ width: '100%', color: '#ff4d4f', borderColor: '#ff4d4f', background: 'transparent' }}
          >
            Chiqish
          </Button>
        </div>
      </Sider>
      <Layout>
        <Content style={{ margin: 16, padding: 16, background: '#fff', borderRadius: 8, minHeight: 400 }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}
