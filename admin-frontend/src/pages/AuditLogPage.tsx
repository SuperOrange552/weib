import React, { useEffect, useState, useCallback } from 'react'
import { getAuditLogs, exportAuditLogs } from '../api/auditLogs'
import { getActionLabel, formatDate } from '../utils'
import type { AuditLog, PageResponse } from '../types'
import {
  Typography, Button, Table, TableBody, TableCell, TableHead, TableRow,
  TablePagination, Paper, Chip, Snackbar, Alert, Stack, TextField
} from '@mui/material'

const AuditLogPage: React.FC = () => {
  const [data, setData] = useState<PageResponse<AuditLog> | null>(null)
  const [page, setPage] = useState(0)
  const [size] = useState(20)
  const [action, setAction] = useState('')
  const [loading, setLoading] = useState(true)
  const [searchCounter, setSearchCounter] = useState(0)
  const [snackbar, setSnackbar] = useState<{
    open: boolean
    msg: string
    severity: 'success' | 'error'
  }>({ open: false, msg: '', severity: 'success' })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAuditLogs({
        page,
        size,
        action: action || undefined
      })
      setData(res.data.data)
    } catch {
      setSnackbar({ open: true, msg: '加载失败', severity: 'error' })
    } finally {
      setLoading(false)
    }
  }, [page, size, action])

  useEffect(() => { load() }, [page, searchCounter])

  const handleSearch = () => {
    if (page === 0) { setSearchCounter(p => p + 1) }
    else { setPage(0) }
  }

  const handleExport = async () => {
    try {
      const res = await exportAuditLogs({})
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const a = document.createElement('a')
      a.href = url
      a.download = 'audit_logs.csv'
      a.click()
      window.URL.revokeObjectURL(url)
      setSnackbar({ open: true, msg: '导出成功', severity: 'success' })
    } catch {
      setSnackbar({ open: true, msg: '导出失败', severity: 'error' })
    }
  }

  return (
    <div className="space-y-4">
      <Typography variant="h5" className="font-semibold">操作日志</Typography>
      <Paper className="p-4">
        <Stack direction="row" spacing={2} className="mb-4" alignItems="center">
          <TextField
            size="small"
            label="操作类型"
            value={action}
            onChange={e => setAction(e.target.value)}
            onKeyDown={e => { if (e.key === 'Enter') handleSearch() }}
          />
          <Button variant="outlined" onClick={handleSearch}>搜索</Button>
          <div className="flex-1" />
          <Button variant="outlined" onClick={handleExport}>导出 CSV</Button>
        </Stack>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>操作人</TableCell>
              <TableCell>操作类型</TableCell>
              <TableCell>目标</TableCell>
              <TableCell>理由</TableCell>
              <TableCell>时间</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {(data?.content || []).map(log => (
              <TableRow key={log.id}>
                <TableCell className="font-medium">
                  {log.adminName || '-'}
                </TableCell>
                <TableCell>
                  <Chip label={getActionLabel(log.action)} size="small" />
                </TableCell>
                <TableCell className="text-gray-600">
                  {log.targetType} #{log.targetId}
                </TableCell>
                <TableCell className="text-gray-500 text-sm max-w-xs truncate">
                  {log.reason || '-'}
                </TableCell>
                <TableCell className="text-gray-500 text-sm">
                  {formatDate(log.createdAt)}
                </TableCell>
              </TableRow>
            ))}
            {(!data || data.content.length === 0) && !loading && (
              <TableRow>
                <TableCell colSpan={5} className="text-center text-gray-400 py-8">
                  暂无数据
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        {data && (
          <TablePagination
            component="div"
            count={data.totalElements}
            page={page}
            rowsPerPage={size}
            onPageChange={(_, p) => setPage(p)}
            rowsPerPageOptions={[size]}
            labelRowsPerPage=""
          />
        )}
      </Paper>
      <Snackbar
        open={snackbar.open}
        autoHideDuration={3000}
        onClose={() => setSnackbar(prev => ({ ...prev, open: false }))}
      >
        <Alert severity={snackbar.severity}>{snackbar.msg}</Alert>
      </Snackbar>
    </div>
  )
}

export default AuditLogPage
