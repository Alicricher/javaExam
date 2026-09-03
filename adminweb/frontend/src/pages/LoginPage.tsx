import { useState } from 'react'
import { Form, Input, Button, Card, message } from 'antd'
import { useNavigate } from 'react-router-dom'
import { login } from '../api/api'
import { useLang, pick } from '../i18n'

export default function LoginPage() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const lang = useLang()

  const onFinish = async (values: { username: string; password: string }) => {
    setLoading(true)
    try {
      await login(values.username, values.password)
      navigate('/')
    } catch (err) {
      const status = (err as { response?: { status?: number; data?: { retryAfterSeconds?: number } } })?.response?.status
      if (status === 429) {
        const retrySec = (err as { response?: { data?: { retryAfterSeconds?: number } } })?.response?.data?.retryAfterSeconds
        const minutes = Math.max(1, Math.ceil((retrySec ?? 900) / 60))
        message.error(pick(lang,
          `Juda ko'p urinish. ${minutes} daqiqadan keyin qayta urinib ko'ring`,
          `Слишком много попыток. Повторите через ${minutes} мин.`))
      } else {
        message.error(pick(lang, "Login yoki parol noto'g'ri", 'Неверный логин или пароль'))
      }
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '100vh', background: '#f0f2f5' }}>
      <Card title="Admin panel" style={{ width: 360 }}>
        <Form layout="vertical" onFinish={onFinish} initialValues={{ username: 'admin' }}>
          <Form.Item name="username" label={pick(lang, 'Login', 'Логин')} rules={[{ required: true, message: pick(lang, 'Login kiriting', 'Введите логин') }]}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label={pick(lang, 'Parol', 'Пароль')} rules={[{ required: true, message: pick(lang, 'Parol kiriting', 'Введите пароль') }]}>
            <Input.Password />
          </Form.Item>
          <Button type="primary" htmlType="submit" block loading={loading}>
            {pick(lang, 'Kirish', 'Войти')}
          </Button>
        </Form>
      </Card>
    </div>
  )
}
