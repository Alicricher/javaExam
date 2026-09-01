import { useEffect, useMemo, useState } from 'react'
import {
  Table, Input, Button, Space, Tag, Typography, Select, Card,
  Statistic, Progress, Modal, Descriptions, Popconfirm, message, Tabs,
} from 'antd'
import { SearchOutlined, EyeOutlined, ReloadOutlined, BarChartOutlined, PrinterOutlined } from '@ant-design/icons'
import { getLessonTest, getTestResultAnswers, getTestResults, getTestGroupStats, getTestStudentDetails, getUnitLessons, getUnits, grantTestRetake } from '../api/api'

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
interface GroupStat {
  group_name: string
  subgroup: string
  student_count: number
  passed_count: number
  avg_score_pct: number
}
interface StudentDetail {
  full_name: string
  group_name: string
  subgroup: string
  score: number
  max_score: number
  passed: boolean
}
interface TestAnswerDetail {
  questionId: number
  orderNum: number
  questionText: string
  points: number
  selectedOptionId: number | null
  selectedOptionText: string | null
  correctOptionId: number | null
  correctOptionText: string | null
  isCorrect: boolean
  answeredAt?: string
}

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
  const [answerDetails, setAnswerDetails] = useState<TestAnswerDetail[]>([])
  const [answersLoading, setAnswersLoading] = useState(false)
  const [groupStats, setGroupStats] = useState<GroupStat[]>([])
  const [groupStatsLoading, setGroupStatsLoading] = useState(false)
  const [studentDetails, setStudentDetails] = useState<StudentDetail[]>([])
  const [activeTab, setActiveTab] = useState('results')

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
    setGroupStats([])
    if (!lessonId) return
    try {
      const res = await getLessonTest(lessonId)
      setLessonTest(res.data)
      setFilters(f => ({ ...f, testId: res.data.id }))
    } catch {
      setLessonTest(null)
    }
  }

  const loadGroupStats = async (testId: number) => {
    setGroupStatsLoading(true)
    try {
      const [statsRes, detailsRes] = await Promise.all([
        getTestGroupStats(testId),
        getTestStudentDetails(testId),
      ])
      setGroupStats(statsRes.data)
      setStudentDetails(detailsRes.data)
    } catch {
      setGroupStats([])
      setStudentDetails([])
    } finally {
      setGroupStatsLoading(false)
    }
  }

  const exportGroupStatsPdf = () => {
    if (!groupStats.length) return
    const testTitle = lessonTest?.titleUz || `Test #${filters.testId}`
    const date = new Date().toLocaleDateString('uz-UZ', { year: 'numeric', month: 'long', day: 'numeric' })
    const totalStudents = groupStats.reduce((s, r) => s + r.student_count, 0)
    const totalPassed = groupStats.reduce((s, r) => s + r.passed_count, 0)
    const overallPct = totalStudents > 0 ? Math.round(totalPassed / totalStudents * 100) : 0

    const rows = groupStats.map(r => {
      const passPct = r.student_count > 0 ? Math.round(r.passed_count / r.student_count * 100) : 0
      return `<tr>
        <td>${r.group_name}</td>
        <td>${r.subgroup}</td>
        <td>${r.student_count}</td>
        <td>${r.passed_count} / ${r.student_count} (${passPct}%)</td>
        <td>${Number(r.avg_score_pct) || 0}%</td>
      </tr>`
    }).join('')

    // Per-student breakdown, grouped the same way as the summary table above it,
    // so a reader can see exactly who passed and who didn't in each group.
    const byGroup = new Map<string, StudentDetail[]>()
    for (const s of studentDetails) {
      const key = `${s.group_name} / ${s.subgroup}`
      if (!byGroup.has(key)) byGroup.set(key, [])
      byGroup.get(key)!.push(s)
    }
    const detailSections = Array.from(byGroup.entries()).map(([groupKey, students]) => {
      const studentRows = students.map(s => {
        const pct = s.max_score > 0 ? Math.round(s.score / s.max_score * 100) : 0
        return `<tr>
          <td>${s.full_name}</td>
          <td>${s.score} / ${s.max_score} (${pct}%)</td>
          <td class="${s.passed ? 'pass' : 'fail'}">${s.passed ? "O'tdi" : "O'tmadi"}</td>
        </tr>`
      }).join('')
      return `<h3>${groupKey}</h3>
        <table>
          <thead><tr><th>F.I.O.</th><th>Ball</th><th>Natija</th></tr></thead>
          <tbody>${studentRows}</tbody>
        </table>`
    }).join('')

    const html = `<!DOCTYPE html><html><head><meta charset="utf-8">
      <title>Guruh statistikasi — ${testTitle}</title>
      <style>
        body { font-family: Arial, sans-serif; margin: 24px; color: #000; }
        h2 { font-size: 16px; margin-bottom: 4px; }
        h3 { font-size: 13px; margin: 18px 0 6px; }
        .meta { font-size: 12px; color: #555; margin-bottom: 16px; }
        table { width: 100%; border-collapse: collapse; font-size: 13px; margin-bottom: 8px; }
        th { background: #1677ff; color: #fff; padding: 8px 10px; text-align: left; }
        td { padding: 6px 10px; border-bottom: 1px solid #e0e0e0; }
        tr:last-child td { border-bottom: none; }
        .summary { background: #f0f4ff; font-weight: bold; }
        .pass { color: #237804; font-weight: bold; }
        .fail { color: #a8071a; font-weight: bold; }
        @media print { body { margin: 0; } h3 { page-break-after: avoid; } table { page-break-inside: auto; } tr { page-break-inside: avoid; } }
      </style>
    </head><body>
      <h2>Guruh statistikasi: ${testTitle}</h2>
      <div class="meta">Sana: ${date}</div>
      <table>
        <thead><tr>
          <th>Guruh</th><th>Kichik guruh</th><th>Talabalar</th><th>O'tdi</th><th>O'rtacha %</th>
        </tr></thead>
        <tbody>${rows}</tbody>
        <tfoot><tr class="summary">
          <td colspan="2">Jami</td>
          <td>${totalStudents}</td>
          <td>${totalPassed} / ${totalStudents} (${overallPct}%)</td>
          <td>—</td>
        </tr></tfoot>
      </table>
      ${detailSections}
    </body></html>`

    const win = window.open('', '_blank')
    if (!win) { message.error("Brauzer popup'ni blokladi. Ruxsat bering va qayta urinib ko'ring."); return }
    win.document.write(html)
    win.document.close()
    win.onload = () => { win.focus(); win.print() }
  }

  const pageStats = useMemo(() => {
    const completed = data.filter(r => r.status === 'completed')
    const timeout = data.filter(r => r.status === 'timeout').length
    const inProgress = data.filter(r => r.status === 'in_progress').length
    const passed = completed.filter(r => percentOf(r) >= 60).length
    const avg = completed.length
      ? Math.round(completed.reduce((sum, r) => sum + percentOf(r), 0) / completed.length)
      : 0
    return { completed: completed.length, timeout, inProgress, passed, avg }
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

  const openResult = async (result: TestResult) => {
    setSelected(result)
    setAnswerDetails([])
    setAnswersLoading(true)
    try {
      const res = await getTestResultAnswers(result.id)
      setAnswerDetails(res.data)
    } finally {
      setAnswersLoading(false)
    }
  }

  return (
    <div>
      <Title level={4}>Test natijalari</Title>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, minmax(130px, 1fr))', gap: 12, marginBottom: 16 }}>
        <Card size="small"><Statistic title="Jami topildi" value={total} /></Card>
        <Card size="small"><Statistic title="Yakunlangan (o'rtacha %)" value={pageStats.avg} suffix="%" /></Card>
        <Card size="small"><Statistic title="Yakunlangan / o'tdi (sahifa)" value={`${pageStats.completed} / ${pageStats.passed}`} /></Card>
        <Card size="small"><Statistic title="Vaqt tugagan / jarayonda" value={`${pageStats.timeout} / ${pageStats.inProgress}`} /></Card>
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

      <Tabs
        activeKey={activeTab}
        onChange={key => {
          setActiveTab(key)
          if (key === 'groups' && filters.testId) loadGroupStats(filters.testId)
        }}
        items={[
          {
            key: 'results',
            label: 'Natijalar',
            children: (
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
                        <Button size="small" icon={<EyeOutlined />} onClick={() => openResult(r)}>Ko'rish</Button>
                        <Popconfirm title="Bu test uchun qayta topshirish berilsinmi?" onConfirm={() => giveRetake(r)} okText="Ha" cancelText="Yo'q">
                          <Button size="small" icon={<ReloadOutlined />} />
                        </Popconfirm>
                      </Space>
                    ),
                  },
                ]}
              />
            ),
          },
          {
            key: 'groups',
            label: <span><BarChartOutlined /> Guruh statistikasi</span>,
            children: (
              <div>
                {!filters.testId && (
                  <Typography.Text type="secondary">Guruh statistikasini ko'rish uchun test tanlang (dars filtri orqali)</Typography.Text>
                )}
                {filters.testId && (
                  <>
                    <Space style={{ marginBottom: 12 }}>
                      <Button
                        size="small"
                        onClick={() => loadGroupStats(filters.testId!)}
                        loading={groupStatsLoading}
                      >Yangilash</Button>
                      <Button
                        size="small"
                        icon={<PrinterOutlined />}
                        onClick={exportGroupStatsPdf}
                        disabled={!groupStats.length}
                        type="default"
                      >PDF eksport</Button>
                    </Space>
                    <Table
                      rowKey={r => `${r.group_name}_${r.subgroup}`}
                      dataSource={groupStats}
                      loading={groupStatsLoading}
                      pagination={false}
                      size="middle"
                      columns={[
                        { title: 'Guruh', dataIndex: 'group_name', width: 150 },
                        { title: 'Kichik guruh', dataIndex: 'subgroup', width: 130 },
                        { title: "Talabalar soni", dataIndex: 'student_count', width: 140 },
                        {
                          title: "O'tdi", key: 'passed',
                          render: (_: unknown, r: GroupStat) => (
                            <span>
                              {r.passed_count}/{r.student_count}{' '}
                              <Tag color={r.student_count > 0 && r.passed_count / r.student_count >= 0.6 ? 'green' : 'red'}>
                                {r.student_count > 0 ? Math.round(r.passed_count / r.student_count * 100) : 0}%
                              </Tag>
                            </span>
                          ),
                        },
                        {
                          title: "O'rtacha ball", dataIndex: 'avg_score_pct',
                          render: (v: number) => (
                            <Space>
                              <Progress
                                percent={Number(v) || 0}
                                size="small"
                                style={{ width: 120 }}
                                status={Number(v) >= 60 ? 'success' : 'exception'}
                              />
                            </Space>
                          ),
                        },
                      ]}
                      summary={tableData => {
                        const totalStudents = tableData.reduce((s, r) => s + r.student_count, 0)
                        const totalPassed = tableData.reduce((s, r) => s + r.passed_count, 0)
                        const overallPct = totalStudents > 0 ? Math.round(totalPassed / totalStudents * 100) : 0
                        return (
                          <Table.Summary.Row>
                            <Table.Summary.Cell index={0} colSpan={2}><strong>Jami</strong></Table.Summary.Cell>
                            <Table.Summary.Cell index={2}><strong>{totalStudents}</strong></Table.Summary.Cell>
                            <Table.Summary.Cell index={3}>
                              <strong>{totalPassed}/{totalStudents}</strong>{' '}
                              <Tag color={overallPct >= 60 ? 'green' : 'red'}>{overallPct}%</Tag>
                            </Table.Summary.Cell>
                            <Table.Summary.Cell index={4} />
                          </Table.Summary.Row>
                        )
                      }}
                    />
                  </>
                )}
              </div>
            ),
          },
        ]}
      />

      <Modal title="Test natijasi" open={!!selected} footer={null}
        onCancel={() => { setSelected(null); setAnswerDetails([]) }} width={1100}>
        {selected && (
          <Space direction="vertical" size={16} style={{ width: '100%' }}>
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
            <Table
              rowKey="questionId"
              size="small"
              loading={answersLoading}
              dataSource={answerDetails}
              pagination={false}
              scroll={{ x: 960 }}
              columns={[
                { title: '#', dataIndex: 'orderNum', width: 58 },
                {
                  title: 'Savol', dataIndex: 'questionText', width: 280,
                  render: (v: string) => <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{v}</div>,
                },
                {
                  title: 'Talaba javobi', dataIndex: 'selectedOptionText', width: 240,
                  render: (v: string | null) => v
                    ? <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{v}</div>
                    : <Text type="secondary">Javob berilmagan</Text>,
                },
                {
                  title: "To'g'ri javob", dataIndex: 'correctOptionText', width: 240,
                  render: (v: string | null) => v
                    ? <div style={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{v}</div>
                    : '-',
                },
                {
                  title: 'Natija', key: 'isCorrect', width: 120,
                  render: (_: unknown, a: TestAnswerDetail) => a.selectedOptionId == null
                    ? <Tag>Berilmagan</Tag>
                    : a.isCorrect ? <Tag color="green">To'g'ri</Tag> : <Tag color="red">Noto'g'ri</Tag>,
                },
                { title: 'Ball', dataIndex: 'points', width: 70 },
              ]}
            />
          </Space>
        )}
      </Modal>
    </div>
  )
}
