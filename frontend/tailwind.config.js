/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        'bg-deep':    '#0d0010',
        'bg-surface': '#160020',
        'bg-border':  '#2a004a',
        'neon':       '#bf00ff',
        'magenta':    '#ff006e',
        'text-main':  '#e8d0ff',
        'text-muted': '#7a5a99',
        'success':    '#00ffaa',
        'warning':    '#ffaa00',
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', '"Fira Code"', 'Consolas', 'monospace'],
      },
    },
  },
  plugins: [],
}

