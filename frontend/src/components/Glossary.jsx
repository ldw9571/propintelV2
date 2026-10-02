import { createContext, useContext, useEffect, useState } from 'react';
import { api } from '../lib/api.js';

const GlossaryContext = createContext({});

export function GlossaryProvider({ children }) {
  const [terms, setTerms] = useState({});
  useEffect(() => {
    api.glossary()
      .then((list) => setTerms(Object.fromEntries(list.map((t) => [t.key, t]))))
      .catch(() => setTerms({}));
  }, []);
  return <GlossaryContext.Provider value={terms}>{children}</GlossaryContext.Provider>;
}

export function useGlossary() {
  return useContext(GlossaryContext);
}

/** 지표 이름 옆에 ⓘ 를 붙이고, 마우스를 올리면 용어 설명을 보여준다. */
export function Term({ k, children }) {
  const terms = useGlossary();
  const [open, setOpen] = useState(false);
  const t = terms[k];
  if (!t) return <span>{children}</span>;
  return (
    <span className="term" onMouseEnter={() => setOpen(true)} onMouseLeave={() => setOpen(false)}>
      {children}
      <i className="term-i" tabIndex={0} aria-label={`${t.term} 설명`} onFocus={() => setOpen(true)} onBlur={() => setOpen(false)}>i</i>
      {open && (
        <span className="term-pop" role="tooltip">
          <strong>{t.term}</strong>
          {t.longDesc}
          {t.example && <div className="ex">예) {t.example}</div>}
        </span>
      )}
    </span>
  );
}
