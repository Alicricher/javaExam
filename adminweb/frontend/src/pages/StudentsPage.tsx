import { useEffect, useState } from 'react'
import {
  Table, Input, Select, Button, Space, Modal, Form, message, Typography, Tag, Tooltip, AutoComplete, Popconfirm,
} from 'antd'
import { SearchOutlined, EditOutlined, ReloadOutlined } from '@ant-design/icons'
import {
  getStudents, getStudentFilterOptions, updateStudentName, getStudentResults,
  grantTestRetake, grantSituationalRetake,
} from '../api/api'
import { useLang, pick } from '../i18n'

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
  const lang = useLang()
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
      message.error(getErrorMessage(err, pick(lang, 'Talabalarni yuklashda xatolik', 'Ошибка при загрузке студентов')))
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
      message.error(getErrorMessage(err, pick(lang, 'Natijalarni yuklashda xatolik', 'Ошибка при загрузке результатов')))
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
      message.success(pick(lang, 'Ism yangilandi', 'Имя обновлено'))
      setEditModal({ open: false, student: null })
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Ismni saqlashda xatolik', 'Ошибка при сохранении имени')))
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
      message.success(pick(lang, 'Qayta topshirish berildi', 'Пересдача предоставлена'))
      setRetakeModal({ open: false, student: null })
      retakeForm.resetFields()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Qayta topshirish berishda xatolik', 'Ошибка при предоставлении пересдачи')))
    } finally {
      setSaving(null)
    }
  }

  const columns = [
    { title: pick(lang, 'Ism', 'Имя'), dataIndex: 'fullName', key: 'fullName' },
    { title: pick(lang, 'Kurs', 'Курс'), dataIndex: 'course', key: 'course', width: 70 },
    { title: pick(lang, 'Guruh', 'Группа'), dataIndex: 'groupName', key: 'groupName' },
    { title: pick(lang, 'Kichik guruh', 'Подгруппа'), dataIndex: 'subgroup', key: 'subgroup' },
    { title: pick(lang, 'Fakultet', 'Факультет'), dataIndex: 'faculty', key: 'faculty' },
    {
      title: pick(lang, 'Amallar', 'Действия'), key: 'actions', width: 190,
      render: (_: unknown, record: Student) => (
        <Space>
          <Tooltip title={pick(lang, "Natijalarni ko'rish", 'Просмотр результатов')}>
            <Button size="small" onClick={() => openResults(record)}>{pick(lang, 'Natijalar', 'Результаты')}</Button>
          </Tooltip>
          <Tooltip title={pick(lang, "Ismni o'zgartirish", 'Изменить имя')}>
            <Button size="small" icon={<EditOutlined />} onClick={() => openEdit(record)} />
          </Tooltip>
          <Tooltip title={pick(lang, 'Qayta topshirish berish', 'Предоставить пересдачу')}>
            <Button size="small" icon={<ReloadOutlined />} onClick={() => setRetakeModal({ open: true, student: record })} />
          </Tooltip>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Title level={4}>{pick(lang, 'Talabalar', 'Студенты')}</Title>
      <Space wrap style={{ marginBottom: 16 }}>
        <Input
          placeholder={pick(lang, 'Ism, ID yoki Telegram ID', 'Имя, ID или Telegram ID')}
          prefix={<SearchOutlined />}
          value={filters.name}
          onChange={e => setFilters(f => ({ ...f, name: e.target.value }))}
          onPressEnter={applyFilters}
          style={{ width: 220 }}
        />
        <Select
          placeholder={pick(lang, 'Kurs', 'Курс')}
          allowClear
          value={filters.course || undefined}
          style={{ width: 110 }}
          onChange={v => setFilters(f => ({ ...f, course: v || 0 }))}
        >
          {filterOptions.courses.map(c => <Select.Option key={c} value={c}>{pick(lang, `${c}-kurs`, `${c} курс`)}</Select.Option>)}
        </Select>
        <AutoComplete
          allowClear
          value={filters.group || undefined}
          options={toSelectOptions(filterOptions.groups)}
          filterOption={(input, option) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())}
          onChange={v => setFilters(f => ({ ...f, group: v || '' }))}
          style={{ width: 130 }}
        >
          <Input placeholder={pick(lang, 'Guruh', 'Группа')} />
        </AutoComplete>
        <AutoComplete
          allowClear
          value={filters.subgroup || undefined}
          options={toSelectOptions(filterOptions.subgroups)}
          filterOption={(input, option) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())}
          onChange={v => setFilters(f => ({ ...f, subgroup: v || '' }))}
          style={{ width: 140 }}
        >
          <Input placeholder={pick(lang, 'Kichik guruh', 'Подгруппа')} />
        </AutoComplete>
        <AutoComplete
          allowClear
          value={filters.faculty || undefined}
          options={toSelectOptions(filterOptions.faculties)}
          filterOption={(input, option) => String(option?.value || '').toLowerCase().includes(input.toLowerCase())}
          onChange={v => setFilters(f => ({ ...f, faculty: v || '' }))}
          style={{ width: 160 }}
        >
          <Input placeholder={pick(lang, 'Fakultet', 'Факультет')} />
        </AutoComplete>
        <Button type="primary" icon={<SearchOutlined />} onClick={applyFilters}>{pick(lang, 'Qidirish', 'Поиск')}</Button>
        <Button onClick={resetFilters}>{pick(lang, 'Tozalash', 'Сбросить')}</Button>
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
          showTotal: t => pick(lang, `Jami: ${t}`, `Всего: ${t}`),
          onChange: (p, size) => {
            setPage(p)
            setPageSize(size)
            load(p, filters, size)
          },
        }}
        scroll={{ x: 900 }}
      />

      <Modal title={pick(lang, "Ismni o'zgartirish", 'Изменить имя')} open={editModal.open}
        confirmLoading={saving === 'edit'}
        onOk={submitEdit} onCancel={() => setEditModal({ open: false, student: null })}>
        <Form form={editForm} layout="vertical">
          <Form.Item name="name" label={pick(lang, 'Yangi ism', 'Новое имя')} rules={[{ required: true, message: pick(lang, 'Ism kiriting', 'Введите имя') }]}>
            <Input />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={pick(lang, 'Qayta topshirish berish', 'Предоставить пересдачу')} open={retakeModal.open}
        onCancel={() => { setRetakeModal({ open: false, student: null }); retakeForm.resetFields() }}
        footer={[
          <Button key="cancel" onClick={() => { setRetakeModal({ open: false, student: null }); retakeForm.resetFields() }}>
            {pick(lang, 'Bekor qilish', 'Отмена')}
          </Button>,
          <Popconfirm key="confirm" title={pick(lang, 'Qayta topshirish berishni tasdiqlaysizmi?', 'Подтвердить предоставление пересдачи?')}
            onConfirm={submitRetake} okText={pick(lang, 'Ha', 'Да')} cancelText={pick(lang, "Yo'q", 'Нет')}>
            <Button type="primary" loading={saving === 'retake'}>{pick(lang, 'Berish', 'Предоставить')}</Button>
          </Popconfirm>,
        ]}>
        <Text type="secondary">
          {pick(lang,
            "Hozircha test yoki topshiriq ID raqamini kiriting. ID ni kontent bo'limidagi jadvaldan ko'rish mumkin.",
            'Пока что введите ID теста или задания. ID можно посмотреть в таблице раздела «Контент».')}
        </Text>
        <Form form={retakeForm} layout="vertical" style={{ marginTop: 12 }}>
          <Form.Item name="type" label={pick(lang, 'Turi', 'Тип')} rules={[{ required: true }]} initialValue="test">
            <Select>
              <Select.Option value="test">{pick(lang, 'Test', 'Тест')}</Select.Option>
              <Select.Option value="situational">{pick(lang, 'Vaziyatli topshiriq', 'Ситуационная задача')}</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item name="id" label={pick(lang, 'Test/topshiriq ID', 'ID теста/задания')} rules={[{ required: true, message: pick(lang, 'ID kiriting', 'Введите ID') }]}>
            <Input type="number" min={1} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={pick(lang, `${resultsModal.student?.fullName || ''} - natijalar`, `${resultsModal.student?.fullName || ''} — результаты`)}
        open={resultsModal.open} footer={null} width={760}
        onCancel={() => setResultsModal({ open: false, student: null, data: [], total: 0, page: 1, pageSize: 10, loading: false })}>
        <Table
          rowKey="id"
          size="small"
          dataSource={resultsModal.data}
          loading={resultsModal.loading}
          columns={[
            { title: pick(lang, 'Test', 'Тест'), dataIndex: 'testTitle' },
            { title: pick(lang, 'Dars', 'Урок'), dataIndex: 'lessonTitle' },
            { title: pick(lang, "Bo'lim", 'Раздел'), dataIndex: 'unitName' },
            { title: pick(lang, 'Ball', 'Балл'), key: 'score', render: (_, r: Record<string, unknown>) => `${r.score}/${r.maxScore}` },
            { title: pick(lang, 'Holat', 'Статус'), dataIndex: 'status', render: (s: string) => <Tag color={s === 'completed' ? 'green' : s === 'timeout' ? 'orange' : 'blue'}>{s}</Tag> },
          ]}
          pagination={{
            current: resultsModal.page,
            total: resultsModal.total,
            pageSize: resultsModal.pageSize,
            showTotal: t => pick(lang, `Jami: ${t}`, `Всего: ${t}`),
            onChange: (p, size) => {
              if (resultsModal.student) openResults(resultsModal.student, p, size)
            },
          }}
        />
      </Modal>
    </div>
  )
}
