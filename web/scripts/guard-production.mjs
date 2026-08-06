const token = process.env.VITE_API_TOKEN?.trim()
if (token && process.env.NODE_ENV !== 'development') {
  console.error('Production build refused: VITE_API_TOKEN must never be bundled into browser assets.')
  process.exit(1)
}
