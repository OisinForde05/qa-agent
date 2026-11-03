import { useState } from 'react'
import type { FormEvent } from 'react'
import { isSavedTestCase } from './testCaseTypes'
import type { SavedTestCase } from './testCaseTypes'

type Props = {
  testCase: SavedTestCase
  onSaved: (testCase: SavedTestCase) => void
}

type EditableFields = Pick<
  SavedTestCase,
  'title' | 'preconditions' | 'steps' | 'expectedResult'
>

type ListEditorProps = {
  label: string
  items: string[]
  onChange: (items: string[]) => void
}

function editableFields(testCase: SavedTestCase): EditableFields {
  return {
    title: testCase.title,
    preconditions: [...testCase.preconditions],
    steps: [...testCase.steps],
    expectedResult: testCase.expectedResult,
  }
}

function ListEditor({ label, items, onChange }: ListEditorProps) {
  return (
    <fieldset className="test-case-list-editor">
      <legend>{label}</legend>

      {items.map((item, index) => (
        <div key={index}>
          <label>
            {label} {index + 1}
            <textarea
              value={item}
              rows={2}
              required
              onChange={(event) => {
                onChange(
                  items.map((value, itemIndex) =>
                    itemIndex === index ? event.target.value : value,
                  ),
                )
              }}
            />
          </label>

          <button
            type="button"
            onClick={() => onChange(items.filter((_, i) => i !== index))}
          >
            Remove
          </button>
        </div>
      ))}

      <button type="button" onClick={() => onChange([...items, ''])}>
        Add item
      </button>
    </fieldset>
  )
}

function TestCaseCard({ testCase, onSaved }: Props) {
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')
  const [form, setForm] = useState(() => editableFields(testCase))

  function startEditing() {
    setForm(editableFields(testCase))
    setError('')
    setMessage('')
    setEditing(true)
  }

  async function sendChange(
    method: 'PUT' | 'PATCH',
    path: string,
    body: unknown,
  ) {
    if (busy) return

    setBusy(true)
    setError('')
    setMessage('')

    try {
      const response = await fetch(path, {
        method,
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
      })

      const data: unknown = await response.json().catch(() => null)

      if (!response.ok) {
        const details =
          typeof data === 'object' &&
          data !== null &&
          'message' in data &&
          typeof data.message === 'string'
            ? data.message
            : `Could not save the change (HTTP ${response.status}).`

        throw new Error(details)
      }

      if (!isSavedTestCase(data) || data.id !== testCase.id) {
        throw new Error('The backend returned an unexpected test case.')
      }

      onSaved(data)
      setEditing(false)
      setMessage(
        method === 'PUT'
          ? 'Changes saved. This case requires review again.'
          : `Test case marked ${data.status.toLowerCase()}.`,
      )
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : 'Unable to save. Check the backend and try again.',
      )
    } finally {
      setBusy(false)
    }
  }

  async function saveDetails(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()

    const details = {
      title: form.title.trim(),
      preconditions: form.preconditions.map((item) => item.trim()),
      steps: form.steps.map((item) => item.trim()),
      expectedResult: form.expectedResult.trim(),
    }

    if (
      !details.title ||
      !details.expectedResult ||
      details.steps.length === 0 ||
      details.steps.some((item) => !item) ||
      details.preconditions.some((item) => !item)
    ) {
      setError(
        'Enter a title, expected result, and at least one step. Remove or complete blank list items.',
      )
      return
    }

    await sendChange('PUT', `/api/test-cases/${testCase.id}`, {
      version: testCase.version,
      ...details,
    })
  }

  async function review(status: 'APPROVED' | 'REJECTED') {
    await sendChange('PATCH', `/api/test-cases/${testCase.id}/status`, {
      version: testCase.version,
      status,
    })
  }

  return (
    <article className="draft-test-case">
      <h5>{testCase.title}</h5>

      <p>
        Status:{' '}
        <strong className={`case-status case-status-${testCase.status.toLowerCase()}`}>
          {testCase.status}
        </strong>
      </p>

      {editing ? (
        <form className="test-case-edit-form" onSubmit={saveDetails}>
          <fieldset disabled={busy}>
            <legend>Edit test case</legend>

            <label>
              Title
              <input
                value={form.title}
                required
                onChange={(event) =>
                  setForm({ ...form, title: event.target.value })
                }
              />
            </label>

            <ListEditor
              label="Preconditions"
              items={form.preconditions}
              onChange={(preconditions) =>
                setForm({ ...form, preconditions })
              }
            />

            <ListEditor
              label="Steps"
              items={form.steps}
              onChange={(steps) => setForm({ ...form, steps })}
            />

            <label>
              Expected result
              <textarea
                value={form.expectedResult}
                rows={3}
                required
                onChange={(event) =>
                  setForm({ ...form, expectedResult: event.target.value })
                }
              />
            </label>

            <p>Saving returns this case to DRAFT for another review.</p>

            <div className="test-case-actions">
              <button type="submit">
                {busy ? 'Saving...' : 'Save changes'}
              </button>

              <button
                type="button"
                onClick={() => {
                  setEditing(false)
                  setError('')
                }}
              >
                Cancel
              </button>
            </div>
          </fieldset>
        </form>
      ) : (
        <>
          <h6>Preconditions</h6>
          {testCase.preconditions.length > 0 ? (
            <ul>
              {testCase.preconditions.map((item, index) => (
                <li key={index}>{item}</li>
              ))}
            </ul>
          ) : (
            <p>None specified.</p>
          )}

          <h6>Steps</h6>
          <ol>
            {testCase.steps.map((step, index) => (
              <li key={index}>{step}</li>
            ))}
          </ol>

          <h6>Expected result</h6>
          <p>{testCase.expectedResult}</p>

          <div className="test-case-actions">
            <button type="button" disabled={busy} onClick={startEditing}>
              Edit
            </button>

            <button
              type="button"
              disabled={busy || testCase.status === 'APPROVED'}
              onClick={() => void review('APPROVED')}
            >
              Approve
            </button>

            <button
              type="button"
              disabled={busy || testCase.status === 'REJECTED'}
              onClick={() => void review('REJECTED')}
            >
              Reject
            </button>
          </div>
        </>
      )}

      {busy && <p role="status">Saving change...</p>}
      {error && <p role="alert">{error}</p>}
      {message && <p role="status">{message}</p>}
    </article>
  )
}

export default TestCaseCard
