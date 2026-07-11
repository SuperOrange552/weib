import React from 'react'
import { Outlet } from 'react-router-dom'
import AdminSidebar from './AdminSidebar'
import AdminNavbar from './AdminNavbar'
import PageContainer from './PageContainer'

const AdminLayout: React.FC = () => {
  return (
    <div className="flex h-screen bg-gray-50">
      <AdminSidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        <AdminNavbar />
        <main className="admin-main flex-1 overflow-auto p-6">
          <PageContainer><Outlet /></PageContainer>
        </main>
      </div>
    </div>
  )
}

export default AdminLayout
