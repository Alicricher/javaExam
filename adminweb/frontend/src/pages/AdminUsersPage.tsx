import { useEffect, useState } from 'react'
import {
  Table, Button, Tag, Modal, Form, Input, Select, Space,
  Popconfirm, message, Typography,
} from 'antd'
import { PlusOutlined, EditOutlined, DeleteOutlined, KeyOutlined } from '@ant-design/icons'
import { getAdminUsers, createAdminUser, updateAdminUser, deleteAdminUser } from '../api/api'

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
      message.error('Xatolik')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleCreate = async () => {
    const vals = await createForm.validateFields()
    try {
      await createAdminUser(vals)
      message.success('Foydalanuvchi yaratildi')
      setCreateModal(false)
      createForm.resetFields()
      load()
    } catch (e: any) {
      message.error(e?.response?.data?.error || 'Xatolik')
    }
  }

  const handleEdit = async () => {
    if (!editUser) return
    const vals = await editForm.validateFields()
    try {
      await updateAdminUser(editUser.id, { role: vals.role, fullName: vals.fullName })
      message.success('Yangilandi')
      setEditUser(null)
      load()
    } catch {
      message.error('Xatolik')
    }
  }

  const handlePassword = async () => {
    if (!pwdUser) return
    const vals = await pwdForm.validateFields()
    try {
      await updateAdminUser(pwdUser.id, { password: vals.password })
      message.success('Parol yangilandi')
      setPwdUser(null)
      pwdForm.resetFields()
    } catch {
      message.error('Xatolik')
    }
  }

  const handleDelete = async (id: number) => {
    try {
      await deleteAdminUser(id)
      message.success("O'chirildi")
      load()
    } catch (e: any) {
      message.error(e?.response?.data?.error || 'Xatolik')
    }
  }

  const columns = [
    { title: 'Username', dataIndex: 'username', key: 'username', width: 160 },
    { title: "To'liq ism", dataIndex: 'fullName', key: 'fullName' },
    {
      title: 'Rol', dataIndex: 'role', key: 'role', width: 150,
      render: (r: string) => {
        const m = roleMeta[r as keyof typeof roleMeta] ?? { label: r, color: 'default' }
        return <Tag color={m.color}>{m.label}</Tag>
      },
    },
    {
      title: "Ro'yxatga olingan", dataIndex: 'createdAt', key: 'createdAt', width: 180,
      render: (v: string) => v ? new Date(v).toLocaleDateString('uz-UZ') : '—',
    },
    {
      title: 'Amallar', key: 'actions', width: 180,
      render: (_: unknown, u: AdminUser) => (
        <Space>
          <Button size="small" icon={<EditOutlined />}
            onClick={() => { setEditUser(u); editForm.setFieldsValue({ role: u.role, fullName: u.fullName }) }}
          />
          <Button size="small" icon={<KeyOutlined />}
            onClick={() => { setPwdUser(u); pwdForm.resetFields() }}
          />
          <Popconfirm title="O'chirishni tasdiqlaysizmi?" onConfirm={() => handleDelete(u.id)}>
            <Button size="small" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 16 }}>
        <Title level={4} style={{ margin: 0 }}>Admin foydalanuvchilari</Title>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => { setCreateModal(true); createForm.resetFields() }}>
          Yangi foydalanuvchi
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
        title="Yangi foydalanuvchi"
        open={createModal}
        onOk={handleCreate}
        onCancel={() => setCreateModal(false)}
        okText="Yaratish"
        cancelText="Bekor qilish"
      >
        <Form form={createForm} layout="vertical">
          <Form.Item name="username" label="Username" rules={[{ required: true, message: 'Kiritish shart' }]}>
            <Input />
          </Form.Item>
          <Form.Item name="fullName" label="To'liq ism">
            <Input />
          </Form.Item>
          <Form.Item name="password" label="Parol" rules={[{ required: true, min: 6, message: 'Kamida 6 belgi' }]}>
            <Input.Password />
          </Form.Item>
          <Form.Item name="role" label="Rol" initialValue="PROFESSOR" rules={[{ required: true }]}>
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
        title={`Tahrirlash: ${editUser?.username}`}
        open={!!editUser}
        onOk={handleEdit}
        onCancel={() => setEditUser(null)}
        okText="Saqlash"
        cancelText="Bekor qilish"
      >
        <Form form={editForm} layout="vertical">
          <Form.Item name="fullName" label="To'liq ism">
            <Input />
          </Form.Item>
          <Form.Item name="role" label="Rol" rules={[{ required: true }]}>
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
        title={`Parolni o'zgartirish: ${pwdUser?.username}`}
        open={!!pwdUser}
        onOk={handlePassword}
        onCancel={() => setPwdUser(null)}
        okText="Saqlash"
        cancelText="Bekor qilish"
      >
        <Form form={pwdForm} layout="vertical">
          <Form.Item name="password" label="Yangi parol" rules={[{ required: true, min: 6, message: 'Kamida 6 belgi' }]}>
            <Input.Password />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
