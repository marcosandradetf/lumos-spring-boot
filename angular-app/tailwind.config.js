/** @type {import('tailwindcss').Config} */
import PrimeUI from 'tailwindcss-primeui';

module.exports = {
  content: [
    "./src/**/*.{html,ts}",
  ],
  theme: {
    fontSize: {
      xs: ['12px', { lineHeight: '16px' }],
      sm: ['13px', { lineHeight: '18px' }],
      base: ['14.5px', { lineHeight: '20px' }],
      md: ['15.5px', { lineHeight: '22px' }],
      lg: ['17px', { lineHeight: '24px' }],
      xl: ['19px', { lineHeight: '26px' }],
      '2xl': ['22px', { lineHeight: '28px' }],
      '3xl': ['26px', { lineHeight: '32px' }],
    },
    extend: {
      fontFamily: {
        sans: [
          'Inter, ui-sans-serif, system-ui, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Helvetica Neue, Arial, Noto Sans, sans-serif, Apple Color Emoji, Segoe UI Emoji, Segoe UI Symbol, Noto Color Emoji',
        ],
      },
    },
  },
  // darkMode: ['selector', '[class~="my-app-dark"]'], // custom class for dark mode
  darkMode: 'media',
  plugins: [
    require('daisyui'),
    PrimeUI,
  ],
  daisyui: {
    themes: false, // false: only light + dark | true: all themes | array: specific themes like this ["light", "dark", "cupcake"]
    darkTheme: "dark", // name of one of the included themes for dark mode
    base: true, // applies background color and foreground color for root element by default
    styled: true, // include daisyUI colors and design decisions for all components
    utils: true, // adds responsive and modifier utility classes
    prefix: "", // prefix for daisyUI classnames (components, modifiers and responsive class names. Not colors)
    logs: true, // Shows info about daisyUI version and used config in the console when building your CSS
    themeRoot: ":root", // The element that receives theme color CSS variables
  },
}
