import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter } from 'react-router-dom';
import App from './App.jsx';
import { GlossaryProvider } from './components/Glossary.jsx';
import './styles.css';

ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <BrowserRouter>
      <GlossaryProvider>
        <App />
      </GlossaryProvider>
    </BrowserRouter>
  </React.StrictMode>,
);
