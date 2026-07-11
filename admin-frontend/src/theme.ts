import { createTheme } from '@mui/material/styles'

export const adminTheme = createTheme({
  palette: { primary: { main: '#2563eb' }, background: { default: '#f8fafc' } },
  shape: { borderRadius: 10 },
  typography: { fontFamily: 'Roboto, "Microsoft YaHei", sans-serif' },
  components: { MuiTableCell: { styleOverrides: { root: { padding: '12px 16px' } } }, MuiPaper: { styleOverrides: { root: { borderRadius: 12 } } } }
})
