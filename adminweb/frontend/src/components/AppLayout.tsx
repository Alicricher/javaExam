import { useEffect, useState } from 'react'
import { Layout, Menu, Button } from 'antd'
import {
  TeamOutlined, FileTextOutlined, SolutionOutlined,
  BookOutlined, LogoutOutlined,
} from '@ant-design/icons'
import { Outlet, useNavigate, useLocation, Navigate } from 'react-router-dom'
import { getMe, logout } from '../api/api'

const { Sider, Content } = Layout

const menuItems = [
  { key: '/students', icon: <TeamOutlined />, label: 'Talabalar' },
  { key: '/results/tests', icon: <FileTextOutlined />, label: 'Test natijalari' },
  { key: '/results/situational', icon: <SolutionOutlined />, label: 'Vaziyatli topshiriqlar' },
  { key: '/content', icon: <BookOutlined />, label: 'Kontent' },
]

export default function AppLayout() {
  const [authed, setAuthed] = useState<boolean | null>(null)
  const navigate = useNavigate()
  const location = useLocation()

  useEffect(() => {
    getMe().then(() => setAuthed(true)).catch(() => setAuthed(false))
  }, [])

  if (authed === null) return null
  if (authed === false) return <Navigate to="/login" replace />

  const handleLogout = async () => {
    await logout()
    navigate('/login')
  }

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider width={240} theme="dark" breakpoint="lg" collapsedWidth={0}>
        <div style={{ color: '#fff', padding: 16, fontWeight: 700, fontSize: 16 }}>
          Admin panel
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
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
