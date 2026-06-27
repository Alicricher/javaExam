import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import type { AxiosResponse } from 'axios'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import StudentsPage from './StudentsPage'
import { getStudentFilterOptions, getStudents } from '../api/api'

vi.mock('../api/api', () => ({
  getStudents: vi.fn(),
  getStudentFilterOptions: vi.fn(),
  updateStudentName: vi.fn(),
  getStudentResults: vi.fn(),
  grantTestRetake: vi.fn(),
  grantSituationalRetake: vi.fn(),
}))

const mockedGetStudents = vi.mocked(getStudents)
const mockedGetStudentFilterOptions = vi.mocked(getStudentFilterOptions)

describe('StudentsPage', () => {
  beforeEach(() => {
    mockedGetStudents.mockReset()
    mockedGetStudentFilterOptions.mockReset()
    mockedGetStudents.mockResolvedValue({
      data: {
        students: [
          {
            id: 1,
            telegramId: 123456,
            fullName: 'Ali Valiyev',
            course: 3,
            groupName: '301',
            subgroup: 'A',
            faculty: 'Stomatologiya',
          },
        ],
        total: 1,
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {},
    } as AxiosResponse)
    mockedGetStudentFilterOptions.mockResolvedValue({
      data: {
        courses: [1, 3],
        groups: ['301'],
        subgroups: ['A'],
        faculties: ['Stomatologiya'],
      },
      status: 200,
      statusText: 'OK',
      headers: {},
      config: {},
    } as AxiosResponse)
  })

  it('renders server paginated student search filters', async () => {
    render(<StudentsPage />)

    expect(screen.getByPlaceholderText('Ism, ID yoki Telegram ID')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Guruh')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Kichik guruh')).toBeInTheDocument()
    expect(screen.getByPlaceholderText('Fakultet')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /Qidirish/i })).toBeInTheDocument()

    await waitFor(() => expect(mockedGetStudents).toHaveBeenCalledWith(expect.objectContaining({
      page: 1,
      size: 20,
    })))
  })

  it('submits filters without loading the full student list', async () => {
    const user = userEvent.setup()
    render(<StudentsPage />)

    await user.clear(screen.getByPlaceholderText('Ism, ID yoki Telegram ID'))
    await user.type(screen.getByPlaceholderText('Ism, ID yoki Telegram ID'), '123456')
    await user.type(screen.getByPlaceholderText('Guruh'), '301')
    await user.type(screen.getByPlaceholderText('Kichik guruh'), 'A')
    await user.type(screen.getByPlaceholderText('Fakultet'), 'Stomatologiya')
    await user.click(screen.getByRole('button', { name: /Qidirish/i }))

    await waitFor(() => expect(mockedGetStudents).toHaveBeenLastCalledWith(expect.objectContaining({
      page: 1,
      size: 20,
      name: '123456',
      group: '301',
      subgroup: 'A',
      faculty: 'Stomatologiya',
    })))
  })

  it('loads filter suggestions from the backend', async () => {
    render(<StudentsPage />)

    await waitFor(() => expect(mockedGetStudentFilterOptions).toHaveBeenCalled())
  })
})
