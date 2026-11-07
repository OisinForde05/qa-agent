import { useEffect, useState } from 'react'
import './App.css'
import RequirementForm from './RequirementForm'
import RequirementList from './RequirementList'

type ServiceStatus = 'checking' | 'connected' | 'unavailable'

function isHealthResponseUp(data: unknown): data is { status: 'UP' } {
  return (
    typeof data === 'object' &&
    data !== null &&
    'status' in data &&
    data.status === 'UP'
  )
}

function App() {
  const [requirementsVersion, setRequirementsVersion] = useState(0)
  const [backendStatus, setBackendStatus] =
    useState<ServiceStatus>('checking')

  useEffect(() => {
    const controller = new AbortController()

    async function checkBackend() {
      try {
        const response = await fetch('/api/health', {
          signal: controller.signal,
        })

        if (!response.ok) {
          throw new Error('Backend health request failed')
        }

        const data: unknown = await response.json()

        if (!isHealthResponseUp(data)) {
          throw new Error('Unexpected backend health response')
        }

        if (!controller.signal.aborted) {
          setBackendStatus('connected')
        }
      } catch {
        if (!controller.signal.aborted) {
          setBackendStatus('unavailable')
        }
      }
    }

    checkBackend()

    return () => controller.abort()
  }, [])

    return (
    <main className="app">
      <header className="app-header">
        <div>
          <h1>QA-Agent</h1>
          <p className="app-description">
            Turn requirements into test cases you can review and approve.
          </p>
        </div>

        <div
          className={`connection-status connection-status-${backendStatus}`}
          role="status"
        >
          <span className="connection-dot" aria-hidden="true" />
          {backendStatus === 'checking' && 'Backend: checking...'}
          {backendStatus === 'connected' && 'Backend: connected'}
          {backendStatus === 'unavailable' && 'Backend: unavailable'}
        </div>
      </header>

      {backendStatus === 'unavailable' && (
        <p className="connection-notice">
          Cannot connect to the backend. Start it and refresh this page.
        </p>
      )}

      <div className="form-panel">
        <RequirementForm
          onSaved={() => setRequirementsVersion((version) => version + 1)}
        />
      </div>

      <RequirementList refreshKey={requirementsVersion} />
    </main>
  )
}

export default App
