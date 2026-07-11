import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider, CssBaseline } from '@mui/material'
import { adminTheme } from './theme'
import ProtectedRoute from './components/ProtectedRoute'
import AdminLayout from './components/AdminLayout'
import LoginPage from './pages/LoginPage'
import DashboardPage from './pages/DashboardPage'
import CompanyAuditPage from './pages/CompanyAuditPage'
import JobAuditPage from './pages/JobAuditPage'
import UserManagementPage from './pages/UserManagementPage'
import AdminManagementPage from './pages/AdminManagementPage'
import AuditLogPage from './pages/AuditLogPage'
import ComplaintReviewPage from './pages/ComplaintReviewPage'
import GlobalSearchPage from './pages/GlobalSearchPage'

function App() {
  return (
    <ThemeProvider theme={adminTheme}>
      <CssBaseline />
      <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/admin/login" element={<LoginPage />} />
          <Route
            path="/admin"
            element={
              <ProtectedRoute>
                <AdminLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<DashboardPage />} />
            <Route path="companies" element={<CompanyAuditPage />} />
            <Route path="jobs" element={<JobAuditPage />} />
            <Route path="users" element={<UserManagementPage />} />
            <Route path="admins" element={<AdminManagementPage />} />
            <Route path="audit-logs" element={<AuditLogPage />} />
            <Route path="complaints" element={<ComplaintReviewPage />} />
            <Route path="search" element={<GlobalSearchPage />} />
          </Route>
          <Route path="*" element={<Navigate to="/admin" replace />} />
        </Routes>
      </BrowserRouter>
      </AuthProvider>
    </ThemeProvider>
  )
}

export default App
