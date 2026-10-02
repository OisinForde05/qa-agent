import { useEffect, useState } from 'react'
import './App.css'

type BackendStatus = 'checking' | 'connected' | 'error'

function App() {
  const [backendStatus, setBackendStatus] =
    useState<BackendStatus>('checking')

  useEffect(() => {
    const controller = new AbortController()

    async function checkBackend() {
      try {
        const response = await fetch('/api/health', {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error('Health request failed')
        }

        const data = await response.json()

        if (data?.status !== 'UP') {
          throw new Error('Unexpected health response')
        }

        if (!controller.signal.aborted) {
          setBackendStatus('connected')
        }
      } catch {
        if (!controller.signal.aborted) {
          setBackendStatus('error')
        }
      }
    }

    checkBackend()

    return () => controller.abort()
  }, [])

  return (
    <main className="app">
      <h1>QA-Agent</h1>
      <p>Requirement-driven test generation and automation.</p>

      <section className="status-panel">
        <h2>Backend connection</h2>

        <p role="status">
          {backendStatus === 'checking' && 'Checking connection...'}
          {backendStatus === 'connected' && 'Connected'}
          {backendStatus === 'error' &&
            'Unable to connect. Check the backend and refresh this page.'}
        </p>
      </section>
    </main>
  )
}

export default App