import React from 'react'
import { useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import {
  Dashboard as DashboardIcon,
  Business as CompanyIcon,
  Work as JobIcon,
  People as UserIcon,
  AdminPanelSettings as AdminIcon,
  History as LogIcon,
  ReportProblem as ComplaintIcon,
  Search as SearchIcon
} from '@mui/icons-material'

interface MenuItem {
  label: string
  path: string
  icon: React.ReactNode
  roles: string[]
}

const menuItems: MenuItem[] = [
  { label: '仪表盘', path: '/admin', icon: <DashboardIcon />, roles: ['super_admin', 'auditor', 'viewer'] },
  { label: '公司审核', path: '/admin/companies', icon: <CompanyIcon />, roles: ['super_admin', 'auditor'] },
  { label: '职位审核', path: '/admin/jobs', icon: <JobIcon />, roles: ['super_admin', 'auditor'] },
  { label: '用户管理', path: '/admin/users', icon: <UserIcon />, roles: ['super_admin'] },
  { label: '管理员管理', path: '/admin/admins', icon: <AdminIcon />, roles: ['super_admin'] },
  { label: '操作日志', path: '/admin/audit-logs', icon: <LogIcon />, roles: ['super_admin', 'auditor'] },
  { label: '投诉审核', path: '/admin/complaints', icon: <ComplaintIcon />, roles: ['super_admin', 'auditor'] },
  { label: '全局检索', path: '/admin/search', icon: <SearchIcon />, roles: ['super_admin', 'auditor'] }
]

const AdminSidebar: React.FC = () => {
  const navigate = useNavigate()
  const location = useLocation()
  const { user } = useAuth()

  const visibleItems = menuItems.filter(
    (item) => user && item.roles.includes(user.roleType)
  )

  return (
    <aside className="w-60 bg-white border-r border-gray-200 flex flex-col">
      <div className="h-16 flex items-center justify-center border-b border-gray-200">
        <span className="text-lg font-bold text-blue-600">微招管理后台</span>
      </div>
      <nav className="flex-1 py-4">
        {visibleItems.map((item) => (
          <button
            key={item.path}
            onClick={() => navigate(item.path)}
            className={`w-full flex items-center px-6 py-3 text-sm transition-colors ${
              location.pathname === item.path
                ? 'bg-blue-50 text-blue-600 border-r-2 border-blue-600'
                : 'text-gray-600 hover:bg-gray-50'
            }`}
          >
            <span className="mr-3">{item.icon}</span>
            {item.label}
          </button>
        ))}
      </nav>
    </aside>
  )
}

export default AdminSidebar
