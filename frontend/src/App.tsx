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
      <h1>QA-Agent</h1>
      <p>Requirement-driven test generation and automation.</p>

      <section
        className="status-panel"
        aria-labelledby="backend-status-heading"
      >
        <h2 id="backend-status-heading">Backend connection</h2>

        <p role="status" aria-labelledby="backend-status-heading">
          {backendStatus === 'checking' && 'Checking connection...'}
          {backendStatus === 'connected' && 'Connected'}
          {backendStatus === 'unavailable' &&
            'Unavailable. Check the backend and refresh this page.'}
        </p>

        <RequirementForm
          onSaved={() => setRequirementsVersion((version) => version + 1)}
        />
      </section>
      <RequirementList refreshKey={requirementsVersion} />
    </main>
  )
}

export default App
