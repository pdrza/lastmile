/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,jsx}",
  ],
  theme: {
    extend: {
      colors: {
        'bg-deep':    '#f3f2f0',
        'bg-surface': '#ffffff',
        'bg-border':  '#e6e4e0',
        'brand':      '#1dd1a1',
        'brand-dark': '#15b98c',
        'brand-soft': '#ecf8ef',
        'accent':     '#64b5f6',
        'text-main':  '#141414',
        'text-muted': '#6b7280',
        'success':    '#1dd1a1',
        'warning':    '#c89639',
        'danger':     '#de4f41',
      },
      fontFamily: {
        sans: ['Nunito', 'system-ui', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'Consolas', 'monospace'],
      },
      boxShadow: {
        soft:      '0 1px 3px rgba(20,20,20,0.06), 0 4px 12px rgba(20,20,20,0.05)',
        'soft-lg': '0 4px 20px rgba(20,20,20,0.10)',
      },
    },
  },
  plugins: [],
}
