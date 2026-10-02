import { won } from '../lib/format.js';

/** 단위가 붙은 숫자 입력. 만원 단위면 아래에 억/만 환산을 보여준다. */
export function NumberField({ label, value, onChange, unit, hint, step = 'any', min, max, showWon }) {
  return (
    <div className="field">
      <label>{label}</label>
      <div className="input-unit">
        <input
          type="number" inputMode="decimal" value={value ?? ''} step={step} min={min} max={max}
          onChange={(e) => onChange(e.target.value === '' ? '' : Number(e.target.value))}
        />
        {unit && <span>{unit}</span>}
      </div>
      {(showWon || hint) && (
        <div className="hint">
          {showWon && value !== '' && value !== null ? won(Number(value) * 10_000) : ''}
          {showWon && hint ? ' · ' : ''}{hint}
        </div>
      )}
    </div>
  );
}

export function Segmented({ options, value, onChange }) {
  return (
    <div className="seg" role="radiogroup">
      {options.map((o) => (
        <button type="button" key={o.value} className={value === o.value ? 'on' : ''} onClick={() => onChange(o.value)}
          role="radio" aria-checked={value === o.value}>
          {o.label}
        </button>
      ))}
    </div>
  );
}

export function Stat({ label, value, sub, tone }) {
  return (
    <div className={`stat ${tone || ''}`}>
      <div className="label">{label}</div>
      <div className="value">{value}</div>
      {sub && <div className="sub">{sub}</div>}
    </div>
  );
}
