import { useEffect, useMemo, useState } from 'react'
import {
  Table, Input, Button, Space, Tag, Typography, Select, Card,
  Statistic, Progress, Modal, Descriptions, Popconfirm, message,
} from 'antd'
import { SearchOutlined, EyeOutlined, ReloadOutlined } from '@ant-design/icons'
import { getLessonTest, getTestResults, getUnitLessons, getUnits, grantTestRetake } from '../api/api'

const { Title, Text } = Typography

interface TestResult {
  id: number
  studentId: number
  testId: number
  studentName: string
  testTitle: string
  lessonTitle: string
  unitName: string
  score: number
  maxScore: number
  status: 'in_progress' | 'completed' | 'timeout'
  startedAt: string
  completedAt?: string
  attemptNumber: number
}

interface Unit { id: number; name: string; titleUz: string }
interface Lesson { id: number; lessonNumber: number; titleUz: string }
interface TestInfo { id: number; titleUz: string }

const statusMeta = {
  completed: { label: 'Yakunlangan', color: 'green' },
  timeout: { label: 'Vaqt tugagan', color: 'orange' },
  in_progress: { label: 'Jarayonda', color: 'blue' },
}

const percentOf = (result: TestResult) =>
  result.maxScore > 0 ? Math.round((result.score / result.maxScore) * 100) : 0

export default function TestResultsPage() {
  const [data, setData] = useState<TestResult[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(false)
  const [page, setPage] = useState(1)
  const [filters, setFilters] = useState({
    studentName: '',
    testName: '',
    group: '',
    subgroup: '',
    status: '',
    unitId: undefined as number | undefined,
    lessonId: undefined as number | undefined,
    testId: undefined as number | undefined,
  })
  const [units, setUnits] = useState<Unit[]>([])
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [lessonTest, setLessonTest] = useState<TestInfo | null>(null)
  const [selected, setSelected] = useState<TestResult | null>(null)

  const load = async (p = page, f = filters) => {
    setLoading(true)
    try {
      const res = await getTestResults({
        page: p,
        size: 20,
        studentName: f.studentName,
        testName: f.testName,
        group: f.group,
        subgroup: f.subgroup,
        status: f.status || undefined,
        unitId: f.unitId,
        lessonId: f.lessonId,
        testId: f.testId,
      })
      setData(res.data.results)
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
    setFilters(f => ({ ...f, unitId, lessonId: undefined, testId: undefined }))
    setLessonTest(null)
    if (!unitId) {
      setLessons([])
      return
    }
    const res = await getUnitLessons(unitId)
    setLessons(res.data)
  }

  const handleLessonChange = async (lessonId?: number) => {
    setFilters(f => ({ ...f, lessonId, testId: undefined }))
    setLessonTest(null)
    if (!lessonId) return
    try {
      const res = await getLessonTest(lessonId)
      setLessonTest(res.data)
      setFilters(f => ({ ...f, testId: res.data.id }))
    } catch {
      setLessonTest(null)
    }
  }

  const pageStats = useMemo(() => {
    const completed = data.filter(r => r.status === 'completed').length
    const timeout = data.filter(r => r.status === 'timeout').length
    const inProgress = data.filter(r => r.status === 'in_progress').length
    const avg = data.length
      ? Math.round(data.reduce((sum, r) => sum + percentOf(r), 0) / data.length)
      : 0
    return { completed, timeout, inProgress, avg }
  }, [data])

  const applyFilters = () => {
    setPage(1)
    load(1, filters)
  }

  const resetFilters = () => {
    const empty = {
      studentName: '',
      testName: '',
      group: '',
      subgroup: '',
      status: '',
      unitId: undefined,
      lessonId: undefined,
      testId: undefined,
    }
    setFilters(empty)
    setLessons([])
    setLessonTest(null)
    setPage(1)
    load(1, empty)
  }

  const giveRetake = async (result: TestResult) => {
    await grantTestRetake(result.studentId, result.testId)
    message.success('Qayta topshirish berildi')
  }

  return (
    <div>
      <Title level={4}>Test natijalari</Title>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(130px, 1fr))', gap: 12, marginBottom: 16 }}>
        <Card size="small"><Statistic title="Jami topildi" value={total} /></Card>
        <Card size="small"><Statistic title="Sahifadagi o'rtacha foiz" value={pageStats.avg} suffix="%" /></Card>
        <Card size="small"><Statistic title="Sahifadagi yakunlangan" value={pageStats.completed} /></Card>
        <Card size="small"><Statistic title="Sahifadagi vaqt tugagan / jarayonda" value={`${pageStats.timeout} / ${pageStats.inProgress}`} /></Card>
      </div>

      <Space wrap style={{ marginBottom: 16 }}>
        <Input placeholder="Talaba ismi" value={filters.studentName}
          onChange={e => setFilters(f => ({ ...f, studentName: e.target.value }))} style={{ width: 210 }} />
        <Input placeholder="Test, dars yoki bo'lim" value={filters.testName}
          onChange={e => setFilters(f => ({ ...f, testName: e.target.value }))} style={{ width: 210 }} />
        <Input placeholder="Guruh" value={filters.group}
          onChange={e => setFilters(f => ({ ...f, group: e.target.value }))} style={{ width: 120 }} />
        <Input placeholder="Kichik guruh" value={filters.subgroup}
          onChange={e => setFilters(f => ({ ...f, subgroup: e.target.value }))} style={{ width: 130 }} />
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
          placeholder="Test"
          allowClear
          value={filters.testId}
          disabled={!lessonTest}
          style={{ width: 150 }}
          onChange={testId => setFilters(f => ({ ...f, testId }))}
          options={lessonTest ? [{ value: lessonTest.id, label: lessonTest.titleUz || `Test #${lessonTest.id}` }] : []}
        />
        <Select
          placeholder="Holat"
          allowClear
          value={filters.status || undefined}
          style={{ width: 150 }}
          onChange={value => setFilters(f => ({ ...f, status: value || '' }))}
          options={[
            { value: 'completed', label: 'Yakunlangan' },
            { value: 'timeout', label: 'Vaqt tugagan' },
            { value: 'in_progress', label: 'Jarayonda' },
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
          { title: 'Test', dataIndex: 'testTitle', width: 160 },
          { title: 'Dars', dataIndex: 'lessonTitle', width: 190, ellipsis: true },
          { title: "Bo'lim", dataIndex: 'unitName', width: 90 },
          {
            title: 'Natija', key: 'score', width: 170,
            render: (_: unknown, r: TestResult) => (
              <Space direction="vertical" size={0} style={{ width: '100%' }}>
                <Text>{r.score}/{r.maxScore} ({percentOf(r)}%)</Text>
                <Progress percent={percentOf(r)} size="small" showInfo={false} status={percentOf(r) >= 60 ? 'success' : 'exception'} />
              </Space>
            ),
          },
          { title: 'Urinish', dataIndex: 'attemptNumber', width: 90 },
          {
            title: 'Holat', dataIndex: 'status', width: 130,
            render: (s: TestResult['status']) => <Tag color={statusMeta[s].color}>{statusMeta[s].label}</Tag>,
          },
          { title: 'Boshlangan', dataIndex: 'startedAt', width: 170, render: (v: string) => v ? new Date(v).toLocaleString('uz') : '-' },
          {
            title: 'Amallar', key: 'actions', width: 160, fixed: 'right',
            render: (_: unknown, r: TestResult) => (
              <Space>
                <Button size="small" icon={<EyeOutlined />} onClick={() => setSelected(r)}>Ko'rish</Button>
                <Popconfirm title="Bu test uchun qayta topshirish berilsinmi?" onConfirm={() => giveRetake(r)} okText="Ha" cancelText="Yo'q">
                  <Button size="small" icon={<ReloadOutlined />} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title="Test natijasi" open={!!selected} footer={null} onCancel={() => setSelected(null)} width={680}>
        {selected && (
          <Descriptions bordered size="small" column={1}>
            <Descriptions.Item label="Talaba">{selected.studentName}</Descriptions.Item>
            <Descriptions.Item label="Test">{selected.testTitle}</Descriptions.Item>
            <Descriptions.Item label="Dars">{selected.lessonTitle}</Descriptions.Item>
            <Descriptions.Item label="Bo'lim">{selected.unitName}</Descriptions.Item>
            <Descriptions.Item label="Ball">{selected.score}/{selected.maxScore} ({percentOf(selected)}%)</Descriptions.Item>
            <Descriptions.Item label="Urinish">{selected.attemptNumber}</Descriptions.Item>
            <Descriptions.Item label="Holat">
              <Tag color={statusMeta[selected.status].color}>{statusMeta[selected.status].label}</Tag>
            </Descriptions.Item>
            <Descriptions.Item label="Boshlangan">{selected.startedAt ? new Date(selected.startedAt).toLocaleString('uz') : '-'}</Descriptions.Item>
            <Descriptions.Item label="Tugagan">{selected.completedAt ? new Date(selected.completedAt).toLocaleString('uz') : '-'}</Descriptions.Item>
            <Descriptions.Item label="ID">result: {selected.id}, student: {selected.studentId}, test: {selected.testId}</Descriptions.Item>
          </Descriptions>
        )}
      </Modal>
    </div>
  )
}
