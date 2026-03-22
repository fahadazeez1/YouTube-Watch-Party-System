import React from 'react'
import { createRoot } from 'react-dom/client'
import App from './App.jsx'
import './index.css'

// FIX: Removed <React.StrictMode> to prevent the double-mount bug 
// which was firing the "Leave Room" cleanup function instantly on load.
createRoot(document.getElementById('root')).render(
    <App />
)