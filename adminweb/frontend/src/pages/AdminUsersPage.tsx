import { useEffect, useState } from 'react'
import {
  Table, Button, Tag, Modal, Form, Input, Select, Space,
  Popconfirm, message, Typography,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, KeyOutlined } from '@ant-design/icons'
import { getAdminUsers, createAdminUser, updateAdminUser, deleteAdminUser } from '../api/api'
import { useLang, pick } from '../i18n'

const { Title } = Typography

interface AdminUser {
  id: number
  username: string
  role: 'SUPER_ADMIN' | 'ZAV_KAFEDRA' | 'PROFESSOR'
  fullName: string
  createdAt: string
}

const roleMeta = {
  SUPER_ADMIN: { label: 'Super Admin', color: 'red' },
  ZAV_KAFEDRA: { label: 'Zav. Kafedra', color: 'blue' },
  PROFESSOR:   { label: 'Professor',   color: 'green' },
}

export default function AdminUsersPage() {
  const lang = useLang()
  const [users, setUsers] = useState<AdminUser[]>([])
  const [loading, setLoading] = useState(false)
  const [createModal, setCreateModal] = useState(false)
  const [editUser, setEditUser] = useState<AdminUser | null>(null)
  const [pwdUser, setPwdUser] = useState<AdminUser | null>(null)
  const [createForm] = Form.useForm()
  const [editForm] = Form.useForm()
  const [pwdForm] = Form.useForm()

  const load = async () => {
    setLoading(true)
    try {
      const res = await getAdminUsers()
      setUsers(res.data)
    } catch {
      message.error(pick(lang, 'Xatolik', 'Ошибка'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async () => {
    const vals = await createForm.validateFields()
    try {
      await createAdminUser(vals)
      message.success(pick(lang, 'Foydalanuvchi yaratildi', 'Пользователь создан'))
      setCreateModal(false)
      createForm.resetFields()
      load()
    } catch (e: any) {
      message.error(e?.response?.data?.error || pick(lang, 'Xatolik', 'Ошибка'))
    }
  }

  const handleEdit = async () => {
    if (!editUser) return
    const vals = await editForm.validateFields()
    try {
      await updateAdminUser(editUser.id, { role: vals.role, fullName: vals.fullName })
      message.success(pick(lang, 'Yangilandi', 'Обновлено'))
      setEditUser(null)
      load()
    } catch {
      message.error(pick(lang, 'Xatolik', 'Ошибка'))
    }
  }

  const handlePassword = async () => {
    if (!pwdUser) return
    const vals = await pwdForm.validateFields()
    try {
      await updateAdminUser(pwdUser.id, { password: vals.password })
      message.success(pick(lang, 'Parol yangilandi', 'Пароль обновлён'))
      setPwdUser(null)
      pwdForm.resetFields()
    } catch {
      message.error(pick(lang, 'Xatolik', 'Ошибка'))
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteAdminUser(id)
      message.success(pick(lang, "O'chirildi", 'Удалено'))
      load()
    } catch (e: any) {
      message.error(e?.response?.data?.error || pick(lang, 'Xatolik', 'Ошибка'))
    }
  }

  const columns = [
    { title: 'Username', dataIndex: 'username', key: 'username', width: 160 },
    { title: pick(lang, "To'liq ism", 'Полное имя'), dataIndex: 'fullName', key: 'fullName' },
    {
      title: pick(lang, 'Rol', 'Роль'), dataIndex: 'role', key: 'role', width: 150,
      render: (r: string) => {
        const m = roleMeta[r as keyof typeof roleMeta] ?? { label: r, color: 'default' }
        return <Tag color={m.color}>{m.label}</Tag>
      },
    },
    {
      title: pick(lang, "Ro'yxatga olingan", 'Дата регистрации'), dataIndex: 'createdAt', key: 'createdAt', width: 180,
      render: (v: string) => v ? new Date(v).toLocaleDateString(lang === 'ru' ? 'ru-RU' : 'uz-UZ') : '—',
    },
    {
      title: pick(lang, 'Amallar', 'Действия'), key: 'actions', width: 180,
      render: (_: unknown, u: AdminUser) => (
        <Space>
          <Button size="small" icon={<EditOutlined />}
            onClick={() => { setEditUser(u); editForm.setFieldsValue({ role: u.role, fullName: u.fullName }) }}
          />
          <Button size="small" icon={<KeyOutlined />}
            onClick={() => { setPwdUser(u); pwdForm.resetFields() }}
          />
          <Popconfirm title={pick(lang, "O'chirishni tasdiqlaysizmi?", 'Подтвердите удаление?')} onConfirm={() => handleDelete(u.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>{pick(lang, 'Admin foydalanuvchilari', 'Пользователи-администраторы')}</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setCreateModal(true); createForm.resetFields() }}>
          {pick(lang, 'Yangi foydalanuvchi', 'Новый пользователь')}
        </Button>
      </div>

      <Table
        dataSource={users}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="small"
      />

      {/* Create modal */}
      <Modal
        title={pick(lang, 'Yangi foydalanuvchi', 'Новый пользователь')}
        open={createModal}
        onOk={handleCreate}
        onCancel={() => setCreateModal(false)}
        okText={pick(lang, 'Yaratish', 'Создать')}
        cancelText={pick(lang, 'Bekor qilish', 'Отмена')}
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="username" label="Username" rules={[{ required: true, message: pick(lang, 'Kiritish shart', 'Обязательное поле') }]}>
            <Input />
          </Form.Item>
          <Form.Item name="fullName" label={pick(lang, "To'liq ism", 'Полное имя')}>
            <Input />
          </Form.Item>
          <Form.Item name="password" label={pick(lang, 'Parol', 'Пароль')} rules={[{ required: true, min: 6, message: pick(lang, 'Kamida 6 belgi', 'Минимум 6 символов') }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="role" label={pick(lang, 'Rol', 'Роль')} initialValue="PROFESSOR" rules={[{ required: true }]}>
            <Select options={[
              { value: 'SUPER_ADMIN', label: 'Super Admin' },
              { value: 'ZAV_KAFEDRA', label: 'Zav. Kafedra' },
              { value: 'PROFESSOR',   label: 'Professor' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit modal */}
      <Modal
        title={`${pick(lang, 'Tahrirlash', 'Редактирование')}: ${editUser?.username}`}
        open={!!editUser}
        onOk={handleEdit}
        onCancel={() => setEditUser(null)}
        okText={pick(lang, 'Saqlash', 'Сохранить')}
        cancelText={pick(lang, 'Bekor qilish', 'Отмена')}
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="fullName" label={pick(lang, "To'liq ism", 'Полное имя')}>
            <Input />
          </Form.Item>
          <Form.Item name="role" label={pick(lang, 'Rol', 'Роль')} rules={[{ required: true }]}>
            <Select options={[
              { value: 'SUPER_ADMIN', label: 'Super Admin' },
              { value: 'ZAV_KAFEDRA', label: 'Zav. Kafedra' },
              { value: 'PROFESSOR',   label: 'Professor' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Password modal */}
      <Modal
        title={`${pick(lang, "Parolni o'zgartirish", 'Смена пароля')}: ${pwdUser?.username}`}
        open={!!pwdUser}
        onOk={handlePassword}
        onCancel={() => setPwdUser(null)}
        okText={pick(lang, 'Saqlash', 'Сохранить')}
        cancelText={pick(lang, 'Bekor qilish', 'Отмена')}
      >
        <Form form={pwdForm} layout="vertical">
          <Form.Item name="password" label={pick(lang, 'Yangi parol', 'Новый пароль')} rules={[{ required: true, min: 6, message: pick(lang, 'Kamida 6 belgi', 'Минимум 6 символов') }]}>
            <Input.Password />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
