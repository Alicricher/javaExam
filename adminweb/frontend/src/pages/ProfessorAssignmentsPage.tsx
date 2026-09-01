import { useEffect, useState } from 'react'
import { Table, Select, Button, Tag, Space, message, Typography } from 'antd'
import { PlusOutlined, CloseOutlined } from '@ant-design/icons'
import { getUnits, getProfessors, getProfessorAssignments, assignProfessorUnit, unassignProfessorUnit } from '../api/api'
import { useLang, pick } from '../i18n'

const { Title, Text } = Typography

interface ProfessorRow {
  id: number
  username: string
  fullName: string
}

interface UnitOption {
  id: number
  name: string
  titleUz: string
  titleRu?: string
}

interface Assignment {
  unitId: number
  name: string
  titleUz: string
  titleRu?: string
}

export default function ProfessorAssignmentsPage() {
  const lang = useLang()
  const [professors, setProfessors] = useState<ProfessorRow[]>([])
  const [units, setUnits] = useState<UnitOption[]>([])
  const [assignments, setAssignments] = useState<Record<number, Assignment[]>>({})
  const [pickerFor, setPickerFor] = useState<number | null>(null)
  const [pickedUnit, setPickedUnit] = useState<number | null>(null)
  const [loading, setLoading] = useState(false)

  const loadAssignments = async (profId: number) => {
    const res = await getProfessorAssignments(profId)
    setAssignments(prev => ({ ...prev, [profId]: res.data }))
  }

  const load = async () => {
    setLoading(true)
    try {
      const [profRes, unitRes] = await Promise.all([
        getProfessors(),
        getUnits(),
      ])
      setProfessors(profRes.data)
      setUnits(unitRes.data)
      await Promise.all((profRes.data as ProfessorRow[]).map((p) => loadAssignments(p.id)))
    } catch (e: any) {
      message.error(e?.response?.data?.error || pick(lang, 'Xatolik', 'Ошибка'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { load() }, [])

  const handleAssign = async (profId: number) => {
    if (pickedUnit == null) return
    try {
      await assignProfessorUnit(profId, pickedUnit)
      message.success(pick(lang, 'Fan biriktirildi', 'Предмет назначен'))
      setPickerFor(null)
      setPickedUnit(null)
      await loadAssignments(profId)
    } catch (e: any) {
      message.error(e?.response?.data?.error || pick(lang, 'Xatolik', 'Ошибка'))
    }
  }

  const handleUnassign = async (profId: number, unitId: number) => {
    try {
      await unassignProfessorUnit(profId, unitId)
      message.success(pick(lang, 'Fan olib tashlandi', 'Предмет удалён'))
      await loadAssignments(profId)
    } catch (e: any) {
      message.error(e?.response?.data?.error || pick(lang, 'Xatolik', 'Ошибка'))
    }
  }

  const columns = [
    { title: 'Username', dataIndex: 'username', key: 'username', width: 160 },
    { title: pick(lang, "To'liq ism", 'ФИО'), dataIndex: 'fullName', key: 'fullName', width: 200 },
    {
      title: pick(lang, 'Biriktirilgan fanlar', 'Назначенные предметы'), key: 'assignments',
      render: (_: unknown, p: ProfessorRow) => {
        const list = assignments[p.id] || []
        const assignedIds = new Set(list.map(a => a.unitId))
        const available = units.filter(u => !assignedIds.has(u.id))
        return (
          <Space wrap>
            {list.map(a => (
              <Tag key={a.unitId} closable onClose={() => handleUnassign(p.id, a.unitId)}
                   closeIcon={<CloseOutlined />}>
                {a.name} - {pick(lang, a.titleUz, a.titleRu || a.titleUz)}
              </Tag>
            ))}
            {pickerFor === p.id ? (
              <Space>
                <Select
                  size="small"
                  style={{ width: 180 }}
                  placeholder={pick(lang, 'Fan tanlang', 'Выберите предмет')}
                  value={pickedUnit ?? undefined}
                  onChange={setPickedUnit}
                  options={available.map(u => ({ value: u.id, label: `${u.name} - ${pick(lang, u.titleUz, u.titleRu || u.titleUz)}` }))}
                />
                <Button size="small" type="primary" onClick={() => handleAssign(p.id)} disabled={pickedUnit == null}>
                  {pick(lang, 'Saqlash', 'Сохранить')}
                </Button>
                <Button size="small" onClick={() => { setPickerFor(null); setPickedUnit(null) }}>
                  {pick(lang, 'Bekor qilish', 'Отмена')}
                </Button>
              </Space>
            ) : (
              <Button size="small" icon={<PlusOutlined />} onClick={() => setPickerFor(p.id)} disabled={available.length === 0}>
                {pick(lang, "Fan qo'shish", 'Добавить предмет')}
              </Button>
            )}
          </Space>
        )
      },
    },
  ]

  return (
    <div>
      <Title level={4} style={{ margin: 0, marginBottom: 4 }}>{pick(lang, 'Professorlarga fanlarni biriktirish', 'Назначение предметов профессорам')}</Title>
      <Text type="secondary">
        {pick(lang, "Har bir professor faqat o'ziga biriktirilgan fanlarni (bo'limlarni) tahrirlashi mumkin.",
          'Каждый профессор может редактировать только назначенные ему предметы (разделы).')}
      </Text>
      <Table
        dataSource={professors}
        columns={columns}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="small"
        style={{ marginTop: 16 }}
      />
    </div>
  )
}
