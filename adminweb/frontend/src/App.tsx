import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import LoginPage from './pages/LoginPage'
import AppLayout from './components/AppLayout'
import StudentsPage from './pages/StudentsPage'
import TestResultsPage from './pages/TestResultsPage'
import SituationalPage from './pages/SituationalPage'
import ContentPage from './pages/ContentPage'
import AdminUsersPage from './pages/AdminUsersPage'

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/" element={<AppLayout />}>
          <Route index element={<Navigate to="/students" replace />} />
          <Route path="students" element={<StudentsPage />} />
          <Route path="results/tests" element={<TestResultsPage />} />
          <Route path="results/situational" element={<SituationalPage />} />
          <Route path="content" element={<ContentPage />} />
          <Route path="admin-users" element={<AdminUsersPage />} />
        </Route>
      </Routes>
    </BrowserRouter>
  )
}
