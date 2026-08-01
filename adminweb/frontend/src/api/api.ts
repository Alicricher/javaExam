import client from './client'

// Auth
export const login = (username: string, password: string) =>
  client.post('/auth/login', { username, password })
export const logout = () => client.post('/auth/logout')
export const getMe = () => client.get('/auth/me')

// Students
export const getStudents = (params?: Record<string, unknown>) =>
  client.get('/students', { params })
export const getStudentFilterOptions = () => client.get('/students/filter-options')
export const getStudent = (id: number) => client.get(`/students/${id}`)
export const updateStudentName = (id: number, name: string) =>
  client.put(`/students/${id}/name`, { name })
export const getStudentResults = (id: number, page = 1, size = 20) =>
  client.get(`/students/${id}/results`, { params: { page, size } })
export const grantTestRetake = (id: number, testId: number) =>
  client.post(`/students/${id}/retakes/test`, { testId })
export const grantSituationalRetake = (id: number, taskId: number) =>
  client.post(`/students/${id}/retakes/situational`, { taskId })

// Units
export const getUnits = () => client.get('/units')
export const createUnit = (data: { name: string; titleUz: string }) =>
  client.post('/units', data)
export const updateUnit = (id: number, data: { name?: string; titleUz?: string }) =>
  client.put(`/units/${id}`, data)
export const deleteUnit = (id: number) => client.delete(`/units/${id}`)
export const getUnitLessons = (id: number) => client.get(`/units/${id}/lessons`)

// Lessons
export const getLesson = (id: number) => client.get(`/lessons/${id}`)
export const createLesson = (data: { unitId: number; titleUz: string }) =>
  client.post('/lessons', data)
export const updateLesson = (id: number, titleUz: string) =>
  client.put(`/lessons/${id}`, { titleUz })
export const deleteLesson = (id: number) => client.delete(`/lessons/${id}`)
export const getLessonTheory = (id: number) => client.get(`/lessons/${id}/theory`)
export const getLessonTasks = (id: number) => client.get(`/lessons/${id}/tasks`)

// Tests
export const getLessonTest = (lessonId: number) =>
  client.get(`/lessons/${lessonId}/test`)
export const createTest = (data: { lessonId: number; timeLimitMinutes?: number }) =>
  client.post('/tests', data)
export const updateTest = (id: number, data: { timeLimitMinutes?: number; titleUz?: string }) =>
  client.put(`/tests/${id}`, data)
export const deleteTest = (id: number) => client.delete(`/tests/${id}`)

// Questions
export const getQuestions = (testId: number, page = 1, size = 20) =>
  client.get(`/tests/${testId}/questions`, { params: { page, size } })
export const getQuestion = (id: number) => client.get(`/questions/${id}`)
export const createQuestion = (
  testId: number,
  data: { questionText: string; points: number; options: { optionText: string; isCorrect: boolean }[] }
) => client.post(`/tests/${testId}/questions`, data)
export const updateQuestion = (id: number, data: { questionText?: string; points?: number }) =>
  client.put(`/questions/${id}`, data)
export const deleteQuestion = (id: number) => client.delete(`/questions/${id}`)
export const moveQuestion = (id: number, direction: 'up' | 'down') =>
  client.post(`/questions/${id}/move`, { direction })
export const clearQuestions = (testId: number) =>
  client.delete(`/tests/${testId}/questions`)
export const updateOption = (questionId: number, optId: number, data: Record<string, unknown>) =>
  client.put(`/questions/${questionId}/options/${optId}`, data)
export const setCorrectOption = (questionId: number, optionId: number) =>
  client.put(`/questions/${questionId}/correct`, { optionId })
export const importQuestions = (testId: number, file: File) => {
  const form = new FormData()
  form.append('file', file)
  return client.post(`/tests/${testId}/questions/import`, form)
}
export const downloadTemplate = (testId: number) =>
  client.get(`/tests/${testId}/questions/template`, { responseType: 'blob' })

// Theory
export const createTheory = (data: FormData) => client.post('/theory', data)
export const updateTheory = (id: number, data: { titleUz?: string; materialType?: string; description?: string } | FormData) =>
  client.put(`/theory/${id}`, data)
export const deleteTheory = (id: number) => client.delete(`/theory/${id}`)
export const downloadTheoryFile = (id: number) =>
  client.get(`/theory/${id}/file`, { responseType: 'blob' })

// Tasks
export const createTask = (data: { lessonId: number; taskText: string; timeLimitMinutes?: number }) =>
  client.post('/tasks', data)
export const updateTask = (id: number, data: { taskText?: string; timeLimitMinutes?: number }) =>
  client.put(`/tasks/${id}`, data)
export const deleteTask = (id: number) => client.delete(`/tasks/${id}`)

// Results
export const getTestResults = (params?: Record<string, unknown>) =>
  client.get('/results/tests', { params })
export const getTestResultAnswers = (id: number) =>
  client.get(`/results/tests/${id}/answers`)
export const getSituationalResults = (params?: Record<string, unknown>) =>
  client.get('/results/situational', { params })
export const getSituationalAnswer = (id: number) =>
  client.get(`/results/situational/${id}`)
export const gradeAnswer = (id: number, data: { mode: 'ai' | 'manual'; grade?: number; feedback?: string }) =>
  client.post(`/results/situational/${id}/grade`, data)
