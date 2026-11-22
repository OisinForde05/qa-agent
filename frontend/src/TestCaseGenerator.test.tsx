import {
  act,
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from '@testing-library/react'
import '@testing-library/jest-dom/vitest'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import TestCaseGenerator from './TestCaseGenerator'

const requirementId = 8

const historyUrl =
  `/api/requirements/${requirementId}/test-generations`

const generateUrl =
  `/api/requirements/${requirementId}/test-cases/generate`

const savedGeneration = {
  id: 3,
  requirementId,
  createdAt: '2026-11-01T12:00:00Z',
  modelName: 'test-model',
  testCases: [],
  requirementIssues: ['Clarify the maximum task title length.'],
}

const newGeneration = {
  ...savedGeneration,
  id: 4,
  requirementIssues: ['Clarify whether duplicate titles are allowed.'],
}

const fetchMock = vi.fn<typeof fetch>()

function jsonResponse(data: unknown, status = 200) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      'Content-Type': 'application/json',
    },
  })
}

async function renderWithHistory() {
  render(<TestCaseGenerator requirementId={requirementId} />)

  await screen.findByText(savedGeneration.requirementIssues[0])

  const button = screen.getByRole('button', {
    name: 'Generate test cases',
  })

  await waitFor(() => expect(button).toBeEnabled())

  return button
}

function expectSavedHistory() {
  expect(screen.getByLabelText('Saved generation')).toHaveValue('3')

  expect(
    screen.getByText(savedGeneration.requirementIssues[0]),
  ).toBeInTheDocument()

  expect(screen.getAllByRole('option')).toHaveLength(1)
}

function expectReadyToGenerate() {
  expect(
    screen.getByRole('button', { name: 'Generate test cases' }),
  ).toBeEnabled()

  expect(
    screen.queryByText(
      'Generating draft test cases. This may take a couple of minutes.',
    ),
  ).not.toBeInTheDocument()
}

beforeEach(() => {
  fetchMock.mockReset()

  // Fail unexpected requests instead of making a real network call.
  fetchMock.mockImplementation(async (input, options) => {
    if (
      input === historyUrl &&
      (options?.method === undefined || options.method === 'GET')
    ) {
      return jsonResponse([savedGeneration])
    }

    throw new Error(`Unexpected request: ${String(input)}`)
  })

  vi.stubGlobal('fetch', fetchMock)
})

afterEach(() => {
  cleanup()
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('test generation failure recovery', () => {
  it.each([503, 504])(
    'shows the backend message and preserves history after HTTP %s',
    async (status) => {
      const button = await renderWithHistory()
      const message = 'Generation is unavailable. Please try again later.'

      fetchMock.mockResolvedValueOnce(
        jsonResponse({ message }, status),
      )

      fireEvent.click(button)

      expect(await screen.findByRole('alert')).toHaveTextContent(message)

      expectReadyToGenerate()
      expectSavedHistory()
    },
  )

  it('uses a fallback message when the error body is not JSON', async () => {
    const button = await renderWithHistory()

    fetchMock.mockResolvedValueOnce(
      new Response('Service unavailable', { status: 503 }),
    )

    fireEvent.click(button)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Test generation is temporarily unavailable.',
    )

    expectReadyToGenerate()
    expectSavedHistory()
  })

  it('recovers when the network request fails', async () => {
    const button = await renderWithHistory()

    fetchMock.mockRejectedValueOnce(new TypeError('Failed to fetch'))

    fireEvent.click(button)

    expect(await screen.findByRole('alert')).toHaveTextContent(
      'Unable to reach the backend. Check that it is running.',
    )

    expectReadyToGenerate()
    expectSavedHistory()
  })

  it('allows a successful attempt after a failed request', async () => {
    const button = await renderWithHistory()

    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: 'Temporarily unavailable.' }, 503),
    )

    fireEvent.click(button)
    await screen.findByRole('alert')

    expectReadyToGenerate()

    fetchMock.mockResolvedValueOnce(jsonResponse(newGeneration))

    fireEvent.click(button)

    await screen.findByText(newGeneration.requirementIssues[0])

    expect(screen.queryByRole('alert')).not.toBeInTheDocument()
    expect(screen.getByLabelText('Saved generation')).toHaveValue('4')
    expect(screen.getAllByRole('option')).toHaveLength(2)
    expectReadyToGenerate()

    // The earlier generation remains selectable after the successful retry.
    fireEvent.change(screen.getByLabelText('Saved generation'), {
      target: { value: '3' },
    })

    expect(
      screen.getByText(savedGeneration.requirementIssues[0]),
    ).toBeInTheDocument()
  })

  it('does not send duplicate requests while generation is pending', async () => {
    const button = await renderWithHistory()

    let finishRequest!: (response: Response) => void

    fetchMock.mockImplementationOnce(
      () =>
        new Promise<Response>((resolve) => {
          finishRequest = resolve
        }),
    )

    fireEvent.click(button)

    expect(button).toBeDisabled()
    expect(button).toHaveTextContent('Generating...')

    fireEvent.click(button)
    fireEvent.click(button)

    const generationRequests = fetchMock.mock.calls.filter(
      ([url, options]) =>
        url === generateUrl && options?.method === 'POST',
    )

    expect(generationRequests).toHaveLength(1)

    await act(async () => {
      finishRequest(jsonResponse(newGeneration))
    })

    expectReadyToGenerate()
  })

  it('aborts a slow request and restores the controls after three minutes', async () => {
    const button = await renderWithHistory()

    // Start fake time only after the initial history request has completed.
    vi.useFakeTimers()

    let requestSignal: AbortSignal | null | undefined

    fetchMock.mockImplementationOnce((_input, options) => {
      requestSignal = options?.signal

      return new Promise<Response>((_resolve, reject) => {
        requestSignal?.addEventListener(
          'abort',
          () => reject(new DOMException('Request aborted', 'AbortError')),
          { once: true },
        )
      })
    })

    fireEvent.click(button)

    expect(button).toBeDisabled()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(180_000)
    })

    expect(requestSignal?.aborted).toBe(true)

    expect(screen.getByRole('alert')).toHaveTextContent(
      'Generation took too long. Please try again later.',
    )

    expectReadyToGenerate()
    expectSavedHistory()
  })
})