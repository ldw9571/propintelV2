import { useEffect, useState } from 'react';
import { useLocation } from 'react-router-dom';
import AnalysisForm from '../components/AnalysisForm.jsx';
import AnalysisResult from '../components/AnalysisResult.jsx';
import { api } from '../lib/api.js';
import { DEFAULT_FORM, fromRequest, toRequest } from '../lib/analysisForm.js';

export default function AnalysisPage() {
  const location = useLocation();
  const [regions, setRegions] = useState([]);
  const [form, setForm] = useState(DEFAULT_FORM);
  const [result, setResult] = useState(null);
  const [lastRequest, setLastRequest] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [title, setTitle] = useState('');
  const [saveMsg, setSaveMsg] = useState('');

  useEffect(() => {
    api.regions().then(setRegions).catch((e) => setError(`지역 목록을 불러오지 못했습니다. 백엔드가 실행 중인지 확인하세요.\n${e.message}`));
  }, []);

  // 저장된 분석에서 "다시 열기"로 들어온 경우
  useEffect(() => {
    const saved = location.state?.savedInput;
    if (saved) {
      const f = fromRequest(saved);
      setForm(f);
      run(f);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [location.state]);

  async function run(f = form) {
    setLoading(true); setError(''); setSaveMsg('');
    try {
      const req = toRequest(f);
      const res = await api.calculate(req);
      setResult(res); setLastRequest(req);
    } catch (e) {
      setError(e.message);
    } finally {
      setLoading(false);
    }
  }

  async function save() {
    if (!lastRequest) return;
    try {
      const s = await api.save(title, null, lastRequest);
      setSaveMsg(`저장했습니다: ${s.title}`);
    } catch (e) {
      setSaveMsg(`저장 실패: ${e.message}`);
    }
  }

  return (
    <>
      <div className="page-head">
        <div>
          <h1>투자 분석</h1>
          <p>투자금과 매물 조건을 넣으면 대출 한도, 필요 현금, 1~10년 자산 변화와 수익을 계산합니다.</p>
        </div>
      </div>
      <div className="analysis">
        <div className="form-col">
          <AnalysisForm form={form} setForm={setForm} regions={regions} onSubmit={() => run()} loading={loading} />
        </div>
        <div>
          {error && <div className="notice bad error" style={{ marginBottom: 16 }}>{error}</div>}
          {!result && !error && (
            <div className="card empty">
              <h2>왼쪽에 조건을 입력하고 ‘분석하기’를 누르세요</h2>
              <p>기본값은 예시입니다. 서울 영등포구 8억 아파트를 현금 3억, 월소득 700만원으로 실거주 매수하는 경우입니다.</p>
            </div>
          )}
          {result && (
            <>
              <div className="card" style={{ marginBottom: 16, display: 'flex', gap: 10, alignItems: 'center', flexWrap: 'wrap' }}>
                <div style={{ flex: 1, minWidth: 200 }}>
                  <div style={{ fontWeight: 700 }}>
                    {result.region.sido} {result.region.sigungu || ''} {result.apartmentName ? `· ${result.apartmentName}` : ''}
                  </div>
                  <div className="small muted">규제·세율 기준일 {result.policyAsOf}</div>
                </div>
                <input style={{ maxWidth: 240 }} placeholder="저장 제목 (선택)" value={title} onChange={(e) => setTitle(e.target.value)} />
                <button className="secondary" onClick={save}>분석 저장</button>
                {saveMsg && <span className="small muted" style={{ width: '100%' }}>{saveMsg}</span>}
              </div>
              <AnalysisResult result={result} holdingYears={lastRequest.holdingYears} />
            </>
          )}
        </div>
      </div>
    </>
  );
}
