import React from 'react'
import { useAuth } from '../context/AuthContext'
import { getAdminRoleLabel } from '../utils'

const AdminNavbar: React.FC = () => {
  const { user, logout } = useAuth()

  return (
    <header className="h-16 bg-white border-b border-gray-200 flex items-center justify-between px-6">
      <h1 className="text-lg font-semibold text-gray-800">微招管理后台</h1>
      {user && (
        <div className="flex items-center gap-4">
          <span className="text-sm text-gray-600">
            {user.nickname || user.username}
            <span className="ml-2 px-2 py-0.5 bg-gray-100 rounded text-xs">
              {getAdminRoleLabel(user.roleType)}
            </span>
          </span>
          <button
            onClick={logout}
            className="text-sm text-gray-500 hover:text-red-500 transition-colors"
          >
            退出登录
          </button>
        </div>
      )}
    </header>
  )
}

export default AdminNavbar
