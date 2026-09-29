/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        glass: {
          light: 'rgba(255, 255, 255, 0.12)',
          dark: 'rgba(15, 23, 42, 0.65)',
          border: 'rgba(255, 255, 255, 0.18)',
          glow: 'rgba(168, 85, 247, 0.25)',
        },
      },
      backdropBlur: {
        xs: '2px',
        '2xl': '40px',
        '3xl': '64px',
      },
      animation: {
        'spin-slow': 'spin 18s linear infinite',
        'pulse-glow': 'pulseGlow 4s ease-in-out infinite',
        'wave-bar': 'waveBar 1.2s ease-in-out infinite alternate',
      },
      keyframes: {
        pulseGlow: {
          '0%, 100%': { opacity: '0.4', transform: 'scale(1)' },
          '50%': { opacity: '0.8', transform: 'scale(1.05)' },
        },
        waveBar: {
          '0%': { height: '20%' },
          '100%': { height: '95%' },
        },
      },
    },
  },
  plugins: [],
};
