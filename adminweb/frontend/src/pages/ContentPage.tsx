import { useEffect, useState } from 'react'
import {
  Button, Modal, Form, Input, InputNumber, Select, Upload, Space,
  Typography, Tabs, Table, Tag, Popconfirm, message, Spin,
} from 'antd'
import {
  PlusOutlined, DeleteOutlined, EditOutlined, UploadOutlined,
  DownloadOutlined, ArrowUpOutlined, ArrowDownOutlined, ImportOutlined,
} from '@ant-design/icons'
import {
  getUnits, createUnit, updateUnit, deleteUnit, getUnitLessons,
  createLesson, updateLesson, deleteLesson,
  getLessonTest, createTest, updateTest, deleteTest,
  getQuestions, getQuestion, createQuestion, updateQuestion, deleteQuestion, moveQuestion, clearQuestions,
  updateOption, setCorrectOption, importQuestions, downloadTemplate,
  uploadQuestionPhoto, deleteQuestionPhoto,
  getLessonTheory, createTheory, updateTheory, deleteTheory, downloadTheoryFile,
  getLessonTasks, createTask, updateTask, deleteTask,
  uploadTaskPhoto, deleteTaskPhoto,
} from '../api/api'

const { Title, Text } = Typography

interface Unit { id: number; name: string; titleUz: string }
interface Lesson { id: number; unitId: number; lessonNumber: number; titleUz: string }
interface Test { id: number; lessonId: number; titleUz: string; timeLimitMinutes: number; totalPoints: number }
interface Question { id: number; testId: number; questionText: string; points: number; orderNum: number; photoFilePath?: string }
interface AnswerOption { id: number; questionId: number; optionText: string; isCorrect: boolean; orderNum: number }
interface QuestionWithOptions { id: number; testId: number; questionText: string; points: number; orderNum: number; photoFilePath?: string; options: AnswerOption[] }
interface TheoryMaterial { id: number; lessonId: number; titleUz: string; materialType: string; filePath: string; description: string }
interface Task { id: number; lessonId: number; taskText: string; timeLimitMinutes: number; orderNum: number; photoFilePath?: string }

const materialTypes = [
  { value: 'material', label: 'Material' },
  { value: 'book', label: 'Kitob' },
  { value: 'manual', label: "Qo'llanma" },
]

const getErrorMessage = (err: unknown, fallback: string) => {
  const e = err as { response?: { data?: { error?: string } }; message?: string }
  return e?.response?.data?.error || e?.message || fallback
}

export default function ContentPage() {
  const [units, setUnits] = useState<Unit[]>([])
  const [selectedUnit, setSelectedUnit] = useState<Unit | null>(null)
  const [selectedLesson, setSelectedLesson] = useState<Lesson | null>(null)
  const [lessons, setLessons] = useState<Lesson[]>([])
  const [loading, setLoading] = useState(false)

  const [unitModal, setUnitModal] = useState<{ open: boolean; unit: Unit | null }>({ open: false, unit: null })
  const [lessonModal, setLessonModal] = useState<{ open: boolean; lesson: Lesson | null }>({ open: false, lesson: null })
  const [saving, setSaving] = useState<'unit' | 'lesson' | null>(null)
  const [unitForm] = Form.useForm()
  const [lessonForm] = Form.useForm()

  const loadUnits = async () => {
    try {
      const res = await getUnits()
      setUnits(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, "Bo'limlarni yuklashda xatolik"))
    }
  }

  const loadLessons = async (unitId: number) => {
    setLoading(true)
    try {
      const res = await getUnitLessons(unitId)
      setLessons(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, 'Darslarni yuklashda xatolik'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadUnits() }, [])

  const selectUnit = (unit: Unit) => {
    setSelectedUnit(unit)
    setSelectedLesson(null)
    loadLessons(unit.id)
  }

  const openUnitModal = (unit?: Unit) => {
    unitForm.setFieldsValue(unit ? { name: unit.name, titleUz: unit.titleUz } : { name: '', titleUz: '' })
    setUnitModal({ open: true, unit: unit || null })
  }

  const submitUnit = async () => {
    try {
      const values = await unitForm.validateFields()
      setSaving('unit')
      if (unitModal.unit) {
        await updateUnit(unitModal.unit.id, values)
        message.success("Bo'lim yangilandi")
      } else {
        await createUnit(values)
        message.success("Bo'lim yaratildi")
      }
      setUnitModal({ open: false, unit: null })
      loadUnits()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, "Bo'limni saqlashda xatolik"))
    } finally {
      setSaving(null)
    }
  }

  const removeUnit = async (id: number) => {
    try {
      await deleteUnit(id)
      message.success("Bo'lim o'chirildi")
      if (selectedUnit?.id === id) {
        setSelectedUnit(null)
        setSelectedLesson(null)
        setLessons([])
      }
      loadUnits()
    } catch (err) {
      message.error(getErrorMessage(err, "Bo'limni o'chirishda xatolik"))
    }
  }

  const openLessonModal = (lesson?: Lesson) => {
    lessonForm.setFieldsValue(lesson ? { titleUz: lesson.titleUz } : { titleUz: '' })
    setLessonModal({ open: true, lesson: lesson || null })
  }

  const submitLesson = async () => {
    try {
      const { titleUz } = await lessonForm.validateFields()
      setSaving('lesson')
      if (lessonModal.lesson) {
        await updateLesson(lessonModal.lesson.id, titleUz)
        message.success('Dars yangilandi')
        if (selectedLesson?.id === lessonModal.lesson.id) {
          setSelectedLesson({ ...selectedLesson, titleUz })
        }
      } else {
        await createLesson({ unitId: selectedUnit!.id, titleUz })
        message.success('Dars yaratildi')
      }
      setLessonModal({ open: false, lesson: null })
      loadLessons(selectedUnit!.id)
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Darsni saqlashda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const removeLesson = async (id: number) => {
    try {
      await deleteLesson(id)
      message.success("Dars o'chirildi")
      if (selectedLesson?.id === id) setSelectedLesson(null)
      loadLessons(selectedUnit!.id)
    } catch (err) {
      message.error(getErrorMessage(err, "Darsni o'chirishda xatolik"))
    }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '240px 280px minmax(0, 1fr)', gap: 16, alignItems: 'start' }}>
      <div>
        <Title level={5}>Bo'limlar</Title>
        <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => openUnitModal()} style={{ marginBottom: 8 }}>
          Bo'lim qo'shish
        </Button>
        <Spin spinning={loading}>
          {units.map(u => (
            <div key={u.id} style={{
              padding: '8px 10px', cursor: 'pointer', borderRadius: 6, marginBottom: 4,
              background: selectedUnit?.id === u.id ? '#e6f4ff' : '#fafafa',
              border: '1px solid #f0f0f0',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8,
            }} onClick={() => selectUnit(u)}>
              <span style={{ minWidth: 0 }}><b>{u.name}</b><br /><Text type="secondary">{u.titleUz}</Text></span>
              <Space size={2}>
                <Button size="small" type="text" icon={<EditOutlined />} onClick={e => { e.stopPropagation(); openUnitModal(u) }} />
                <Popconfirm title="Bo'limni o'chirishni tasdiqlaysizmi?" onConfirm={() => removeUnit(u.id)} okText="Ha" cancelText="Yo'q">
                  <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={e => e.stopPropagation()} />
                </Popconfirm>
              </Space>
            </div>
          ))}
        </Spin>
      </div>

      <div>
        {selectedUnit ? (
          <>
            <Title level={5}>Darslar - {selectedUnit.name}</Title>
            <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => openLessonModal()} style={{ marginBottom: 8 }}>
              Dars qo'shish
            </Button>
            {lessons.map(l => (
              <div key={l.id} style={{
                padding: '8px 10px', cursor: 'pointer', borderRadius: 6, marginBottom: 4,
                background: selectedLesson?.id === l.id ? '#e6f4ff' : '#fafafa',
                border: '1px solid #f0f0f0',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8,
              }} onClick={() => setSelectedLesson(l)}>
                <span style={{ minWidth: 0 }}>{l.lessonNumber}. {l.titleUz}</span>
                <Space size={2}>
                  <Button size="small" type="text" icon={<EditOutlined />} onClick={e => { e.stopPropagation(); openLessonModal(l) }} />
                  <Popconfirm title="Darsni o'chirishni tasdiqlaysizmi?" onConfirm={() => removeLesson(l.id)} okText="Ha" cancelText="Yo'q">
                    <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={e => e.stopPropagation()} />
                  </Popconfirm>
                </Space>
              </div>
            ))}
          </>
        ) : (
          <Text type="secondary">Avval bo'lim tanlang.</Text>
        )}
      </div>

      <div style={{ minWidth: 0 }}>
        {selectedLesson ? (
          <>
            <Title level={5}>Dars {selectedLesson.lessonNumber}: {selectedLesson.titleUz}</Title>
            <Tabs items={[
              { key: 'test', label: 'Test', children: <TestTab lesson={selectedLesson} /> },
              { key: 'theory', label: 'Nazariya', children: <TheoryTab lesson={selectedLesson} /> },
              { key: 'tasks', label: 'Vaziyatli topshiriqlar', children: <TasksTab lesson={selectedLesson} /> },
            ]} />
          </>
        ) : (
          <Text type="secondary">Kontentni tahrirlash uchun dars tanlang.</Text>
        )}
      </div>

      <Modal title={unitModal.unit ? "Bo'limni tahrirlash" : "Bo'lim yaratish"}
        open={unitModal.open} confirmLoading={saving === 'unit'} onOk={submitUnit} onCancel={() => setUnitModal({ open: false, unit: null })}>
        <Form form={unitForm} layout="vertical">
          <Form.Item name="name" label="Kod (masalan F1)" rules={[{ required: true, message: 'Kod kiriting' }]}><Input /></Form.Item>
          <Form.Item name="titleUz" label="Nomi" rules={[{ required: true, message: 'Nom kiriting' }]}><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title={lessonModal.lesson ? 'Darsni tahrirlash' : 'Dars yaratish'}
        open={lessonModal.open} confirmLoading={saving === 'lesson'} onOk={submitLesson} onCancel={() => setLessonModal({ open: false, lesson: null })}>
        <Form form={lessonForm} layout="vertical">
          <Form.Item name="titleUz" label="Dars nomi" rules={[{ required: true, message: 'Dars nomini kiriting' }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function TestTab({ lesson }: { lesson: Lesson }) {
  const [test, setTest] = useState<Test | null>(null)
  const [questions, setQuestions] = useState<Question[]>([])
  const [total, setTotal] = useState(0)
  const [page, setPage] = useState(1)
  const [loading, setLoading] = useState(false)
  const [selectedQuestion, setSelectedQuestion] = useState<QuestionWithOptions | null>(null)
  const [testModal, setTestModal] = useState(false)
  const [questionModal, setQuestionModal] = useState(false)
  const [saving, setSaving] = useState<'test' | 'question' | 'import' | 'template' | 'delete' | 'clear' | null>(null)
  const [testForm] = Form.useForm()
  const [questionForm] = Form.useForm()

  const loadQuestions = async (testId: number, p: number) => {
    setLoading(true)
    try {
      const res = await getQuestions(testId, p, 15)
      setQuestions(res.data.questions)
      setTotal(res.data.total)
      setPage(p)
    } catch (err) {
      message.error(getErrorMessage(err, 'Savollarni yuklashda xatolik'))
    } finally {
      setLoading(false)
    }
  }

  const loadTest = async () => {
    try {
      const res = await getLessonTest(lesson.id)
      setTest(res.data)
      testForm.setFieldsValue({ titleUz: res.data.titleUz, timeLimitMinutes: res.data.timeLimitMinutes })
      loadQuestions(res.data.id, 1)
    } catch (err) {
      if ((err as { response?: { status?: number } })?.response?.status !== 404) {
        message.error(getErrorMessage(err, 'Testni yuklashda xatolik'))
      }
      setTest(null)
      setQuestions([])
      setSelectedQuestion(null)
    }
  }

  useEffect(() => { loadTest() }, [lesson.id])

  const handleCreateTest = async () => {
    try {
      const values = await testForm.validateFields()
      setSaving('test')
      await createTest({ lessonId: lesson.id, timeLimitMinutes: values.timeLimitMinutes })
      message.success('Test yaratildi')
      setTestModal(false)
      loadTest()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Test yaratishda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const handleUpdateTest = async () => {
    if (!test) return
    try {
      const values = await testForm.validateFields()
      setSaving('test')
      const res = await updateTest(test.id, values)
      setTest(res.data)
      setTestModal(false)
      message.success('Test sozlamalari yangilandi')
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Test sozlamalarini saqlashda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const handleCreateQuestion = async () => {
    if (!test) return
    try {
      const values = await questionForm.validateFields()
      const letters = ['A', 'B', 'C', 'D', 'E'] as const
      const options = letters
        .map(letter => ({
          optionText: String(values[`option${letter}`] || '').trim(),
          isCorrect: values.correct === letter,
        }))
        .filter(o => o.optionText)

      if (options.length < 2) {
        message.error('Kamida 2 ta javob varianti kiriting')
        return
      }
      if (!options.some(o => o.isCorrect)) {
        message.error("To'g'ri javob mavjud variantlardan biri bo'lishi kerak")
        return
      }

      setSaving('question')
      const res = await createQuestion(test.id, {
        questionText: values.questionText,
        points: values.points,
        options,
      })
      message.success("Savol qo'shildi")
      setQuestionModal(false)
      questionForm.resetFields()
      setSelectedQuestion(res.data)
      loadQuestions(test.id, 1)
      loadTest()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, "Savol qo'shishda xatolik"))
    } finally {
      setSaving(null)
    }
  }

  const handleImport = async (file: File) => {
    if (!test) return false
    try {
      setSaving('import')
      const res = await importQuestions(test.id, file)
      message.success(`${res.data.imported} ta savol import qilindi`)
      loadQuestions(test.id, 1)
      loadTest()
    } catch (err: unknown) {
      message.error(getErrorMessage(err, 'Import xatosi'))
    } finally {
      setSaving(null)
    }
    return false
  }

  const handleDownloadTemplate = async () => {
    if (!test) return
    try {
      setSaving('template')
      const res = await downloadTemplate(test.id)
      const url = URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = 'questions_template.xlsx'
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      message.error(getErrorMessage(err, 'Shablonni yuklab olishda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const handleDeleteTest = async () => {
    if (!test) return
    try {
      setSaving('delete')
      await deleteTest(test.id)
      message.success("Test o'chirildi")
      setTest(null)
      setQuestions([])
      setSelectedQuestion(null)
    } catch (err) {
      message.error(getErrorMessage(err, "Testni o'chirishda xatolik"))
    } finally {
      setSaving(null)
    }
  }

  const loadQuestion = async (id: number) => {
    try {
      const res = await getQuestion(id)
      setSelectedQuestion(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, 'Savolni yuklashda xatolik'))
    }
  }

  const handleMove = async (id: number, direction: 'up' | 'down') => {
    if (!test) return
    try {
      await moveQuestion(id, direction)
      loadQuestions(test.id, page)
    } catch (err) {
      message.error(getErrorMessage(err, "Savol tartibini o'zgartirishda xatolik"))
    }
  }

  if (!test) return (
    <div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => { testForm.setFieldsValue({ titleUz: 'Test', timeLimitMinutes: 30 }); setTestModal(true) }}>
        Test yaratish
      </Button>
      <Modal title="Test yaratish" open={testModal} confirmLoading={saving === 'test'} onOk={handleCreateTest} onCancel={() => setTestModal(false)}>
        <Form form={testForm} layout="vertical">
          <Form.Item name="timeLimitMinutes" label="Vaqt chegarasi (daqiqa)" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )

  return (
    <div>
      <Space wrap style={{ marginBottom: 12 }}>
        <Tag>ID: {test.id}</Tag>
        <Text>Vaqt: <b>{test.timeLimitMinutes} daqiqa</b> | Jami: <b>{test.totalPoints} ball</b></Text>
        <Button icon={<EditOutlined />} size="small" onClick={() => {
          testForm.setFieldsValue({ titleUz: test.titleUz, timeLimitMinutes: test.timeLimitMinutes })
          setTestModal(true)
        }}>
          Test sozlamalari
        </Button>
        <Button type="primary" icon={<PlusOutlined />} size="small" onClick={() => {
          questionForm.setFieldsValue({ points: 1, correct: 'A' })
          setQuestionModal(true)
        }}>
          Savol qo'shish
        </Button>
        <Upload beforeUpload={handleImport} showUploadList={false} accept=".xlsx,.csv" disabled={saving === 'import'}>
          <Button icon={<ImportOutlined />} size="small" loading={saving === 'import'}>Import</Button>
        </Upload>
        <Button icon={<DownloadOutlined />} size="small" loading={saving === 'template'} onClick={handleDownloadTemplate}>Shablon</Button>
        <Popconfirm title="Barcha savollarni o'chirishni tasdiqlaysizmi?" onConfirm={async () => {
          try {
            setSaving('clear')
            await clearQuestions(test.id)
            setSelectedQuestion(null)
            loadQuestions(test.id, 1)
            loadTest()
            message.success('Savollar tozalandi')
          } catch (err) {
            message.error(getErrorMessage(err, 'Savollarni tozalashda xatolik'))
          } finally {
            setSaving(null)
          }
        }} okText="Ha" cancelText="Yo'q">
          <Button danger size="small" loading={saving === 'clear'}>Savollarni tozalash</Button>
        </Popconfirm>
        <Popconfirm title="Testni o'chirishni tasdiqlaysizmi?" onConfirm={handleDeleteTest} okText="Ha" cancelText="Yo'q">
          <Button danger size="small" icon={<DeleteOutlined />}>Testni o'chirish</Button>
        </Popconfirm>
      </Space>

      <div style={{ display: 'grid', gridTemplateColumns: 'minmax(360px, 430px) minmax(0, 1fr)', gap: 16 }}>
        <Table
          rowKey="id"
          size="small"
          dataSource={questions}
          loading={loading}
          pagination={{ current: page, total, pageSize: 15, onChange: p => loadQuestions(test.id, p), size: 'small' }}
          onRow={r => ({ onClick: () => loadQuestion(r.id), style: { cursor: 'pointer', background: selectedQuestion?.id === r.id ? '#e6f4ff' : undefined } })}
          columns={[
            { title: '#', dataIndex: 'orderNum', width: 45 },
            { title: 'ID', dataIndex: 'id', width: 65 },
            { title: 'Savol', dataIndex: 'questionText', ellipsis: true },
            { title: 'Ball', dataIndex: 'points', width: 60 },
            {
              title: '', key: 'actions', width: 110,
              render: (_: unknown, r: Question) => (
                <Space size={2}>
                  <Button size="small" type="text" icon={<ArrowUpOutlined />} onClick={e => { e.stopPropagation(); handleMove(r.id, 'up') }} />
                  <Button size="small" type="text" icon={<ArrowDownOutlined />} onClick={e => { e.stopPropagation(); handleMove(r.id, 'down') }} />
                  <Popconfirm title="Savolni o'chirishni tasdiqlaysizmi?" onConfirm={async () => {
                    try {
                      await deleteQuestion(r.id)
                      message.success("Savol o'chirildi")
                      setSelectedQuestion(null)
                      loadQuestions(test.id, page)
                      loadTest()
                    } catch (err) {
                      message.error(getErrorMessage(err, "Savolni o'chirishda xatolik"))
                    }
                  }} okText="Ha" cancelText="Yo'q">
                    <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={e => e.stopPropagation()} />
                  </Popconfirm>
                </Space>
              ),
            },
          ]}
        />

        {selectedQuestion ? (
          <QuestionEditor question={selectedQuestion} onUpdate={() => { loadQuestions(test.id, page); loadQuestion(selectedQuestion.id); loadTest() }} />
        ) : (
          <Text type="secondary">Tahrirlash uchun savol tanlang.</Text>
        )}
      </div>

      <Modal title="Test sozlamalari" open={testModal} confirmLoading={saving === 'test'} onOk={handleUpdateTest} onCancel={() => setTestModal(false)} okText="Saqlash">
        <Form form={testForm} layout="vertical">
          <Form.Item name="titleUz" label="Test nomi" rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="timeLimitMinutes" label="Vaqt chegarasi (daqiqa)" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title="Savol qo'shish" open={questionModal} confirmLoading={saving === 'question'} onOk={handleCreateQuestion} onCancel={() => { setQuestionModal(false); questionForm.resetFields() }} okText="Saqlash" width={760}>
        <Form form={questionForm} layout="vertical">
          <Form.Item name="questionText" label="Savol matni" rules={[{ required: true }]}><Input.TextArea autoSize={{ minRows: 6, maxRows: 16 }} /></Form.Item>
          <Form.Item name="points" label="Ball" rules={[{ required: true }]}><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="correct" label="To'g'ri javob" rules={[{ required: true }]}>
            <Select options={['A', 'B', 'C', 'D', 'E'].map(v => ({ value: v, label: v }))} />
          </Form.Item>
          {['A', 'B', 'C', 'D', 'E'].map(letter => (
            <Form.Item key={letter} name={`option${letter}`} label={`${letter} varianti`} rules={letter === 'A' || letter === 'B' ? [{ required: true }] : []}>
              <Input />
            </Form.Item>
          ))}
        </Form>
      </Modal>
    </div>
  )
}

function QuestionEditor({ question, onUpdate }: { question: QuestionWithOptions; onUpdate: () => void }) {
  const [form] = Form.useForm()
  const [saving, setSaving] = useState(false)
  const [savingOptionId, setSavingOptionId] = useState<number | null>(null)
  const [photoLoading, setPhotoLoading] = useState(false)

  useEffect(() => {
    form.setFieldsValue({ questionText: question.questionText, points: question.points })
  }, [question.id])

  const save = async () => {
    try {
      const values = await form.validateFields()
      setSaving(true)
      await updateQuestion(question.id, values)
      message.success('Saqlandi')
      onUpdate()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Savolni saqlashda xatolik'))
    } finally {
      setSaving(false)
    }
  }

  const saveOption = async (opt: AnswerOption, text: string) => {
    if (text === opt.optionText) return
    try {
      setSavingOptionId(opt.id)
      await updateOption(question.id, opt.id, { optionText: text })
      message.success('Variant saqlandi')
      onUpdate()
    } catch (err) {
      message.error(getErrorMessage(err, 'Variantni saqlashda xatolik'))
    } finally {
      setSavingOptionId(null)
    }
  }

  return (
    <div style={{ border: '1px solid #e8e8e8', borderRadius: 6, padding: 12, minWidth: 0 }}>
      <Space style={{ marginBottom: 8 }}><Tag>ID: {question.id}</Tag><Tag>#{question.orderNum}</Tag></Space>
      <Form form={form} layout="vertical">
        <Form.Item name="questionText" label="Savol matni"><Input.TextArea autoSize={{ minRows: 6, maxRows: 16 }} /></Form.Item>
        <Form.Item name="points" label="Ball"><InputNumber min={1} /></Form.Item>
        <Button type="primary" size="small" loading={saving} onClick={save}>Saqlash</Button>
      </Form>
      <div style={{ marginTop: 12 }}>
        <Text strong>Javob variantlari:</Text>
        {question.options?.map(opt => (
          <div key={opt.id} style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 8 }}>
            <Tag color={opt.isCorrect ? 'green' : 'default'}>{opt.isCorrect ? "To'g'ri" : String.fromCharCode(64 + opt.orderNum)}</Tag>
            <Input defaultValue={opt.optionText} disabled={savingOptionId === opt.id} style={{ flex: 1 }} onBlur={e => saveOption(opt, e.target.value)} />
            {!opt.isCorrect && <Button size="small" loading={savingOptionId === opt.id} onClick={async () => {
              try {
                setSavingOptionId(opt.id)
                await setCorrectOption(question.id, opt.id)
                message.success("To'g'ri javob yangilandi")
                onUpdate()
              } catch (err) {
                message.error(getErrorMessage(err, "To'g'ri javobni saqlashda xatolik"))
              } finally {
                setSavingOptionId(null)
              }
            }}>To'g'ri</Button>}
          </div>
        ))}
      </div>
      <div style={{ marginTop: 12, borderTop: '1px solid #f0f0f0', paddingTop: 10 }}>
        <Text strong>Savol rasmi:</Text>
        {question.photoFilePath && (
          <div style={{ marginTop: 6, marginBottom: 6 }}>
            <Tag color="blue" style={{ marginBottom: 4 }}>Rasm yuklangan</Tag>
            <Button danger size="small" loading={photoLoading} onClick={async () => {
              try {
                setPhotoLoading(true)
                await deleteQuestionPhoto(question.id)
                message.success("Rasm o'chirildi")
                onUpdate()
              } catch (err) {
                message.error(getErrorMessage(err, "Rasmni o'chirishda xatolik"))
              } finally {
                setPhotoLoading(false)
              }
            }}>Rasmni o'chirish</Button>
          </div>
        )}
        <Upload
          accept="image/*"
          showUploadList={false}
          customRequest={async ({ file, onSuccess, onError }) => {
            try {
              setPhotoLoading(true)
              await uploadQuestionPhoto(question.id, file as File)
              message.success('Rasm yuklandi')
              onUpdate()
              onSuccess?.('ok')
            } catch (err) {
              message.error(getErrorMessage(err, 'Rasmni yuklashda xatolik'))
              onError?.(err as Error)
            } finally {
              setPhotoLoading(false)
            }
          }}
        >
          <Button size="small" icon={<UploadOutlined />} loading={photoLoading} style={{ marginTop: 4 }}>
            {question.photoFilePath ? 'Rasmni almashtirish' : 'Rasm yuklash'}
          </Button>
        </Upload>
      </div>
    </div>
  )
}

function TheoryTab({ lesson }: { lesson: Lesson }) {
  const [materials, setMaterials] = useState<TheoryMaterial[]>([])
  const [modal, setModal] = useState(false)
  const [editModal, setEditModal] = useState<{ open: boolean; material: TheoryMaterial | null }>({ open: false, material: null })
  const [saving, setSaving] = useState<'create' | 'edit' | 'download' | 'delete' | null>(null)
  const [form] = Form.useForm()
  const [editForm] = Form.useForm()

  const load = async () => {
    try {
      const res = await getLessonTheory(lesson.id)
      setMaterials(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, 'Materiallarni yuklashda xatolik'))
    }
  }

  useEffect(() => { load() }, [lesson.id])

  const fillFormData = (values: Record<string, unknown>, fd = new FormData()) => {
    fd.append('titleUz', String(values.titleUz || ''))
    fd.append('materialType', String(values.materialType || 'material'))
    if (values.description) fd.append('description', String(values.description))
    const fileValue = values.file as { fileList?: { originFileObj?: File }[] } | undefined
    if (fileValue?.fileList?.[0]?.originFileObj) fd.append('file', fileValue.fileList[0].originFileObj)
    return fd
  }

  const handleCreate = async () => {
    try {
      const values = await form.validateFields()
      setSaving('create')
      const fd = fillFormData(values)
      fd.append('lessonId', String(lesson.id))
      await createTheory(fd)
      message.success("Material qo'shildi")
      setModal(false)
      form.resetFields()
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, "Material qo'shishda xatolik"))
    } finally {
      setSaving(null)
    }
  }

  const handleEdit = async () => {
    try {
      const values = await editForm.validateFields()
      setSaving('edit')
      const fd = fillFormData(values)
      await updateTheory(editModal.material!.id, fd)
      message.success('Material yangilandi')
      setEditModal({ open: false, material: null })
      editForm.resetFields()
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Materialni saqlashda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  const handleDownload = async (m: TheoryMaterial) => {
    try {
      setSaving('download')
      const res = await downloadTheoryFile(m.id)
      const url = URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = m.titleUz
      a.click()
      URL.revokeObjectURL(url)
    } catch (err) {
      message.error(getErrorMessage(err, 'Faylni yuklab olishda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  return (
    <div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.setFieldsValue({ materialType: 'material' }); setModal(true) }} style={{ marginBottom: 12 }}>
        Material qo'shish
      </Button>
      <Table
        rowKey="id" size="small" dataSource={materials} pagination={false}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 70 },
          { title: 'Nomi', dataIndex: 'titleUz' },
          { title: 'Turi', dataIndex: 'materialType', width: 110 },
          { title: 'Tavsif', dataIndex: 'description', ellipsis: true },
          {
            title: 'Amallar', key: 'actions', width: 150,
            render: (_: unknown, r: TheoryMaterial) => (
              <Space>
                {r.filePath && <Button size="small" icon={<DownloadOutlined />} loading={saving === 'download'} onClick={() => handleDownload(r)} />}
                <Button size="small" icon={<EditOutlined />} onClick={() => {
                  editForm.setFieldsValue({ titleUz: r.titleUz, materialType: r.materialType, description: r.description })
                  setEditModal({ open: true, material: r })
                }} />
                <Popconfirm title="Materialni o'chirishni tasdiqlaysizmi?" onConfirm={async () => {
                  try {
                    setSaving('delete')
                    await deleteTheory(r.id)
                    message.success("O'chirildi")
                    load()
                  } catch (err) {
                    message.error(getErrorMessage(err, "Materialni o'chirishda xatolik"))
                  } finally {
                    setSaving(null)
                  }
                }} okText="Ha" cancelText="Yo'q">
                  <Button size="small" danger icon={<DeleteOutlined />} loading={saving === 'delete'} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <TheoryModal title="Material qo'shish" open={modal} form={form} confirmLoading={saving === 'create'} onOk={handleCreate} onCancel={() => { setModal(false); form.resetFields() }} />
      <TheoryModal title="Materialni tahrirlash" open={editModal.open} form={editForm} confirmLoading={saving === 'edit'} onOk={handleEdit} onCancel={() => { setEditModal({ open: false, material: null }); editForm.resetFields() }} />
    </div>
  )
}

function TheoryModal({ title, open, form, confirmLoading, onOk, onCancel }: { title: string; open: boolean; form: ReturnType<typeof Form.useForm>[0]; confirmLoading: boolean; onOk: () => void; onCancel: () => void }) {
  return (
    <Modal title={title} open={open} confirmLoading={confirmLoading} onOk={onOk} onCancel={onCancel} okText="Saqlash">
      <Form form={form} layout="vertical">
        <Form.Item name="titleUz" label="Nomi" rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="materialType" label="Turi" initialValue="material"><Select options={materialTypes} /></Form.Item>
        <Form.Item name="description" label="Tavsif"><Input.TextArea rows={2} /></Form.Item>
        <Form.Item name="file" label="Fayl">
          <Upload beforeUpload={() => false} maxCount={1}><Button icon={<UploadOutlined />}>Fayl tanlash</Button></Upload>
        </Form.Item>
      </Form>
    </Modal>
  )
}

function TasksTab({ lesson }: { lesson: Lesson }) {
  const [tasks, setTasks] = useState<Task[]>([])
  const [modal, setModal] = useState<{ open: boolean; task: Task | null }>({ open: false, task: null })
  const [saving, setSaving] = useState<'submit' | 'delete' | null>(null)
  const [photoLoadingId, setPhotoLoadingId] = useState<number | null>(null)
  const [form] = Form.useForm()

  const load = async () => {
    try {
      const res = await getLessonTasks(lesson.id)
      setTasks(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, 'Topshiriqlarni yuklashda xatolik'))
    }
  }

  useEffect(() => { load() }, [lesson.id])

  const openModal = (task?: Task) => {
    form.setFieldsValue(task ? { taskText: task.taskText, timeLimitMinutes: task.timeLimitMinutes } : { taskText: '', timeLimitMinutes: 30 })
    setModal({ open: true, task: task || null })
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setSaving('submit')
      if (modal.task) {
        await updateTask(modal.task.id, values)
        message.success('Topshiriq yangilandi')
      } else {
        await createTask({ lessonId: lesson.id, ...values })
        message.success('Topshiriq yaratildi')
      }
      setModal({ open: false, task: null })
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, 'Topshiriqni saqlashda xatolik'))
    } finally {
      setSaving(null)
    }
  }

  return (
    <div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()} style={{ marginBottom: 12 }}>Topshiriq qo'shish</Button>
      <Table
        rowKey="id" size="small" dataSource={tasks} pagination={false}
        columns={[
          { title: '#', dataIndex: 'orderNum', width: 50 },
          { title: 'ID', dataIndex: 'id', width: 70 },
          { title: 'Matn', dataIndex: 'taskText', ellipsis: true },
          { title: 'Vaqt', dataIndex: 'timeLimitMinutes', width: 90, render: (v: number) => `${v} daq` },
          { title: 'Rasm', dataIndex: 'photoFilePath', width: 70, render: (v: string) => v ? <Tag color="blue">✓</Tag> : <Tag>-</Tag> },
          {
            title: 'Amallar', key: 'actions', width: 200,
            render: (_: unknown, r: Task) => (
              <Space>
                <Button size="small" icon={<EditOutlined />} onClick={() => openModal(r)} />
                <Upload
                  accept="image/*"
                  showUploadList={false}
                  customRequest={async ({ file, onSuccess, onError }) => {
                    try {
                      setPhotoLoadingId(r.id)
                      await uploadTaskPhoto(r.id, file as File)
                      message.success('Rasm yuklandi')
                      load()
                      onSuccess?.('ok')
                    } catch (err) {
                      message.error(getErrorMessage(err, 'Rasmni yuklashda xatolik'))
                      onError?.(err as Error)
                    } finally {
                      setPhotoLoadingId(null)
                    }
                  }}
                >
                  <Button size="small" icon={<UploadOutlined />} loading={photoLoadingId === r.id} title="Rasm yuklash" />
                </Upload>
                {r.photoFilePath && (
                  <Popconfirm title="Rasmni o'chirishni tasdiqlaysizmi?" onConfirm={async () => {
                    try {
                      setPhotoLoadingId(r.id)
                      await deleteTaskPhoto(r.id)
                      message.success("Rasm o'chirildi")
                      load()
                    } catch (err) {
                      message.error(getErrorMessage(err, "Rasmni o'chirishda xatolik"))
                    } finally {
                      setPhotoLoadingId(null)
                    }
                  }} okText="Ha" cancelText="Yo'q">
                    <Button size="small" danger icon={<DeleteOutlined />} loading={photoLoadingId === r.id} title="Rasmni o'chirish" />
                  </Popconfirm>
                )}
                <Popconfirm title="Topshiriqni o'chirishni tasdiqlaysizmi?" onConfirm={async () => {
                  try {
                    setSaving('delete')
                    await deleteTask(r.id)
                    message.success("O'chirildi")
                    load()
                  } catch (err) {
                    message.error(getErrorMessage(err, "Topshiriqni o'chirishda xatolik"))
                  } finally {
                    setSaving(null)
                  }
                }} okText="Ha" cancelText="Yo'q">
                  <Button size="small" danger icon={<DeleteOutlined />} loading={saving === 'delete'} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={modal.task ? 'Topshiriqni tahrirlash' : 'Topshiriq yaratish'} width={720}
        open={modal.open} confirmLoading={saving === 'submit'} onOk={handleSubmit} onCancel={() => setModal({ open: false, task: null })} okText="Saqlash">
        <Form form={form} layout="vertical">
          <Form.Item name="taskText" label="Topshiriq matni" rules={[{ required: true }]}>
            <Input.TextArea autoSize={{ minRows: 8, maxRows: 20 }} />
          </Form.Item>
          <Form.Item name="timeLimitMinutes" label="Vaqt chegarasi (daqiqa)" rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
