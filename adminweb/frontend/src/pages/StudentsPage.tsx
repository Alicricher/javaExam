import { useEffect, useState } from 'react'
import {
  Table, Input, Select, Button, Space, Modal, Form, message, Typography, Tag, Tooltip, AutoComplete,
} from 'antd'
import { SearchOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons'
import {
  getStudents, getStudentFilterOptions, updateStudentName, getStudentResults,
  grantTestRetake, grantSituationalRetake,
} from '../api/api'

const { Title, Text } = Typography

interface Student {
  id: number
  telegramId: number
  fullName: string
  course: number
  groupName: string
  subgroup: string
  faculty: string
}

interface StudentFilterOptions {
  courses: number[]
  groups: string[]
  subgroups: string[]
  faculties: string[]
}

const toSelectOptions = (values: string[]) => values.map(value => ({ value }))

const getErrorMessage = (err: unknown, fallback: string) => {
  const e = err as { response?: { data?: { error?: string } }; message?: string }
  return e?.response?.data?.error || e?.message || fallback
}

export default function StudentsPage() {
  const [students, setStudents] = useState<Student[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [pageSize, setPageSize] = useState(20)
  const [filters, setFilters] = useState({ name: '', course: 0, group: '', subgroup: '', faculty: '' })
  const [filterOptions, setFilterOptions] = useState<StudentFilterOptions>({
    courses: [1, 2, 3, 4, 5, 6],
    groups: [],
    subgroups: [],
    faculties: [],
  })
  const [saving, setSaving] = useState<'edit' | 'retake' | null>(null)

  const [editModal, setEditModal] = useState<{ open: boolean; student: Student | null }>({ open: false, student: null })
  const [resultsModal, setResultsModal] = useState<{
    open: boolean
    student: Student | null
    data: Record<string, unknown>[]
    total: number
    page: number
    pageSize: number
    loading: boolean
  }>({ open: false, student: null, data: [], total: 0, page: 1, pageSize: 10, loading: false })
  const [retakeModal, setRetakeModal] = useState<{ open: boolean; student: Student | null }>({ open: false, student: null })

  const [editForm] = Form.useForm()
  const [retakeForm] = Form.useForm()

  const load = async (p = page, f = filters, size = pageSize) => {
    setLoading(true)
    try {
      const res = await getStudents({
        page: p,
        size,
        name: f.name,
        course: f.course || undefined,
        group: f.group,
        subgroup: f.subgroup,
        faculty: f.faculty,
      })
      setStudents(res.data.students)
      setTotal(res.data.total)
    } catch (err) {
      message.error(getErrorMessage(err, 'Talabalarni yuklashda xatolik'))
    } finally {
      setLoading(false)
    }
  }

  const loadFilterOptions = async () => {
    try {
      const res = await getStudentFilterOptions()
      setFilterOptions({
        courses: res.data.courses?.length ? res.data.courses : [1, 2, 3, 4, 5, 6],
        groups: res.data.groups || [],
        subgroups: res.data.subgroups || [],
        faculties: res.data.faculties || [],
      })
    } catch {
      setFilterOptions(options => options)
    }
  }

  useEffect(() => {
    load()
    loadFilterOptions()
  }, [])

  const openResults = async (student: Student, p = 1, size = resultsModal.pageSize) => {
    setResultsModal(m => ({ ...m, open: true, student, loading: true, page: p, pageSize: size }))
    try {
      const res = await getStudentResults(student.id, p, size)
      setResultsModal({
        open: true,
        student,
        data: res.data.results || [],
        total: res.data.total || 0,
        page: p,
        pageSize: size,
        loading: false,
      })
    } catch (err) {
      message.error(getErrorMessage(err, 'Natijalarni yuklashda xatolik'))
      setResultsModal(m => ({ ...m, loading: false }))
    }
  }

  const openEdit = (student: Student) => {
    editForm.setFieldsValue({ name: student.fullName })
    setEditModal({ open: true, student })
  }

  const submitEdit = async () => {
    try {
      const { name } = await editForm.validateFields()
      setSaving('edit')
      await updateStudentName(editModal.student!.id, name)
      message.success('Ism yangilandi')
      setEditModal({ open: false, student: null })
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Ismni saqlashda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const applyFilters = () => {
    setPage(1)
    load(1, filters, pageSize)
  }

  const resetFilters = () => {
    const empty = { name: '', course: 0, group: '', subgroup: '', faculty: '' }
    setFilters(empty)
    setPage(1)
    load(1, empty, pageSize)
  }

  const submitRetake = async () => {
    try {
      const values = await retakeForm.validateFields()
      setSaving('retake')
      const student = retakeModal.student!
      if (values.type === 'test') {
        await grantTestRetake(student.id, Number(values.id))
      } else {
        await grantSituationalRetake(student.id, Number(values.id))
      }
      message.success('Qayta topshirish berildi')
      setRetakeModal({ open: false, student: null })
      retakeForm.resetFields()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Qayta topshirish berishda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const columns = [
    { title: 'Ism', dataIndex: 'fullName', key: 'fullName' },
    { title: 'Kurs', dataIndex: 'course', key: 'course', width: 70 },
    { title: 'Guruh', dataIndex: 'groupName', key: 'groupName' },
    { title: 'Kichik guruh', dataIndex: 'subgroup', key: 'subgroup' },
    { title: 'Fakultet', dataIndex: 'faculty', key: 'faculty' },
    {
      title: 'Amallar', key: 'actions', width: 190,
      render: (_: unknown, record: Student) => (
        <Space>
          <Tooltip title="Natijalarni ko'rish">
            <Button size="small" onClick={() => openResults(record)}>Natijalar</Button>
          </Tooltip>
          <Tooltip title="Ismni o'zgartirish">
            <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          </Tooltip>
          <Tooltip title="Qayta topshirish berish">
            <Button size="small" icon={<ReloadOutlined />} onClick={() => setRetakeModal({ open: true, student: record })} />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={4}>Talabalar</Title>
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder="Ism, ID yoki Telegram ID"
          prefix={<SearchOutlined />}
          value={filters.name}
          onChange={e => setFilters(f => ({ ...f, name: e.target.value }))}
          onPressEnter={applyFilters}
          style={{ width: 220 }}
        />
        <Select
          placeholder="Kurs"
          allowClear
          value={filters.course || undefined}
          style={{ width: 110 }}
          onChange={v => setFilters(f => ({ ...f, course: v || 0 }))}
        >
          {filterOptions.courses.map(c => <Select.Option key={c} value={c}>{c}-kurs</Select.Option>)}
        </Select>
        <AutoComplete
          allowClear
          value={filters.group || undefined}
          options={toSelectOptions(filterOptions.groups)}
          filterOption={(input, option) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())}
          onChange={v => setFilters(f => ({ ...f, group: v || '' }))}
          style={{ width: 130 }}
        >
          <Input placeholder="Guruh" />
        </AutoComplete>
        <AutoComplete
          allowClear
          value={filters.subgroup || undefined}
          options={toSelectOptions(filterOptions.subgroups)}
          filterOption={(input, option) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())}
          onChange={v => setFilters(f => ({ ...f, subgroup: v || '' }))}
          style={{ width: 140 }}
        >
          <Input placeholder="Kichik guruh" />
        </AutoComplete>
        <AutoComplete
          allowClear
          value={filters.faculty || undefined}
          options={toSelectOptions(filterOptions.faculties)}
          filterOption={(input, option) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())}
          onChange={v => setFilters(f => ({ ...f, faculty: v || '' }))}
          style={{ width: 160 }}
        >
          <Input placeholder="Fakultet" />
        </AutoComplete>
        <Button type="primary" icon={<SearchOutlined />} onClick={applyFilters}>Qidirish</Button>
        <Button onClick={resetFilters}>Tozalash</Button>
      </Space>

      <Table
        rowKey="id"
        columns={columns}
        dataSource={students}
        loading={loading}
        pagination={{
          current: page,
          total,
          pageSize,
          showSizeChanger: true,
          pageSizeOptions: [20, 50, 100],
          showTotal: t => `Jami: ${t}`,
          onChange: (p, size) => {
            setPage(p)
            setPageSize(size)
            load(p, filters, size)
          },
        }}
        scroll={{ x: 900 }}
      />

      <Modal title="Ismni o'zgartirish" open={editModal.open}
        confirmLoading={saving === 'edit'}
        onOk={submitEdit} onCancel={() => setEditModal({ open: false, student: null })}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label="Yangi ism" rules={[{ required: true, message: 'Ism kiriting' }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Qayta topshirish berish" open={retakeModal.open}
        confirmLoading={saving === 'retake'}
        onOk={submitRetake} onCancel={() => { setRetakeModal({ open: false, student: null }); retakeForm.resetFields() }}>
        <Text type="secondary">
          Hozircha test yoki topshiriq ID raqamini kiriting. ID ni kontent bo'limidagi jadvaldan ko'rish mumkin.
        </Text>
        <Form form={retakeForm} layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item name="type" label="Turi" rules={[{ required: true }]} initialValue="test">
            <Select>
              <Select.Option value="test">Test</Select.Option>
              <Select.Option value="situational">Vaziyatli topshiriq</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="id" label="Test/topshiriq ID" rules={[{ required: true, message: 'ID kiriting' }]}>
            <Input type="number" min={1} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={`${resultsModal.student?.fullName || ''} - natijalar`}
        open={resultsModal.open} footer={null} width={760}
        onCancel={() => setResultsModal({ open: false, student: null, data: [], total: 0, page: 1, pageSize: 10, loading: false })}>
        <Table
          rowKey="id"
          size="small"
          dataSource={resultsModal.data}
          loading={resultsModal.loading}
          columns={[
            { title: 'Test', dataIndex: 'testTitle' },
            { title: 'Dars', dataIndex: 'lessonTitle' },
            { title: "Bo'lim", dataIndex: 'unitName' },
            { title: 'Ball', key: 'score', render: (_, r: Record<string, unknown>) => `${r.score}/${r.maxScore}` },
            { title: 'Holat', dataIndex: 'status', render: (s: string) => <Tag color={s === 'completed' ? 'green' : s === 'timeout' ? 'orange' : 'blue'}>{s}</Tag> },
          ]}
          pagination={{
            current: resultsModal.page,
            total: resultsModal.total,
            pageSize: resultsModal.pageSize,
            showTotal: t => `Jami: ${t}`,
            onChange: (p, size) => {
              if (resultsModal.student) openResults(resultsModal.student, p, size)
            },
          }}
        />
      </Modal>
    </div>
  )
}
