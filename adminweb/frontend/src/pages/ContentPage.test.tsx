import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ContentPage from './ContentPage'
import {
  createTheory,
  getLessonTasks,
  getLessonTest,
  getLessonTheory,
  getQuestion,
  getQuestions,
  getUnitLessons,
  getUnits,
  updateTest,
} from '../api/api'

vi.mock('../api/api', () => ({
  getUnits: vi.fn(),
  createUnit: vi.fn(),
  updateUnit: vi.fn(),
  deleteUnit: vi.fn(),
  getUnitLessons: vi.fn(),
  createLesson: vi.fn(),
  updateLesson: vi.fn(),
  deleteLesson: vi.fn(),
  getLessonTest: vi.fn(),
  createTest: vi.fn(),
  updateTest: vi.fn(),
  deleteTest: vi.fn(),
  getQuestions: vi.fn(),
  getQuestion: vi.fn(),
  createQuestion: vi.fn(),
  updateQuestion: vi.fn(),
  deleteQuestion: vi.fn(),
  moveQuestion: vi.fn(),
  clearQuestions: vi.fn(),
  updateOption: vi.fn(),
  setCorrectOption: vi.fn(),
  importQuestions: vi.fn(),
  downloadTemplate: vi.fn(),
  getLessonTheory: vi.fn(),
  createTheory: vi.fn(),
  updateTheory: vi.fn(),
  deleteTheory: vi.fn(),
  downloadTheoryFile: vi.fn(),
  getLessonTasks: vi.fn(),
  createTask: vi.fn(),
  updateTask: vi.fn(),
  deleteTask: vi.fn(),
}))

const axiosResponse = <T,>(data: T) => ({
  data,
  status: 200,
  statusText: 'OK',
  headers: {},
  config: {},
}) as AxiosResponse<T>

describe('ContentPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(getUnits).mockResolvedValue(axiosResponse([
      { id: 1, name: 'F1', titleUz: 'Stomatologiya' },
    ]))
    vi.mocked(getUnitLessons).mockResolvedValue(axiosResponse([
      { id: 10, unitId: 1, lessonNumber: 1, titleUz: 'Kirish' },
    ]))
    vi.mocked(getLessonTest).mockResolvedValue(axiosResponse({
      id: 100,
      lessonId: 10,
      titleUz: 'Test',
      timeLimitMinutes: 30,
      totalPoints: 1,
    }))
    vi.mocked(getQuestions).mockResolvedValue(axiosResponse({
      questions: [
        { id: 500, testId: 100, questionText: 'Old question', points: 1, orderNum: 1 },
      ],
      total: 1,
    }))
    vi.mocked(getQuestion).mockResolvedValue(axiosResponse({
      id: 500,
      testId: 100,
      questionText: 'Old question',
      points: 1,
      orderNum: 1,
      options: [
        { id: 1, questionId: 500, optionText: 'A', isCorrect: true, orderNum: 1 },
        { id: 2, questionId: 500, optionText: 'B', isCorrect: false, orderNum: 2 },
      ],
    }))
    vi.mocked(getLessonTheory).mockResolvedValue(axiosResponse([]))
    vi.mocked(getLessonTasks).mockResolvedValue(axiosResponse([]))
    vi.mocked(updateTest).mockResolvedValue(axiosResponse({
      id: 100,
      lessonId: 10,
      titleUz: 'Updated test',
      timeLimitMinutes: 45,
      totalPoints: 1,
    }))
    vi.mocked(createTheory).mockResolvedValue(axiosResponse({
      id: 7,
      lessonId: 10,
      titleUz: 'Yangi material',
      materialType: 'material',
      filePath: '',
      description: '',
    }))
  })

  const openLesson = async () => {
    const user = userEvent.setup()
    render(<ContentPage />)

    await user.click(await screen.findByText('F1'))
    await user.click(await screen.findByText(/1\. Kirish/))
    await screen.findByText('Test sozlamalari')
    return user
  }

  it('saves test settings through the API', async () => {
    const user = await openLesson()

    await user.click(screen.getByRole('button', { name: /Test sozlamalari/i }))
    const dialog = await screen.findByRole('dialog')
    await user.clear(within(dialog).getByLabelText('Test nomi'))
    await user.type(within(dialog).getByLabelText('Test nomi'), 'Updated test')
    await user.clear(within(dialog).getByLabelText('Vaqt chegarasi (daqiqa)'))
    await user.type(within(dialog).getByLabelText('Vaqt chegarasi (daqiqa)'), '45')
    await user.click(within(dialog).getByRole('button', { name: 'Saqlash' }))

    await waitFor(() => expect(updateTest).toHaveBeenCalledWith(100, {
      titleUz: 'Updated test',
      timeLimitMinutes: 45,
    }))
  })

  it('creates theory material with lesson id and title', async () => {
    const user = await openLesson()

    await user.click(screen.getByRole('tab', { name: 'Nazariya' }))
    await user.click(await screen.findByRole('button', { name: /Material qo'shish/i }))
    const dialog = await screen.findByRole('dialog')
    await user.type(within(dialog).getByLabelText('Nomi'), 'Yangi material')
    await user.click(within(dialog).getByRole('button', { name: 'Saqlash' }))

    await waitFor(() => expect(createTheory).toHaveBeenCalled())
    const formData = vi.mocked(createTheory).mock.calls[0][0] as FormData
    expect(formData.get('lessonId')).toBe('10')
    expect(formData.get('titleUz')).toBe('Yangi material')
  })
})
