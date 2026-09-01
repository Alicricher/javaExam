import { useEffect, useState } from 'react'
import { Table, Input, Button, Space, Tag, Modal, Form, InputNumber, Typography, message, Select, Card, Statistic, Divider, List } from 'antd'
import { SearchOutlined, RobotOutlined, EditOutlined } from '@ant-design/icons'
import {
  getSituationalResults, getSituationalAnswer, gradeAnswer,
  getUnits, getUnitLessons, getLessonTasks,
} from '../api/api'
import { type AdminLang, useLang, pick } from '../i18n'

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

interface Unit { id: number; name: string; titleUz: string; titleRu?: string }
interface Lesson { id: number; lessonNumber: number; titleUz: string; titleRu?: string }
interface Task { id: number; taskText: string }

interface AiBlock {
  grade: number
  feedback: string
  passed: boolean
  criteria: { name: string; points: number; comment: string }[]
  citations: string[]
  confidence: 'high' | 'medium' | 'low'
  sourceGap: boolean
}

const normalizeSitResult = (result: SitResult): SitResult => ({
  ...result,
  isGraded: result.isGraded ?? result.graded ?? false,
})

function AiResultPanel({ block, lang }: { block: AiBlock; lang: AdminLang }) {
  const confidenceColor = block.confidence === 'high' ? 'green' : block.confidence === 'medium' ? 'orange' : 'red'
  return (
    <Card size="small" title={pick(lang, "AI tavsiyasi (mahalliy + xalqaro manbalar asosida)", "Рекомендация ИИ (на основе локальных и международных источников)")} style={{ marginBottom: 12 }}>
      <Space wrap style={{ marginBottom: 8 }}>
        <Tag color={block.passed ? 'green' : 'red'}>{block.grade}/100</Tag>
        <Tag color={confidenceColor}>{pick(lang, 'ishonch', 'уверенность')}: {block.confidence}</Tag>
        {block.sourceGap && <Tag color="gold">{pick(lang, 'manbada topilmagan', 'не найдено в источнике')}</Tag>}
      </Space>
      <Paragraph style={{ marginBottom: 8 }}>{block.feedback}</Paragraph>
      {block.criteria?.length > 0 && (
        <List size="small" dataSource={block.criteria}
          renderItem={c => <List.Item style={{ padding: '4px 0' }}>
            <Text strong>{c.name}</Text> ({c.points}): <Text type="secondary">{c.comment}</Text>
          </List.Item>} />
      )}
      {block.citations?.length > 0 && (
        <Paragraph type="secondary" style={{ marginTop: 8, marginBottom: 0, fontSize: 12 }}>
          {pick(lang, 'Manbalar', 'Источники')}: {block.citations.join(', ')}
        </Paragraph>
      )}
    </Card>
  )
}

export default function SituationalPage() {
  const lang = useLang()
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
  const [aiResult, setAiResult] = useState<AiBlock | null>(null)
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
      const result: AiBlock = res.data
      setAiResult(result)
      gradeForm.setFieldsValue({ grade: result.grade, feedback: result.feedback })
    } catch {
      message.error(pick(lang, 'AI baholashda xatolik', 'Ошибка при оценке ИИ'))
    } finally {
      setGradeModal(m => ({ ...m, aiLoading: false }))
    }
  }

  const closeGradeModal = () => {
    setGradeModal({ open: false, id: null, aiLoading: false })
    setAiResult(null)
    gradeForm.resetFields()
  }

  const handleManualGrade = async () => {
    const values = await gradeForm.validateFields()
    await gradeAnswer(gradeModal.id!, {
      mode: 'manual',
      grade: values.grade,
      feedback: values.feedback,
      citations: aiResult?.citations,
    })
    message.success(pick(lang, 'Baho saqlandi', 'Оценка сохранена'))
    closeGradeModal()
    load()
  }

  return (
    <div>
      <Title level={4}>{pick(lang, 'Vaziyatli topshiriqlar', 'Ситуационные задачи')}</Title>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, minmax(130px, 1fr))', gap: 12, marginBottom: 16 }}>
        <Card size="small"><Statistic title={pick(lang, 'Jami topildi', 'Всего найдено')} value={total} /></Card>
        <Card size="small"><Statistic title={pick(lang, 'Sahifadagi baholangan', 'Оценено на странице')} value={data.filter(r => r.isGraded).length} /></Card>
        <Card size="small"><Statistic title={pick(lang, 'Sahifadagi kutilmoqda', 'Ожидает на странице')} value={data.filter(r => !r.isGraded).length} /></Card>
      </div>

      <Space wrap style={{ marginBottom: 16 }}>
        <Input placeholder={pick(lang, 'Talaba ismi', 'Имя студента')} value={filters.studentName}
          onChange={e => setFilters(f => ({ ...f, studentName: e.target.value }))} style={{ width: 220 }} />
        <Input placeholder={pick(lang, 'Guruh', 'Группа')} value={filters.group}
          onChange={e => setFilters(f => ({ ...f, group: e.target.value }))} style={{ width: 130 }} />
        <Input placeholder={pick(lang, 'Kichik guruh', 'Подгруппа')} value={filters.subgroup}
          onChange={e => setFilters(f => ({ ...f, subgroup: e.target.value }))} style={{ width: 140 }} />
        <Select
          placeholder={pick(lang, "Bo'lim", 'Раздел')}
          allowClear
          value={filters.unitId}
          style={{ width: 150 }}
          onChange={handleUnitChange}
          options={units.map(u => ({ value: u.id, label: `${u.name} - ${pick(lang, u.titleUz, u.titleRu || u.titleUz)}` }))}
        />
        <Select
          placeholder={pick(lang, 'Dars', 'Урок')}
          allowClear
          value={filters.lessonId}
          disabled={!filters.unitId}
          style={{ width: 220 }}
          onChange={handleLessonChange}
          options={lessons.map(l => ({ value: l.id, label: `${l.lessonNumber}. ${pick(lang, l.titleUz, l.titleRu || l.titleUz)}` }))}
        />
        <Select
          placeholder={pick(lang, 'Topshiriq', 'Задача')}
          allowClear
          value={filters.taskId}
          disabled={!filters.lessonId}
          style={{ width: 240 }}
          onChange={taskId => setFilters(f => ({ ...f, taskId }))}
          options={tasks.map(t => ({ value: t.id, label: t.taskText }))}
        />
        <Select
          placeholder={pick(lang, 'Holat', 'Статус')}
          allowClear
          value={filters.graded || undefined}
          style={{ width: 150 }}
          onChange={value => setFilters(f => ({ ...f, graded: value || '' }))}
          options={[
            { value: 'true', label: pick(lang, 'Baholangan', 'Оценено') },
            { value: 'false', label: pick(lang, 'Kutilmoqda', 'Ожидает') },
          ]}
        />
        <Button type="primary" icon={<SearchOutlined />} onClick={applyFilters}>{pick(lang, 'Qidirish', 'Искать')}</Button>
        <Button onClick={resetFilters}>{pick(lang, 'Tozalash', 'Сбросить')}</Button>
      </Space>
      <Table
        rowKey="id"
        dataSource={data}
        loading={loading}
        scroll={{ x: 1120 }}
        pagination={{ current: page, total, pageSize: 20, onChange: p => { setPage(p); load(p) } }}
        columns={[
          { title: pick(lang, 'Talaba', 'Студент'), dataIndex: 'studentName', fixed: 'left', width: 190 },
          { title: pick(lang, 'Topshiriq', 'Задача'), dataIndex: 'taskText', ellipsis: true, width: 260 },
          { title: pick(lang, 'Dars', 'Урок'), dataIndex: 'lessonTitle', width: 190, ellipsis: true },
          { title: pick(lang, "Bo'lim", 'Раздел'), dataIndex: 'unitName', width: 90 },
          {
            title: pick(lang, 'Holat', 'Статус'), key: 'graded', width: 150,
            render: (_: unknown, r: SitResult) => r.isGraded
              ? <Tag color="green">{pick(lang, 'Baholangan', 'Оценено')} {r.grade}/100</Tag>
              : <Tag color="orange">{pick(lang, 'Kutilmoqda', 'Ожидает')}</Tag>,
          },
          { title: pick(lang, 'Yuborilgan', 'Отправлено'), dataIndex: 'submittedAt', width: 170, render: (v: string) => v ? new Date(v).toLocaleString(lang === 'ru' ? 'ru' : 'uz') : '-' },
          {
            title: pick(lang, 'Amallar', 'Действия'), key: 'actions', width: 170,
            render: (_: unknown, r: SitResult) => (
              <Space>
                <Button size="small" onClick={() => openView(r)}>{pick(lang, "Ko'rish", 'Просмотр')}</Button>
                <Button size="small" icon={<EditOutlined />}
                  onClick={() => { setGradeModal({ open: true, id: r.id, aiLoading: false }); setAiResult(null); gradeForm.resetFields() }}>
                  {pick(lang, 'Baholash', 'Оценить')}
                </Button>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={pick(lang, 'Javob', 'Ответ')} open={viewModal.open} footer={null} width={680}
        onCancel={() => setViewModal({ open: false, item: null })}>
        {viewModal.item && (
          <>
            <Text strong>{pick(lang, 'Topshiriq:', 'Задача:')}</Text>
            <Paragraph>{viewModal.item.taskText}</Paragraph>
            <Text strong>{pick(lang, 'Javob:', 'Ответ:')}</Text>
          <Paragraph>{viewModal.item.answerText}</Paragraph>
          {viewModal.item.isGraded && (
            <>
              <Text strong>{pick(lang, 'Baho: ', 'Оценка: ')}</Text><Text>{viewModal.item.grade}/100</Text>
              {viewModal.item.feedback && <><br /><Text strong>{pick(lang, 'Izoh: ', 'Комментарий: ')}</Text><Text>{viewModal.item.feedback}</Text></>}
            </>
          )}
          <Paragraph type="secondary" style={{ marginTop: 12 }}>ID: answer {viewModal.item.id}, task {viewModal.item.taskId}</Paragraph>
        </>
      )}
      </Modal>

      <Modal title={pick(lang, 'Baholash', 'Оценивание')} open={gradeModal.open} width={720}
        footer={[
          <Button key="ai" type="primary" icon={<RobotOutlined />}
            loading={gradeModal.aiLoading} onClick={handleAiGrade}>
            {pick(lang, 'AI bilan baholash', 'Оценить с помощью ИИ')}
          </Button>,
          <Button key="manual" onClick={handleManualGrade}>{pick(lang, "Qo'lda baholash (saqlash)", 'Оценить вручную (сохранить)')}</Button>,
          <Button key="cancel" onClick={closeGradeModal}>{pick(lang, 'Bekor qilish', 'Отмена')}</Button>,
        ]}
        onCancel={closeGradeModal}>
        {aiResult && (
          <>
            <AiResultPanel block={aiResult} lang={lang} />
            <Paragraph type="secondary">
              {pick(lang,
                "AI tavsiyasi pastdagi formaga avtomatik qo'yildi — kerak bo'lsa tahrirlang, so'ng \"Qo'lda baholash (saqlash)\" bosing — faqat shu bosqichda baho saqlanadi.",
                'Рекомендация ИИ автоматически подставлена в форму ниже — при необходимости отредактируйте, затем нажмите «Оценить вручную (сохранить)» — оценка сохраняется только на этом шаге.')}
            </Paragraph>
            <Divider />
          </>
        )}
        <Form form={gradeForm} layout="vertical">
          <Form.Item name="grade" label={pick(lang, 'Ball (0-100)', 'Балл (0-100)')} rules={[{ required: true, type: 'number', min: 0, max: 100 }]}>
            <InputNumber min={0} max={100} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item name="feedback" label={pick(lang, 'Izoh', 'Комментарий')}>
            <Input.TextArea autoSize={{ minRows: 3, maxRows: 12 }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
