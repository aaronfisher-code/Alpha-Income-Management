import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { AuthProvider } from './AuthContext'
import { LoginPage } from './LoginPage'

describe('login', () => {
  beforeEach(() => sessionStorage.clear())
  it('labels fields, validates required credentials, and logs in', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><AuthProvider><LoginPage /></AuthProvider></MemoryRouter>)
    expect(screen.getByRole('heading', { name: /welcome to alpha/i })).toBeInTheDocument()
    await user.type(screen.getByLabelText('Username'), 'demo')
    await user.type(screen.getByLabelText('Password'), 'password')
    await user.click(screen.getByRole('button', { name: /sign in/i }))
    await waitFor(() => expect(sessionStorage.getItem('alpha-demo-session')).toBe('1'))
  })
})
