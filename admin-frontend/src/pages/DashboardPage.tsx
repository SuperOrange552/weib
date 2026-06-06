import React, { useEffect, useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { getStats, getRecentLogs } from '../api/dashboard'
import StatCard from '../components/StatCard'
import { formatDateShort, getActionLabel } from '../utils'
import type { DashboardStats, AuditLog } from '../types'
import {
  Skeleton, Table, TableBody, TableCell, TableHead, TableRow, Chip, Paper, Typography
} from '@mui/material'
import {
  People, Work, TrendingUp, PendingActions
} from '@mui/icons-material'
import {
  LineChart, Line, PieChart, Pie, Cell, XAxis, YAxis,
  CartesianGrid, Tooltip, ResponsiveContainer
} from 'recharts'

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899']

const DashboardPage: React.FC = () => {
  const { user } = useAuth()
  const [stats, setStats] = useState<DashboardStats | null>(null)
  const [recentLogs, setRecentLogs] = useState<AuditLog[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const load = async () => {
      try {
        const [statsRes, logsRes] = await Promise.all([
          getStats(),
          getRecentLogs()
        ])
        setStats(statsRes.data.data)
        setRecentLogs(logsRes.data.data || [])
      } catch (err) {
        console.error('仪表盘数据加载失败', err)
      } finally {
        setLoading(false)
      }
    }
    load()
  }, [])

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {[1, 2, 3, 4].map(i => (
            <Skeleton key={i} variant="rounded" height={120} />
          ))}
        </div>
        <Skeleton variant="rounded" height={300} />
      </div>
    )
  }

  return (
    <div className="space-y-6">
      <Typography variant="h5" className="font-semibold">
        仪表盘
      </Typography>

      {/* 指标卡片 */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          title="总用户数"
          value={stats?.totalUsers ?? 0}
          icon={<People />}
          color="blue"
        />
        <StatCard
          title="总职位数"
          value={stats?.totalJobs ?? 0}
          icon={<Work />}
          color="green"
        />
        <StatCard
          title="今日新增"
          value={stats?.todayNewUsers ?? 0}
          icon={<TrendingUp />}
          color="orange"
        />
        <StatCard
          title="待审核"
          value={stats?.pendingCount ?? 0}
          icon={<PendingActions />}
          color="red"
        />
      </div>

      {/* 图表行 */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        <Paper className="p-4">
          <Typography variant="h6" className="mb-3 font-semibold">
            近 7 天用户增长
          </Typography>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={stats?.userGrowth || []}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip />
              <Line
                type="monotone"
                dataKey="count"
                stroke="#3B82F6"
                strokeWidth={2}
                dot={{ r: 4 }}
                name="新增用户"
              />
            </LineChart>
          </ResponsiveContainer>
        </Paper>
        <Paper className="p-4">
          <Typography variant="h6" className="mb-3 font-semibold">
            职位行业分布
          </Typography>
          <ResponsiveContainer width="100%" height={300}>
            <PieChart>
              <Pie
                data={stats?.jobDistribution || []}
                dataKey="count"
                nameKey="industry"
                cx="50%"
                cy="50%"
                outerRadius={100}
                label={({ name, percent }) =>
                  `${name} ${(percent * 100).toFixed(0)}%`
                }
              >
                {(stats?.jobDistribution || []).map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip />
            </PieChart>
          </ResponsiveContainer>
        </Paper>
      </div>

      {/* 最近审核记录 */}
      <Paper className="p-4">
        <Typography variant="h6" className="mb-3 font-semibold">
          最近审核记录
        </Typography>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>操作</TableCell>
              <TableCell>目标</TableCell>
              <TableCell>时间</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {recentLogs.slice(0, 5).map(log => (
              <TableRow key={log.id}>
                <TableCell>
                  <Chip label={getActionLabel(log.action)} size="small" />
                </TableCell>
                <TableCell className="text-gray-600">
                  {log.targetType} #{log.targetId}
                </TableCell>
                <TableCell className="text-gray-500">
                  {formatDateShort(log.createdAt)}
                </TableCell>
              </TableRow>
            ))}
            {recentLogs.length === 0 && (
              <TableRow>
                <TableCell colSpan={3} className="text-center text-gray-400">
                  暂无记录
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </Paper>
    </div>
  )
}

export default DashboardPage
