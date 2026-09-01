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
import { useLang, pick, type AdminLang } from '../i18n'

const { Title, Text } = Typography

interface Unit { id: number; name: string; titleUz: string; titleRu?: string }
interface Lesson { id: number; unitId: number; lessonNumber: number; titleUz: string; titleRu?: string }
interface Test { id: number; lessonId: number; titleUz: string; titleRu?: string; timeLimitMinutes: number; totalPoints: number }
interface Question { id: number; testId: number; questionText: string; points: number; orderNum: number; photoFilePath?: string }
interface AnswerOption { id: number; questionId: number; optionText: string; isCorrect: boolean; orderNum: number }
interface QuestionWithOptions { id: number; testId: number; questionText: string; points: number; orderNum: number; photoFilePath?: string; options: AnswerOption[] }
interface TheoryMaterial { id: number; lessonId: number; titleUz: string; materialType: string; filePath: string; description: string }
interface Task { id: number; lessonId: number; taskText: string; timeLimitMinutes: number; orderNum: number; photoFilePath?: string }

function materialTypes(lang: AdminLang) {
  return [
    { value: 'material', label: pick(lang, 'Material', 'Материал') },
    { value: 'book', label: pick(lang, 'Kitob', 'Книга') },
    { value: 'manual', label: pick(lang, "Qo'llanma", 'Пособие') },
  ]
}

const getErrorMessage = (err: unknown, fallback: string) => {
  const e = err as { response?: { data?: { error?: string } }; message?: string }
  return e?.response?.data?.error || e?.message || fallback
}

export default function ContentPage() {
  const lang = useLang()
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
      message.error(getErrorMessage(err, pick(lang, "Bo'limlarni yuklashda xatolik", "Ошибка при загрузке разделов")))
    }
  }

  const loadLessons = async (unitId: number) => {
    setLoading(true)
    try {
      const res = await getUnitLessons(unitId)
      setLessons(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, 'Darslarni yuklashda xatolik', 'Ошибка при загрузке уроков')))
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
        message.success(pick(lang, "Bo'lim yangilandi", "Раздел обновлён"))
      } else {
        await createUnit(values)
        message.success(pick(lang, "Bo'lim yaratildi", "Раздел создан"))
      }
      setUnitModal({ open: false, unit: null })
      loadUnits()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, "Bo'limni saqlashda xatolik", "Ошибка при сохранении раздела")))
    } finally {
      setSaving(null)
    }
  }

  const removeUnit = async (id: number) => {
    try {
      await deleteUnit(id)
      message.success(pick(lang, "Bo'lim o'chirildi", "Раздел удалён"))
      if (selectedUnit?.id === id) {
        setSelectedUnit(null)
        setSelectedLesson(null)
        setLessons([])
      }
      loadUnits()
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, "Bo'limni o'chirishda xatolik", "Ошибка при удалении раздела")))
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
        message.success(pick(lang, 'Dars yangilandi', 'Урок обновлён'))
        if (selectedLesson?.id === lessonModal.lesson.id) {
          setSelectedLesson({ ...selectedLesson, titleUz })
        }
      } else {
        await createLesson({ unitId: selectedUnit!.id, titleUz })
        message.success(pick(lang, 'Dars yaratildi', 'Урок создан'))
      }
      setLessonModal({ open: false, lesson: null })
      loadLessons(selectedUnit!.id)
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Darsni saqlashda xatolik', 'Ошибка при сохранении урока')))
    } finally {
      setSaving(null)
    }
  }

  const removeLesson = async (id: number) => {
    try {
      await deleteLesson(id)
      message.success(pick(lang, "Dars o'chirildi", "Урок удалён"))
      if (selectedLesson?.id === id) setSelectedLesson(null)
      loadLessons(selectedUnit!.id)
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, "Darsni o'chirishda xatolik", "Ошибка при удалении урока")))
    }
  }

  return (
    <div style={{ display: 'grid', gridTemplateColumns: '240px 280px minmax(0, 1fr)', gap: 16, alignItems: 'start' }}>
      <div>
        <Title level={5}>{pick(lang, "Bo'limlar", "Разделы")}</Title>
        <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => openUnitModal()} style={{ marginBottom: 8 }}>
          {pick(lang, "Bo'lim qo'shish", "Добавить раздел")}
        </Button>
        <Spin spinning={loading}>
          {units.map(u => (
            <div key={u.id} style={{
              padding: '8px 10px', cursor: 'pointer', borderRadius: 6, marginBottom: 4,
              background: selectedUnit?.id === u.id ? '#e6f4ff' : '#fafafa',
              border: '1px solid #f0f0f0',
              display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8,
            }} onClick={() => selectUnit(u)}>
              <span style={{ minWidth: 0 }}><b>{u.name}</b><br /><Text type="secondary">{pick(lang, u.titleUz, u.titleRu || u.titleUz)}</Text></span>
              <Space size={2}>
                <Button size="small" type="text" icon={<EditOutlined />} onClick={e => { e.stopPropagation(); openUnitModal(u) }} />
                <Popconfirm title={pick(lang, "Bo'limni o'chirishni tasdiqlaysizmi?", "Удалить раздел?")} onConfirm={() => removeUnit(u.id)} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
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
            <Title level={5}>{pick(lang, "Darslar", "Уроки")} - {selectedUnit.name}</Title>
            <Button size="small" type="primary" icon={<PlusOutlined />} onClick={() => openLessonModal()} style={{ marginBottom: 8 }}>
              {pick(lang, "Dars qo'shish", "Добавить урок")}
            </Button>
            {lessons.map(l => (
              <div key={l.id} style={{
                padding: '8px 10px', cursor: 'pointer', borderRadius: 6, marginBottom: 4,
                background: selectedLesson?.id === l.id ? '#e6f4ff' : '#fafafa',
                border: '1px solid #f0f0f0',
                display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 8,
              }} onClick={() => setSelectedLesson(l)}>
                <span style={{ minWidth: 0 }}>{l.lessonNumber}. {pick(lang, l.titleUz, l.titleRu || l.titleUz)}</span>
                <Space size={2}>
                  <Button size="small" type="text" icon={<EditOutlined />} onClick={e => { e.stopPropagation(); openLessonModal(l) }} />
                  <Popconfirm title={pick(lang, "Darsni o'chirishni tasdiqlaysizmi?", "Удалить урок?")} onConfirm={() => removeLesson(l.id)} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
                    <Button size="small" type="text" danger icon={<DeleteOutlined />} onClick={e => e.stopPropagation()} />
                  </Popconfirm>
                </Space>
              </div>
            ))}
          </>
        ) : (
          <Text type="secondary">{pick(lang, "Avval bo'lim tanlang.", "Сначала выберите раздел.")}</Text>
        )}
      </div>

      <div style={{ minWidth: 0 }}>
        {selectedLesson ? (
          <>
            <Title level={5}>{pick(lang, "Dars", "Урок")} {selectedLesson.lessonNumber}: {pick(lang, selectedLesson.titleUz, selectedLesson.titleRu || selectedLesson.titleUz)}</Title>
            <Tabs items={[
              { key: 'test', label: pick(lang, 'Test', 'Тест'), children: <TestTab lesson={selectedLesson} /> },
              { key: 'theory', label: pick(lang, 'Nazariya', 'Теория'), children: <TheoryTab lesson={selectedLesson} /> },
              { key: 'tasks', label: pick(lang, 'Vaziyatli topshiriqlar', 'Ситуационные задачи'), children: <TasksTab lesson={selectedLesson} /> },
            ]} />
          </>
        ) : (
          <Text type="secondary">{pick(lang, "Kontentni tahrirlash uchun dars tanlang.", "Выберите урок для редактирования контента.")}</Text>
        )}
      </div>

      <Modal title={unitModal.unit ? pick(lang, "Bo'limni tahrirlash", "Редактировать раздел") : pick(lang, "Bo'lim yaratish", "Создать раздел")}
        open={unitModal.open} confirmLoading={saving === 'unit'} onOk={submitUnit} onCancel={() => setUnitModal({ open: false, unit: null })}>
        <Form form={unitForm} layout="vertical">
          <Form.Item name="name" label={pick(lang, "Kod (masalan F1)", "Код (например F1)")} rules={[{ required: true, message: pick(lang, 'Kod kiriting', 'Введите код') }]}><Input /></Form.Item>
          <Form.Item name="titleUz" label={pick(lang, "Nomi", "Название")} rules={[{ required: true, message: pick(lang, 'Nom kiriting', 'Введите название') }]}><Input /></Form.Item>
        </Form>
      </Modal>

      <Modal title={lessonModal.lesson ? pick(lang, 'Darsni tahrirlash', 'Редактировать урок') : pick(lang, 'Dars yaratish', 'Создать урок')}
        open={lessonModal.open} confirmLoading={saving === 'lesson'} onOk={submitLesson} onCancel={() => setLessonModal({ open: false, lesson: null })}>
        <Form form={lessonForm} layout="vertical">
          <Form.Item name="titleUz" label={pick(lang, "Dars nomi", "Название урока")} rules={[{ required: true, message: pick(lang, 'Dars nomini kiriting', 'Введите название урока') }]}><Input /></Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

function TestTab({ lesson }: { lesson: Lesson }) {
  const lang = useLang()
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
      message.error(getErrorMessage(err, pick(lang, 'Savollarni yuklashda xatolik', 'Ошибка при загрузке вопросов')))
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
        message.error(getErrorMessage(err, pick(lang, 'Testni yuklashda xatolik', 'Ошибка при загрузке теста')))
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
      message.success(pick(lang, 'Test yaratildi', 'Тест создан'))
      setTestModal(false)
      loadTest()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Test yaratishda xatolik', 'Ошибка при создании теста')))
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
      message.success(pick(lang, 'Test sozlamalari yangilandi', 'Настройки теста обновлены'))
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Test sozlamalarini saqlashda xatolik', 'Ошибка при сохранении настроек теста')))
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
        message.error(pick(lang, 'Kamida 2 ta javob varianti kiriting', 'Введите минимум 2 варианта ответа'))
        return
      }
      if (!options.some(o => o.isCorrect)) {
        message.error(pick(lang, "To'g'ri javob mavjud variantlardan biri bo'lishi kerak", "Правильный ответ должен быть одним из введённых вариантов"))
        return
      }

      setSaving('question')
      const res = await createQuestion(test.id, {
        questionText: values.questionText,
        points: values.points,
        options,
      })
      message.success(pick(lang, "Savol qo'shildi", "Вопрос добавлен"))
      setQuestionModal(false)
      questionForm.resetFields()
      setSelectedQuestion(res.data)
      loadQuestions(test.id, 1)
      loadTest()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, "Savol qo'shishda xatolik", "Ошибка при добавлении вопроса")))
    } finally {
      setSaving(null)
    }
  }

  const handleImport = async (file: File) => {
    if (!test) return false
    try {
      setSaving('import')
      const res = await importQuestions(test.id, file)
      message.success(`${res.data.imported} ${pick(lang, 'ta savol import qilindi', 'вопрос(ов) импортировано')}`)
      loadQuestions(test.id, 1)
      loadTest()
    } catch (err: unknown) {
      message.error(getErrorMessage(err, pick(lang, 'Import xatosi', 'Ошибка импорта')))
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
      message.error(getErrorMessage(err, pick(lang, 'Shablonni yuklab olishda xatolik', 'Ошибка при скачивании шаблона')))
    } finally {
      setSaving(null)
    }
  }

  const handleDeleteTest = async () => {
    if (!test) return
    try {
      setSaving('delete')
      await deleteTest(test.id)
      message.success(pick(lang, "Test o'chirildi", "Тест удалён"))
      setTest(null)
      setQuestions([])
      setSelectedQuestion(null)
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, "Testni o'chirishda xatolik", "Ошибка при удалении теста")))
    } finally {
      setSaving(null)
    }
  }

  const loadQuestion = async (id: number) => {
    try {
      const res = await getQuestion(id)
      setSelectedQuestion(res.data)
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, 'Savolni yuklashda xatolik', 'Ошибка при загрузке вопроса')))
    }
  }

  const handleMove = async (id: number, direction: 'up' | 'down') => {
    if (!test) return
    try {
      await moveQuestion(id, direction)
      loadQuestions(test.id, page)
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, "Savol tartibini o'zgartirishda xatolik", "Ошибка при изменении порядка вопроса")))
    }
  }

  if (!test) return (
    <div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => { testForm.setFieldsValue({ titleUz: 'Test', timeLimitMinutes: 30 }); setTestModal(true) }}>
        {pick(lang, 'Test yaratish', 'Создать тест')}
      </Button>
      <Modal title={pick(lang, 'Test yaratish', 'Создать тест')} open={testModal} confirmLoading={saving === 'test'} onOk={handleCreateTest} onCancel={() => setTestModal(false)}>
        <Form form={testForm} layout="vertical">
          <Form.Item name="timeLimitMinutes" label={pick(lang, 'Vaqt chegarasi (daqiqa)', 'Ограничение по времени (мин.)')} rules={[{ required: true }]}>
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
        <Text>{pick(lang, 'Vaqt', 'Время')}: <b>{test.timeLimitMinutes} {pick(lang, 'daqiqa', 'мин.')}</b> | {pick(lang, 'Jami', 'Всего')}: <b>{test.totalPoints} {pick(lang, 'ball', 'баллов')}</b></Text>
        <Button icon={<EditOutlined />} size="small" onClick={() => {
          testForm.setFieldsValue({ titleUz: test.titleUz, timeLimitMinutes: test.timeLimitMinutes })
          setTestModal(true)
        }}>
          {pick(lang, 'Test sozlamalari', 'Настройки теста')}
        </Button>
        <Button type="primary" icon={<PlusOutlined />} size="small" onClick={() => {
          questionForm.setFieldsValue({ points: 1, correct: 'A' })
          setQuestionModal(true)
        }}>
          {pick(lang, "Savol qo'shish", 'Добавить вопрос')}
        </Button>
        <Upload beforeUpload={handleImport} showUploadList={false} accept=".xlsx,.csv" disabled={saving === 'import'}>
          <Button icon={<ImportOutlined />} size="small" loading={saving === 'import'}>{pick(lang, 'Import', 'Импорт')}</Button>
        </Upload>
        <Button icon={<DownloadOutlined />} size="small" loading={saving === 'template'} onClick={handleDownloadTemplate}>{pick(lang, 'Shablon', 'Шаблон')}</Button>
        <Popconfirm title={pick(lang, "Barcha savollarni o'chirishni tasdiqlaysizmi?", 'Удалить все вопросы?')} onConfirm={async () => {
          try {
            setSaving('clear')
            await clearQuestions(test.id)
            setSelectedQuestion(null)
            loadQuestions(test.id, 1)
            loadTest()
            message.success(pick(lang, 'Savollar tozalandi', 'Вопросы очищены'))
          } catch (err) {
            message.error(getErrorMessage(err, pick(lang, 'Savollarni tozalashda xatolik', 'Ошибка при очистке вопросов')))
          } finally {
            setSaving(null)
          }
        }} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
          <Button danger size="small" loading={saving === 'clear'}>{pick(lang, 'Savollarni tozalash', 'Очистить вопросы')}</Button>
        </Popconfirm>
        <Popconfirm title={pick(lang, "Testni o'chirishni tasdiqlaysizmi?", 'Удалить тест?')} onConfirm={handleDeleteTest} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
          <Button danger size="small" icon={<DeleteOutlined />}>{pick(lang, "Testni o'chirish", 'Удалить тест')}</Button>
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
            { title: pick(lang, 'Savol', 'Вопрос'), dataIndex: 'questionText', ellipsis: true },
            { title: pick(lang, 'Ball', 'Балл'), dataIndex: 'points', width: 60 },
            {
              title: '', key: 'actions', width: 110,
              render: (_: unknown, r: Question) => (
                <Space size={2}>
                  <Button size="small" type="text" icon={<ArrowUpOutlined />} onClick={e => { e.stopPropagation(); handleMove(r.id, 'up') }} />
                  <Button size="small" type="text" icon={<ArrowDownOutlined />} onClick={e => { e.stopPropagation(); handleMove(r.id, 'down') }} />
                  <Popconfirm title={pick(lang, "Savolni o'chirishni tasdiqlaysizmi?", 'Удалить вопрос?')} onConfirm={async () => {
                    try {
                      await deleteQuestion(r.id)
                      message.success(pick(lang, "Savol o'chirildi", 'Вопрос удалён'))
                      setSelectedQuestion(null)
                      loadQuestions(test.id, page)
                      loadTest()
                    } catch (err) {
                      message.error(getErrorMessage(err, pick(lang, "Savolni o'chirishda xatolik", 'Ошибка при удалении вопроса')))
                    }
                  }} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
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
          <Text type="secondary">{pick(lang, 'Tahrirlash uchun savol tanlang.', 'Выберите вопрос для редактирования.')}</Text>
        )}
      </div>

      <Modal title={pick(lang, 'Test sozlamalari', 'Настройки теста')} open={testModal} confirmLoading={saving === 'test'} onOk={handleUpdateTest} onCancel={() => setTestModal(false)} okText={pick(lang, 'Saqlash', 'Сохранить')}>
        <Form form={testForm} layout="vertical">
          <Form.Item name="titleUz" label={pick(lang, 'Test nomi', 'Название теста')} rules={[{ required: true }]}><Input /></Form.Item>
          <Form.Item name="timeLimitMinutes" label={pick(lang, 'Vaqt chegarasi (daqiqa)', 'Ограничение по времени (мин.)')} rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>

      <Modal title={pick(lang, "Savol qo'shish", 'Добавить вопрос')} open={questionModal} confirmLoading={saving === 'question'} onOk={handleCreateQuestion} onCancel={() => { setQuestionModal(false); questionForm.resetFields() }} okText={pick(lang, 'Saqlash', 'Сохранить')} width={760}>
        <Form form={questionForm} layout="vertical">
          <Form.Item name="questionText" label={pick(lang, 'Savol matni', 'Текст вопроса')} rules={[{ required: true }]}><Input.TextArea autoSize={{ minRows: 6, maxRows: 16 }} /></Form.Item>
          <Form.Item name="points" label={pick(lang, 'Ball', 'Балл')} rules={[{ required: true }]}><InputNumber min={1} style={{ width: '100%' }} /></Form.Item>
          <Form.Item name="correct" label={pick(lang, "To'g'ri javob", 'Правильный ответ')} rules={[{ required: true }]}>
            <Select options={['A', 'B', 'C', 'D', 'E'].map(v => ({ value: v, label: v }))} />
          </Form.Item>
          {['A', 'B', 'C', 'D', 'E'].map(letter => (
            <Form.Item key={letter} name={`option${letter}`} label={pick(lang, `${letter} varianti`, `Вариант ${letter}`)} rules={letter === 'A' || letter === 'B' ? [{ required: true }] : []}>
              <Input />
            </Form.Item>
          ))}
        </Form>
      </Modal>
    </div>
  )
}

function QuestionEditor({ question, onUpdate }: { question: QuestionWithOptions; onUpdate: () => void }) {
  const lang = useLang()
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
      message.success(pick(lang, 'Saqlandi', 'Сохранено'))
      onUpdate()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Savolni saqlashda xatolik', 'Ошибка при сохранении вопроса')))
    } finally {
      setSaving(false)
    }
  }

  const saveOption = async (opt: AnswerOption, text: string) => {
    if (text === opt.optionText) return
    try {
      setSavingOptionId(opt.id)
      await updateOption(question.id, opt.id, { optionText: text })
      message.success(pick(lang, 'Variant saqlandi', 'Вариант сохранён'))
      onUpdate()
    } catch (err) {
      message.error(getErrorMessage(err, pick(lang, 'Variantni saqlashda xatolik', 'Ошибка при сохранении варианта')))
    } finally {
      setSavingOptionId(null)
    }
  }

  return (
    <div style={{ border: '1px solid #e8e8e8', borderRadius: 6, padding: 12, minWidth: 0 }}>
      <Space style={{ marginBottom: 8 }}><Tag>ID: {question.id}</Tag><Tag>#{question.orderNum}</Tag></Space>
      <Form form={form} layout="vertical">
        <Form.Item name="questionText" label={pick(lang, 'Savol matni', 'Текст вопроса')}><Input.TextArea autoSize={{ minRows: 6, maxRows: 16 }} /></Form.Item>
        <Form.Item name="points" label={pick(lang, 'Ball', 'Балл')}><InputNumber min={1} /></Form.Item>
        <Button type="primary" size="small" loading={saving} onClick={save}>{pick(lang, 'Saqlash', 'Сохранить')}</Button>
      </Form>
      <div style={{ marginTop: 12 }}>
        <Text strong>{pick(lang, 'Javob variantlari:', 'Варианты ответов:')}</Text>
        {question.options?.map(opt => (
          <div key={opt.id} style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 8 }}>
            <Tag color={opt.isCorrect ? 'green' : 'default'}>{opt.isCorrect ? pick(lang, "To'g'ri", 'Верный') : String.fromCharCode(64 + opt.orderNum)}</Tag>
            <Input defaultValue={opt.optionText} disabled={savingOptionId === opt.id} style={{ flex: 1 }} onBlur={e => saveOption(opt, e.target.value)} />
            {!opt.isCorrect && <Button size="small" loading={savingOptionId === opt.id} onClick={async () => {
              try {
                setSavingOptionId(opt.id)
                await setCorrectOption(question.id, opt.id)
                message.success(pick(lang, "To'g'ri javob yangilandi", 'Правильный ответ обновлён'))
                onUpdate()
              } catch (err) {
                message.error(getErrorMessage(err, pick(lang, "To'g'ri javobni saqlashda xatolik", 'Ошибка при сохранении правильного ответа')))
              } finally {
                setSavingOptionId(null)
              }
            }}>{pick(lang, "To'g'ri", 'Сделать верным')}</Button>}
          </div>
        ))}
      </div>
      <div style={{ marginTop: 12, borderTop: '1px solid #f0f0f0', paddingTop: 10 }}>
        <Text strong>{pick(lang, 'Savol rasmi:', 'Фото вопроса:')}</Text>
        {question.photoFilePath && (
          <div style={{ marginTop: 6, marginBottom: 6 }}>
            <Tag color="blue" style={{ marginBottom: 4 }}>{pick(lang, 'Rasm yuklangan', 'Фото загружено')}</Tag>
            <Button danger size="small" loading={photoLoading} onClick={async () => {
              try {
                setPhotoLoading(true)
                await deleteQuestionPhoto(question.id)
                message.success(pick(lang, "Rasm o'chirildi", 'Фото удалено'))
                onUpdate()
              } catch (err) {
                message.error(getErrorMessage(err, pick(lang, "Rasmni o'chirishda xatolik", 'Ошибка при удалении фото')))
              } finally {
                setPhotoLoading(false)
              }
            }}>{pick(lang, "Rasmni o'chirish", 'Удалить фото')}</Button>
          </div>
        )}
        <Upload
          accept="image/*"
          showUploadList={false}
          customRequest={async ({ file, onSuccess, onError }) => {
            try {
              setPhotoLoading(true)
              await uploadQuestionPhoto(question.id, file as File)
              message.success(pick(lang, 'Rasm yuklandi', 'Фото загружено'))
              onUpdate()
              onSuccess?.('ok')
            } catch (err) {
              message.error(getErrorMessage(err, pick(lang, 'Rasmni yuklashda xatolik', 'Ошибка при загрузке фото')))
              onError?.(err as Error)
            } finally {
              setPhotoLoading(false)
            }
          }}
        >
          <Button size="small" icon={<UploadOutlined />} loading={photoLoading} style={{ marginTop: 4 }}>
            {question.photoFilePath ? pick(lang, 'Rasmni almashtirish', 'Заменить фото') : pick(lang, 'Rasm yuklash', 'Загрузить фото')}
          </Button>
        </Upload>
      </div>
    </div>
  )
}

function TheoryTab({ lesson }: { lesson: Lesson }) {
  const lang = useLang()
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
      message.error(getErrorMessage(err, pick(lang, 'Materiallarni yuklashda xatolik', 'Ошибка при загрузке материалов')))
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
      message.success(pick(lang, "Material qo'shildi", "Материал добавлен"))
      setModal(false)
      form.resetFields()
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, "Material qo'shishda xatolik", "Ошибка при добавлении материала")))
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
      message.success(pick(lang, 'Material yangilandi', 'Материал обновлён'))
      setEditModal({ open: false, material: null })
      editForm.resetFields()
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Materialni saqlashda xatolik', 'Ошибка при сохранении материала')))
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
      message.error(getErrorMessage(err, pick(lang, 'Faylni yuklab olishda xatolik', 'Ошибка при скачивании файла')))
    } finally {
      setSaving(null)
    }
  }

  return (
    <div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => { form.setFieldsValue({ materialType: 'material' }); setModal(true) }} style={{ marginBottom: 12 }}>
        {pick(lang, "Material qo'shish", 'Добавить материал')}
      </Button>
      <Table
        rowKey="id" size="small" dataSource={materials} pagination={false}
        columns={[
          { title: 'ID', dataIndex: 'id', width: 70 },
          { title: pick(lang, 'Nomi', 'Название'), dataIndex: 'titleUz' },
          { title: pick(lang, 'Turi', 'Тип'), dataIndex: 'materialType', width: 110 },
          { title: pick(lang, 'Tavsif', 'Описание'), dataIndex: 'description', ellipsis: true },
          {
            title: pick(lang, 'Amallar', 'Действия'), key: 'actions', width: 150,
            render: (_: unknown, r: TheoryMaterial) => (
              <Space>
                {r.filePath && <Button size="small" icon={<DownloadOutlined />} loading={saving === 'download'} onClick={() => handleDownload(r)} />}
                <Button size="small" icon={<EditOutlined />} onClick={() => {
                  editForm.setFieldsValue({ titleUz: r.titleUz, materialType: r.materialType, description: r.description })
                  setEditModal({ open: true, material: r })
                }} />
                <Popconfirm title={pick(lang, "Materialni o'chirishni tasdiqlaysizmi?", 'Удалить материал?')} onConfirm={async () => {
                  try {
                    setSaving('delete')
                    await deleteTheory(r.id)
                    message.success(pick(lang, "O'chirildi", 'Удалено'))
                    load()
                  } catch (err) {
                    message.error(getErrorMessage(err, pick(lang, "Materialni o'chirishda xatolik", 'Ошибка при удалении материала')))
                  } finally {
                    setSaving(null)
                  }
                }} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
                  <Button size="small" danger icon={<DeleteOutlined />} loading={saving === 'delete'} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />
      <TheoryModal title={pick(lang, "Material qo'shish", 'Добавить материал')} open={modal} form={form} confirmLoading={saving === 'create'} onOk={handleCreate} onCancel={() => { setModal(false); form.resetFields() }} />
      <TheoryModal title={pick(lang, "Materialni tahrirlash", 'Редактировать материал')} open={editModal.open} form={editForm} confirmLoading={saving === 'edit'} onOk={handleEdit} onCancel={() => { setEditModal({ open: false, material: null }); editForm.resetFields() }} />
    </div>
  )
}

function TheoryModal({ title, open, form, confirmLoading, onOk, onCancel }: { title: string; open: boolean; form: ReturnType<typeof Form.useForm>[0]; confirmLoading: boolean; onOk: () => void; onCancel: () => void }) {
  const lang = useLang()
  return (
    <Modal title={title} open={open} confirmLoading={confirmLoading} onOk={onOk} onCancel={onCancel} okText={pick(lang, 'Saqlash', 'Сохранить')}>
      <Form form={form} layout="vertical">
        <Form.Item name="titleUz" label={pick(lang, 'Nomi', 'Название')} rules={[{ required: true }]}><Input /></Form.Item>
        <Form.Item name="materialType" label={pick(lang, 'Turi', 'Тип')} initialValue="material"><Select options={materialTypes(lang)} /></Form.Item>
        <Form.Item name="description" label={pick(lang, 'Tavsif', 'Описание')}><Input.TextArea rows={2} /></Form.Item>
        <Form.Item name="file" label={pick(lang, 'Fayl', 'Файл')}>
          <Upload beforeUpload={() => false} maxCount={1}><Button icon={<UploadOutlined />}>{pick(lang, 'Fayl tanlash', 'Выбрать файл')}</Button></Upload>
        </Form.Item>
      </Form>
    </Modal>
  )
}

function TasksTab({ lesson }: { lesson: Lesson }) {
  const lang = useLang()
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
      message.error(getErrorMessage(err, pick(lang, 'Topshiriqlarni yuklashda xatolik', 'Ошибка при загрузке задач')))
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
        message.success(pick(lang, 'Topshiriq yangilandi', 'Задача обновлена'))
      } else {
        await createTask({ lessonId: lesson.id, ...values })
        message.success(pick(lang, 'Topshiriq yaratildi', 'Задача создана'))
      }
      setModal({ open: false, task: null })
      load()
    } catch (err) {
      if ((err as { errorFields?: unknown[] })?.errorFields) return
      message.error(getErrorMessage(err, pick(lang, 'Topshiriqni saqlashda xatolik', 'Ошибка при сохранении задачи')))
    } finally {
      setSaving(null)
    }
  }

  return (
    <div>
      <Button type="primary" icon={<PlusOutlined />} onClick={() => openModal()} style={{ marginBottom: 12 }}>{pick(lang, "Topshiriq qo'shish", 'Добавить задачу')}</Button>
      <Table
        rowKey="id" size="small" dataSource={tasks} pagination={false}
        columns={[
          { title: '#', dataIndex: 'orderNum', width: 50 },
          { title: 'ID', dataIndex: 'id', width: 70 },
          { title: pick(lang, 'Matn', 'Текст'), dataIndex: 'taskText', ellipsis: true },
          { title: pick(lang, 'Vaqt', 'Время'), dataIndex: 'timeLimitMinutes', width: 90, render: (v: number) => `${v} ${pick(lang, 'daq', 'мин.')}` },
          { title: pick(lang, 'Rasm', 'Фото'), dataIndex: 'photoFilePath', width: 70, render: (v: string) => v ? <Tag color="blue">✓</Tag> : <Tag>-</Tag> },
          {
            title: pick(lang, 'Amallar', 'Действия'), key: 'actions', width: 200,
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
                      message.success(pick(lang, 'Rasm yuklandi', 'Фото загружено'))
                      load()
                      onSuccess?.('ok')
                    } catch (err) {
                      message.error(getErrorMessage(err, pick(lang, 'Rasmni yuklashda xatolik', 'Ошибка при загрузке фото')))
                      onError?.(err as Error)
                    } finally {
                      setPhotoLoadingId(null)
                    }
                  }}
                >
                  <Button size="small" icon={<UploadOutlined />} loading={photoLoadingId === r.id} title={pick(lang, 'Rasm yuklash', 'Загрузить фото')} />
                </Upload>
                {r.photoFilePath && (
                  <Popconfirm title={pick(lang, "Rasmni o'chirishni tasdiqlaysizmi?", 'Удалить фото?')} onConfirm={async () => {
                    try {
                      setPhotoLoadingId(r.id)
                      await deleteTaskPhoto(r.id)
                      message.success(pick(lang, "Rasm o'chirildi", 'Фото удалено'))
                      load()
                    } catch (err) {
                      message.error(getErrorMessage(err, pick(lang, "Rasmni o'chirishda xatolik", 'Ошибка при удалении фото')))
                    } finally {
                      setPhotoLoadingId(null)
                    }
                  }} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
                    <Button size="small" danger icon={<DeleteOutlined />} loading={photoLoadingId === r.id} title={pick(lang, "Rasmni o'chirish", 'Удалить фото')} />
                  </Popconfirm>
                )}
                <Popconfirm title={pick(lang, "Topshiriqni o'chirishni tasdiqlaysizmi?", 'Удалить задачу?')} onConfirm={async () => {
                  try {
                    setSaving('delete')
                    await deleteTask(r.id)
                    message.success(pick(lang, "O'chirildi", 'Удалено'))
                    load()
                  } catch (err) {
                    message.error(getErrorMessage(err, pick(lang, "Topshiriqni o'chirishda xatolik", 'Ошибка при удалении задачи')))
                  } finally {
                    setSaving(null)
                  }
                }} okText={pick(lang, "Ha", "Да")} cancelText={pick(lang, "Yo'q", "Нет")}>
                  <Button size="small" danger icon={<DeleteOutlined />} loading={saving === 'delete'} />
                </Popconfirm>
              </Space>
            ),
          },
        ]}
      />

      <Modal title={modal.task ? pick(lang, 'Topshiriqni tahrirlash', 'Редактировать задачу') : pick(lang, 'Topshiriq yaratish', 'Создать задачу')} width={720}
        open={modal.open} confirmLoading={saving === 'submit'} onOk={handleSubmit} onCancel={() => setModal({ open: false, task: null })} okText={pick(lang, 'Saqlash', 'Сохранить')}>
        <Form form={form} layout="vertical">
          <Form.Item name="taskText" label={pick(lang, 'Topshiriq matni', 'Текст задачи')} rules={[{ required: true }]}>
            <Input.TextArea autoSize={{ minRows: 8, maxRows: 20 }} />
          </Form.Item>
          <Form.Item name="timeLimitMinutes" label={pick(lang, 'Vaqt chegarasi (daqiqa)', 'Ограничение по времени (мин.)')} rules={[{ required: true }]}>
            <InputNumber min={1} style={{ width: '100%' }} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}
