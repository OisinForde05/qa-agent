import { useEffect, useState } from 'react'
import './App.css'
import RequirementForm from './RequirementForm'

type ServiceStatus = 'checking' | 'connected' | 'unavailable'
type AiServiceStatus = ServiceStatus | 'waiting' | 'cannot-check'

function isHealthResponseUp(data: unknown): data is { status: 'UP' } {
  return (
    typeof data === 'object' &&
    data !== null &&
    'status' in data &&
    data.status === 'UP'
  )
}

function App() {
  const [backendStatus, setBackendStatus] = useState<ServiceStatus>('checking')
  const [aiServiceStatus, setAiServiceStatus] =
    useState<AiServiceStatus>('waiting')

  useEffect(() => {
    const controller = new AbortController()

    async function checkServices() {
      try {
        const backendResponse = await fetch('/api/health', {
          signal: controller.signal,
        })

        if (!backendResponse.ok) {
          throw new Error('Backend health request failed')
        }

        const backendData: unknown = await backendResponse.json()

        if (!isHealthResponseUp(backendData)) {
          throw new Error('Unexpected backend health response')
        }

        if (controller.signal.aborted) return
        setBackendStatus('connected')
        setAiServiceStatus('checking')
      } catch {
        if (controller.signal.aborted) return
        setBackendStatus('unavailable')
        setAiServiceStatus('cannot-check')
        return
      }

      try {
        const aiResponse = await fetch('/api/ai/health', {
          signal: controller.signal,
        })

        if (!aiResponse.ok) {
          throw new Error('AI service health request failed')
        }

        const aiData: unknown = await aiResponse.json()

        if (!isHealthResponseUp(aiData)) {
          throw new Error('Unexpected AI service health response')
        }

        if (!controller.signal.aborted) {
          setAiServiceStatus('connected')
        }
      } catch {
        if (!controller.signal.aborted) {
          setAiServiceStatus('unavailable')
        }
      }
    }

    checkServices()

    return () => controller.abort()
  }, [])

  return (
    <main className="app">
      <h1>QA-Agent</h1>
      <p>Requirement-driven test generation and automation.</p>

      <section className="status-panel" aria-labelledby="backend-status-heading">
        <h2 id="backend-status-heading">Backend connection</h2>

        <p role="status" aria-labelledby="backend-status-heading">
          {backendStatus === 'checking' && 'Checking connection...'}
          {backendStatus === 'connected' && 'Connected'}
          {backendStatus === 'unavailable' &&
            'Unavailable. Check the backend and refresh this page.'}
        </p>

        <h2 id="ai-status-heading">AI service connection</h2>

        <p role="status" aria-labelledby="ai-status-heading">
          {aiServiceStatus === 'waiting' && 'Waiting for backend check...'}
          {aiServiceStatus === 'checking' && 'Checking connection...'}
          {aiServiceStatus === 'connected' && 'Connected'}
          {aiServiceStatus === 'unavailable' && 'Unavailable'}
          {aiServiceStatus === 'cannot-check' &&
            'Cannot check: backend unavailable'}
        </p>
        <RequirementForm />
      </section>
    </main>
  )
}

export default App