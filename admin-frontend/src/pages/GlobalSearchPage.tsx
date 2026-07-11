import React, { useCallback, useState } from 'react'
import { Alert, Avatar, Box, Button, Chip, CircularProgress, Dialog, DialogContent, DialogTitle, MenuItem, Paper, Select, Stack, Table, TableBody, TableCell, TableHead, TablePagination, TableRow, TextField, Typography } from '@mui/material'
import SearchIcon from '@mui/icons-material/Search'
import { getAdminSearchDetail, searchAdmin } from '../api/search'
import type { AdminSearchResult } from '../api/search'
import type { PageResponse } from '../types'
import { formatDate } from '../utils'

const types = [{ value: 'ALL', label: '全部' }, { value: 'USER', label: '求职者/用户' }, { value: 'COMPANY', label: '公司' }, { value: 'JOB', label: '招聘岗位' }, { value: 'RESUME', label: '简历' }]

const GlobalSearchPage: React.FC = () => {
  const [type, setType] = useState('ALL'); const [keyword, setKeyword] = useState(''); const [query, setQuery] = useState(''); const [page, setPage] = useState(0)
  const [data, setData] = useState<PageResponse<AdminSearchResult> | null>(null); const [loading, setLoading] = useState(false); const [error, setError] = useState('')
  const [detail, setDetail] = useState<Record<string, unknown> | null>(null); const [detailTitle, setDetailTitle] = useState('')
  const load = useCallback(async (nextPage = page, nextType = type, nextQuery = query) => {
    if (!nextQuery.trim()) { setData(null); setError('请输入搜索关键词'); return }
    setLoading(true); setError('')
    try { setData((await searchAdmin({ type: nextType, q: nextQuery.trim(), page: nextPage, size: 20 })).data.data) } catch { setError('搜索失败，请稍后重试') } finally { setLoading(false) }
  }, [page, type, query])
  const submit = (e: React.FormEvent) => { e.preventDefault(); setPage(0); setQuery(keyword); load(0, type, keyword) }
  const selectType = (value: string) => { setType(value); setPage(0); if (query) load(0, value, query) }
  const openDetail = async (row: AdminSearchResult) => { try { const r = await getAdminSearchDetail(row.type, row.id); setDetailTitle(`${row.title}（${row.type} #${row.id}）`); setDetail(r.data.data.data) } catch { setError('详情加载失败') } }
  return <Box>
    <Stack direction="row" justifyContent="space-between" alignItems="center" mb={3}><Typography variant="h5" fontWeight={700}>全局检索</Typography><Typography color="text.secondary">用户、公司、职位、简历</Typography></Stack>
    <Paper component="form" onSubmit={submit} sx={{ p: 2, mb: 2 }}><Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}><Select size="small" value={type} onChange={e => selectType(e.target.value)} sx={{ minWidth: 140 }}>{types.map(t => <MenuItem key={t.value} value={t.value}>{t.label}</MenuItem>)}</Select><TextField size="small" fullWidth value={keyword} onChange={e => setKeyword(e.target.value)} placeholder="用户名、公司名、职位名、学校或专业" inputProps={{ maxLength: 80 }} /><Button type="submit" variant="contained" startIcon={<SearchIcon />} disabled={loading}>搜索</Button></Stack></Paper>
    {error && <Alert severity="warning" sx={{ mb: 2 }}>{error}</Alert>}
    <Paper>{loading ? <Stack alignItems="center" p={6}><CircularProgress /></Stack> : <><Table size="small"><TableHead><TableRow><TableCell>类型</TableCell><TableCell>对象</TableCell><TableCell>摘要</TableCell><TableCell>状态</TableCell><TableCell>创建时间</TableCell><TableCell /></TableRow></TableHead><TableBody>{(data?.content || []).map(row => <TableRow hover key={`${row.type}-${row.id}`}><TableCell><Chip size="small" label={row.type} /></TableCell><TableCell><Stack direction="row" spacing={1} alignItems="center"><Avatar src={row.avatar} sx={{ width: 30, height: 30 }}>{row.title?.slice(0, 1)}</Avatar><Box><Typography variant="body2" fontWeight={600}>{row.title}</Typography><Typography variant="caption" color="text.secondary">#{row.id}</Typography></Box></Stack></TableCell><TableCell>{row.subtitle || '-'}</TableCell><TableCell>{row.status || '-'}</TableCell><TableCell>{row.createdAt ? formatDate(row.createdAt) : '-'}</TableCell><TableCell><Button size="small" onClick={() => openDetail(row)}>查看详情</Button></TableCell></TableRow>)}</TableBody></Table>{!data?.content?.length && <Typography p={5} textAlign="center" color="text.secondary">输入关键词后开始搜索</Typography>}<TablePagination component="div" count={data?.totalElements || 0} page={page} rowsPerPage={20} rowsPerPageOptions={[20]} onPageChange={(_, p) => { setPage(p); load(p) }} /></>}</Paper>
    <Dialog open={!!detail} onClose={() => setDetail(null)} fullWidth maxWidth="sm"><DialogTitle>{detailTitle}</DialogTitle><DialogContent dividers><Stack spacing={1}>{Object.entries(detail || {}).map(([key, value]) => <Stack direction="row" key={key} spacing={2}><Typography sx={{ width: 130, color: 'text.secondary' }}>{key}</Typography><Typography sx={{ whiteSpace: 'pre-wrap', wordBreak: 'break-word' }}>{String(value ?? '-')}</Typography></Stack>)}</Stack></DialogContent></Dialog>
  </Box>
}
export default GlobalSearchPage
