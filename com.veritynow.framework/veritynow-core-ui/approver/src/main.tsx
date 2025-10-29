import React from 'react;'
import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ReactQueryDevtools } from '@tanstack/react-query-devtools'
import { createRootRoute, createRoute, createRouter, RouterProvider } from '@tanstack/react-router'
import { ThemeProvider, createTheme, CssBaseline } from '@mui/material'
import App from './App'
import './styles/global.css'

const rootRoute = createRootRoute()
const indexRoute = createRoute({ getParentRoute: () => rootRoute, path: '/', component: App })
const router = createRouter({ routeTree: rootRoute.addChildren([indexRoute]) })
const qc = new QueryClient()
const theme = createTheme({ palette: { mode: 'light' } })

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={qc}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <RouterProvider router={router} />
        <ReactQueryDevtools initialIsOpen={false} />
      </ThemeProvider>
    </QueryClientProvider>
  </StrictMode>
)