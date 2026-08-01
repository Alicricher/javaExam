import { useEffect, useState } from 'react'
import { Table, Input, Button, Space, Tag, Modal, Form, InputNumber, Typography, message, Select, Card, Statistic } from 'antd'
import { SearchOutlined, RobotOutlined, EditOutlined } from '@ant-design/icons'
import {
  getSituationalResults, getSituationalAnswer, gradeAnswer,
  getUnits, getUnitLessons, getLessonTasks,
} from '../api/api'

const { Title, Paragraph, Text } = Typography

interface SitResult {
  id: number
  studentName: string
  taskId: number
  taskText: string
  lessonTitle: string
  unitName: string
  answerText: string
  isGraded: boolean
  graded?: boolean
  grade: number | null
  feedback: string
  submittedAt: string
}

interface Unit { id: number; name: string; titleUz: string }
interface Lesson { id: number; lessonNumber: number; titleUz: string }
interface Task { id: number; taskText: string }

const normalizeSitResult = (result: SitResult): SitResult => ({
  ...result,
  isGraded: result.isGraded ?? result.graded ?? false,
})

export default function SituationalPage() {
  const [data, setData] = useState<SitResult[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [filters, setFilters] = useState({
    studentName: '',
    group: '',
    subgroup: '',
    graded: '',
    unitId: undefined as number | undefined,
    lessonId: undefined as number | undefined,
    taskId: undefined as number | undefined,
  })
  const [units, setUnits] = useState<Unit[]>([])
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [tasks, setTasks] = useState<Task[]>([])

  const [viewModal, setViewModal] = useState<{ open: boolean; item: SitResult | null }>({ open: false, item: null })
  const [gradeModal, setGradeModal] = useState<{ open: boolean; id: number | null; aiLoading: boolean }>({ open: false, id: null, aiLoading: false })
  const [gradeForm] = Form.useForm()

  const load = async (p = page, f = filters) => {
    setLoading(true)
    try {
      const res = await getSituationalResults({
        page: p,
        size: 20,
        studentName: f.studentName,
        group: f.group,
        subgroup: f.subgroup,
        graded: f.graded || undefined,
        unitId: f.unitId,
        lessonId: f.lessonId,
        taskId: f.taskId,
      })
      setData(res.data.results.map(normalizeSitResult))
      setTotal(res.data.total)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    load()
    getUnits().then(res => setUnits(res.data))
  }, [])

  const handleUnitChange = async (unitId?: number) => {
    setFilters(f => ({ ...f, unitId, lessonId: undefined, taskId: undefined }))
    setTasks([])
    if (!unitId) {
      setLessons([])
      return
    }
    const res = await getUnitLessons(unitId)
    setLessons(res.data)
  }

  const handleLessonChange = async (lessonId?: number) => {
    setFilters(f => ({ ...f, lessonId, taskId: undefined }))
    if (!lessonId) {
      setTasks([])
      return
    }
    const res = await getLessonTasks(lessonId)
    setTasks(res.data)
  }

  const applyFilters = () => {
    setPage(1)
    load(1, filters)
  }

  const resetFilters = () => {
    const empty = {
      studentName: '',
      group: '',
      subgroup: '',
      graded: '',
      unitId: undefined,
      lessonId: undefined,
      taskId: undefined,
    }
    setFilters(empty)
    setLessons([])
    setTasks([])
    setPage(1)
    load(1, empty)
  }

  const openView = async (item: SitResult) => {
    const res = await getSituationalAnswer(item.id)
    setViewModal({ open: true, item: normalizeSitResult({ ...item, ...res.data }) })
  }

  const handleAiGrade = async () => {
    setGradeModal(m => ({ ...m, aiLoading: true }))
    try {
      const res = await gradeAnswer(gradeModal.id!, { mode: 'ai' })
      message.success(`AI baho: ${res.data.grade}/100`)
      setGradeModal({ open: false, id: null, aiLoading: false })
      load()
    } catch {
      message.error('AI baholashda xatolik')
      setGradeModal(m => ({ ...m, aiLoading: false }))
    }
  }

  const handleManualGrade = async () => {
    const values = await gradeForm.validateFields()
    await gradeAnswer(gradeModal.id!, { mode: 'manual', grade: values.grade, feedback: values.feedback })
    message.success('Baho saqlandi')
    setGradeModal({ open: false, id: null, aiLoading: false })
    gradeForm.resetFields()
    load()
  }

  return (
    <div>
      <Title level={4}>Vaziyatli topshiriqlar</Title>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(130px, 1fr))', gap: 12, marginBottom: 16 }}>
        <Card size="small"><Statistic title="Jami topildi" value={total} /></Card>
        <Card size="small"><Statistic title="Sahifadagi baholangan" value={data.filter(r => r.isGraded).length} /></Card>
        <Card size="small"><Statistic title="Sahifadagi kutilmoqda" value={data.filter(r => !r.isGraded).length} /></Card>
      </div>

      <Space wrap style={{ marginBottom: 16 }}>
        <Input placeholder="Talaba ismi" value={filters.studentName}
          onChange={e => setFilters(f => ({ ...f, studentName: e.target.value }))} style={{ width: 220 }} />
        <Input placeholder="Guruh" value={filters.group}
          onChange={e => setFilters(f => ({ ...f, group: e.target.value }))} style={{ width: 130 }} />
        <Input placeholder="Kichik guruh" value={filters.subgroup}
          onChange={e => setFilters(f => ({ ...f, subgroup: e.target.value }))} style={{ width: 140 }} />
        <Select
          placeholder="Bo'lim"
          allowClear
          value={filters.unitId}
          style={{ width: 150 }}
          onChange={handleUnitChange}
          options={units.map(u => ({ value: u.id, label: `${u.name} - ${u.titleUz}` }))}
        />
        <Select
          placeholder="Dars"
          allowClear
          value={filters.lessonId}
          disabled={!filters.unitId}
          style={{ width: 220 }}
          onChange={handleLessonChange}
          options={lessons.map(l => ({ value: l.id, label: `${l.lessonNumber}. ${l.titleUz}` }))}
        />
        <Select
          placeholder="Topshiriq"
          allowClear
          value={filters.taskId}
          disabled={!filters.lessonId}
          style={{ width: 240 }}
          onChange={taskId => setFilters(f => ({ ...f, taskId }))}
          options={tasks.map(t => ({ value: t.id, label: t.taskText }))}
        />
        <Select
          placeholder="Holat"
          allowClear
          value={filters.graded || undefined}
          style={{ width: 150 }}
          onChange={value => setFilters(f => ({ ...f, graded: value || '' }))}
          options={[
            { value: 'true', label: 'Baholangan' },
            { value: 'false', label: 'Kutilmoqda' },
          ]}
        />
        <Button type="primary" icon={<SearchOutlined />} onClick={applyFilters}>Qidirish</Button>
        <Button onClick={resetFilters}>Tozalash</Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={data}
        loading={loading}
        scroll={{ x: 1120 }}
        pagination={{ current: page, total, pageSize: 20, onChange: p => { setPage(p); load(p) } }}
        columns={[
          { title: 'Talaba', dataIndex: 'studentName', fixed: 'left', width: 190 },
          { title: 'Topshiriq', dataIndex: 'taskText', ellipsis: true, width: 260 },
          { title: 'Dars', dataIndex: 'lessonTitle', width: 190, ellipsis: true },
          { title: "Bo'lim", dataIndex: 'unitName', width: 90 },
          {
            title: 'Holat', key: 'graded', width: 150,
            render: (_: unknown, r: SitResult) => r.isGraded
              ? <Tag color="green">Baholangan {r.grade}/100</Tag>
              : <Tag color="orange">Kutilmoqda</Tag>,
          },
          { title: 'Yuborilgan', dataIndex: 'submittedAt', width: 170, render: (v: string) => v ? new Date(v).toLocaleString('uz') : '-' },
          {
            title: 'Amallar', key: 'actions', width: 170,
            render: (_: unknown, r: SitResult) => (
              <Space>
                <Button size="small" onClick={() => openView(r)}>Ko'rish</Button>
                <Button size="small" icon={<EditOutlined />}
                  onClick={() => { setGradeModal({ open: true, id: r.id, aiLoading: false }); gradeForm.resetFields() }}>
                  Baholash
                </Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal title="Javob" open={viewModal.open} footer={null} width={680}
        onCancel={() => setViewModal({ open: false, item: null })}>
        {viewModal.item && (
          <>
            <Text strong>Topshiriq:</Text>
            <Paragraph>{viewModal.item.taskText}</Paragraph>
            <Text strong>Javob:</Text>
          <Paragraph>{viewModal.item.answerText}</Paragraph>
          {viewModal.item.isGraded && (
            <>
              <Text strong>Baho: </Text><Text>{viewModal.item.grade}/100</Text>
              {viewModal.item.feedback && <><br /><Text strong>Izoh: </Text><Text>{viewModal.item.feedback}</Text></>}
            </>
          )}
          <Paragraph type="secondary" style={{ marginTop: 12 }}>ID: answer {viewModal.item.id}, task {viewModal.item.taskId}</Paragraph>
        </>
      )}
      </Modal>

      <Modal title="Baholash" open={gradeModal.open}
        footer={[
          <Button key="ai" type="primary" icon={<RobotOutlined />}
            loading={gradeModal.aiLoading} onClick={handleAiGrade}>
            AI bilan baholash
          </Button>,
          <Button key="manual" onClick={handleManualGrade}>Qo'lda baholash</Button>,
          <Button key="cancel" onClick={() => { setGradeModal({ open: false, id: null, aiLoading: false }); gradeForm.resetFields() }}>Bekor qilish</Button>,
        ]}
        onCancel={() => { setGradeModal({ open: false, id: null, aiLoading: false }); gradeForm.resetFields() }}>
        <Form form={gradeForm} layout="vertical">
          <Form.Item name="grade" label="Ball (0-100)" rules={[{ required: true, type: 'number', min: 0, max: 100 }]}>
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="feedback" label="Izoh">
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
