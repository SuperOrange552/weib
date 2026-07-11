import React, { useCallback, useEffect, useState } from 'react'
import {
  Alert, Button, Chip, Dialog, DialogActions, DialogContent, DialogTitle,
  FormControl, InputLabel, MenuItem, Paper, Select, Snackbar, Stack,
  Table, TableBody, TableCell, TableHead, TablePagination, TableRow,
  TextField, Typography
} from '@mui/material'
import { getComplaints, rejectComplaint, resolveComplaint } from '../api/complaints'
import type { Complaint, ComplaintReviewRequest } from '../types/complaints'
import type { PageResponse } from '../types'
import { formatDate } from '../utils'

const categoryLabel: Record<string, string> = {
  FAKE_JOB: '虚假职位', FAKE_PHOTO: '虚假照片/资料', FRAUD: '诈骗或收费',
  HARASSMENT: '骚扰', SPAM: '垃圾信息', ILLEGAL: '违法违规', OTHER: '其他'
}

const ComplaintReviewPage: React.FC = () => {
  const [data, setData] = useState<PageResponse<Complaint> | null>(null)
  const [page, setPage] = useState(0)
  const [status, setStatus] = useState('PENDING')
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState<Complaint | null>(null)
  const [dialog, setDialog] = useState<'reject' | 'resolve' | null>(null)
  const [reason, setReason] = useState('')
  const [offline, setOffline] = useState(false)
  const [sanctionType, setSanctionType] = useState('')
  const [endsAt, setEndsAt] = useState('')
  const [snackbar, setSnackbar] = useState({ open: false, msg: '', severity: 'success' as 'success' | 'error' })

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const response = await getComplaints({ page, size: 20, status: status || undefined })
      setData(response.data.data)
    } catch {
      setSnackbar({ open: true, msg: '投诉列表加载失败', severity: 'error' })
    } finally { setLoading(false) }
  }, [page, status])

  useEffect(() => { load() }, [load])

  const closeDialog = () => {
    setDialog(null); setReason(''); setOffline(false); setSanctionType(''); setEndsAt('')
  }

  const handleReject = async () => {
    if (!selected || !reason.trim()) return
    try { await rejectComplaint(selected.id, reason.trim()); setSnackbar({ open: true, msg: '投诉已驳回', severity: 'success' }); closeDialog(); load() }
    catch { setSnackbar({ open: true, msg: '处理失败', severity: 'error' }) }
  }

  const handleResolve = async () => {
    if (!selected || !reason.trim()) return
    const request: ComplaintReviewRequest = { reason: reason.trim() }
    if (offline) request.contentAction = 'OFFLINE'
    if (sanctionType) request.sanction = {
      userId: selected.targetType === 'USER' ? selected.targetId : selected.reporterId,
      sanctionType: sanctionType as 'MUTE' | 'PUBLISH_BAN' | 'ACCOUNT_BAN',
      targetType: selected.targetType, targetId: selected.targetId, sourceComplaintId: selected.id,
      reason: reason.trim(), endsAt: endsAt || undefined
    }
    try { await resolveComplaint(selected.id, request); setSnackbar({ open: true, msg: '投诉已处理', severity: 'success' }); closeDialog(); load() }
    catch { setSnackbar({ open: true, msg: '处理失败，请检查权限或参数', severity: 'error' }) }
  }

  return <div className="space-y-4">
    <Typography variant="h5" className="font-semibold">投诉审核</Typography>
    <Paper className="p-4">
      <Stack direction="row" spacing={2} className="mb-4">
        <FormControl size="small" sx={{ minWidth: 140 }}><InputLabel>状态</InputLabel><Select value={status} label="状态" onChange={e => { setStatus(e.target.value); setPage(0) }}><MenuItem value="PENDING">待审核</MenuItem><MenuItem value="RESOLVED">已处理</MenuItem><MenuItem value="REJECTED">已驳回</MenuItem><MenuItem value="">全部</MenuItem></Select></FormControl>
      </Stack>
      <Table size="small"><TableHead><TableRow><TableCell>ID</TableCell><TableCell>对象</TableCell><TableCell>类型</TableCell><TableCell>说明</TableCell><TableCell>状态</TableCell><TableCell>时间</TableCell><TableCell>操作</TableCell></TableRow></TableHead><TableBody>
        {(data?.content || []).map(item => <TableRow key={item.id} hover><TableCell>{item.id}</TableCell><TableCell>{item.targetType} #{item.targetId}</TableCell><TableCell>{categoryLabel[item.category] || item.category}</TableCell><TableCell sx={{ maxWidth: 300 }}>{item.description}</TableCell><TableCell><Chip size="small" label={item.status} color={item.status === 'PENDING' ? 'warning' : item.status === 'RESOLVED' ? 'success' : 'default'} /></TableCell><TableCell>{formatDate(item.createdAt)}</TableCell><TableCell>{item.status === 'PENDING' && <Stack direction="row" spacing={1}><Button size="small" onClick={() => { setSelected(item); setDialog('resolve') }}>处理</Button><Button size="small" color="error" onClick={() => { setSelected(item); setDialog('reject') }}>驳回</Button></Stack>}</TableCell></TableRow>)}
        {!loading && (data?.content || []).length === 0 && <TableRow><TableCell colSpan={7} align="center">暂无投诉</TableCell></TableRow>}
      </TableBody></Table>
      {data && <TablePagination component="div" count={data.totalElements} page={page} rowsPerPage={20} rowsPerPageOptions={[20]} onPageChange={(_, value) => setPage(value)} />}
    </Paper>
    <Dialog open={dialog === 'reject'} onClose={closeDialog} fullWidth maxWidth="sm"><DialogTitle>驳回投诉</DialogTitle><DialogContent><TextField autoFocus fullWidth multiline rows={3} label="处理理由" value={reason} onChange={e => setReason(e.target.value)} sx={{ mt: 1 }} /></DialogContent><DialogActions><Button onClick={closeDialog}>取消</Button><Button color="error" variant="contained" disabled={!reason.trim()} onClick={handleReject}>确认驳回</Button></DialogActions></Dialog>
    <Dialog open={dialog === 'resolve'} onClose={closeDialog} fullWidth maxWidth="sm"><DialogTitle>处理投诉 #{selected?.id}</DialogTitle><DialogContent><TextField autoFocus fullWidth multiline rows={3} label="处理理由" value={reason} onChange={e => setReason(e.target.value)} sx={{ mt: 1, mb: 2 }} /><FormControl fullWidth size="small" sx={{ mb: 2 }}><InputLabel>处罚类型</InputLabel><Select value={sanctionType} label="处罚类型" onChange={e => setSanctionType(e.target.value)}><MenuItem value="">不处罚</MenuItem><MenuItem value="MUTE">禁言</MenuItem><MenuItem value="PUBLISH_BAN">禁止发布</MenuItem><MenuItem value="ACCOUNT_BAN">封禁账号</MenuItem></Select></FormControl>{sanctionType && <TextField fullWidth size="small" type="datetime-local" label="结束时间（留空为永久）" InputLabelProps={{ shrink: true }} value={endsAt} onChange={e => setEndsAt(e.target.value)} sx={{ mb: 2 }} />}<label><input type="checkbox" checked={offline} onChange={e => setOffline(e.target.checked)} /> 同时下架投诉对象</label></DialogContent><DialogActions><Button onClick={closeDialog}>取消</Button><Button variant="contained" disabled={!reason.trim()} onClick={handleResolve}>确认处理</Button></DialogActions></Dialog>
    <Snackbar open={snackbar.open} autoHideDuration={3000} onClose={() => setSnackbar(v => ({ ...v, open: false }))}><Alert severity={snackbar.severity}>{snackbar.msg}</Alert></Snackbar>
  </div>
}

export default ComplaintReviewPage
