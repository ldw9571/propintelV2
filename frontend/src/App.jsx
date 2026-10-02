import { useEffect, useState } from 'react';
import { NavLink, Route, Routes, useLocation } from 'react-router-dom';
import { api } from './lib/api.js';
import HomePage from './pages/HomePage.jsx';
import AnalysisPage from './pages/AnalysisPage.jsx';
import LoanCalculatorPage from './pages/LoanCalculatorPage.jsx';
import SavedPage from './pages/SavedPage.jsx';
import MarketPage from './pages/MarketPage.jsx';
import WatchPage from './pages/WatchPage.jsx';
import AlertsPage from './pages/AlertsPage.jsx';
import NewsPage from './pages/NewsPage.jsx';
import InsightPage from './pages/InsightPage.jsx';
import PolicyPage from './pages/PolicyPage.jsx';
import GlossaryPage from './pages/GlossaryPage.jsx';
import ProfilePage from './pages/ProfilePage.jsx';

export default function App() {
  const [unread, setUnread] = useState(0);
  const location = useLocation();
  useEffect(() => { api.unread().then((r) => setUnread(r.unread)).catch(() => {}); }, [location.pathname]);

  return (
    <div className="app">
      <aside className="sidebar">
        <div className="brand">PropIntel 투자 노트</div>
        <div className="brand-sub">부동산 투자 공부용</div>
        <nav className="nav">
          <NavLink to="/" end>대시보드</NavLink>
          <NavLink to="/analysis">투자 분석</NavLink>
          <NavLink to="/loan">대출 계산기</NavLink>
          <NavLink to="/market">시장 데이터</NavLink>
          <NavLink to="/watch">관심 목록</NavLink>
          <NavLink to="/alerts">알림{unread > 0 && <span className="badge up" style={{ marginLeft: 6 }}>{unread}</span>}</NavLink>
          <NavLink to="/news">뉴스</NavLink>
          <NavLink to="/insight">원인 분석</NavLink>
          <NavLink to="/saved">저장한 분석</NavLink>
          <NavLink to="/policies">기준 데이터</NavLink>
          <NavLink to="/glossary">용어 사전</NavLink>
          <NavLink to="/profile">내 자금</NavLink>
        </nav>
      </aside>
      <main className="main">
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/analysis" element={<AnalysisPage />} />
          <Route path="/loan" element={<LoanCalculatorPage />} />
          <Route path="/market" element={<MarketPage />} />
          <Route path="/watch" element={<WatchPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/news" element={<NewsPage />} />
          <Route path="/insight" element={<InsightPage />} />
          <Route path="/saved" element={<SavedPage />} />
          <Route path="/policies" element={<PolicyPage />} />
          <Route path="/glossary" element={<GlossaryPage />} />
          <Route path="/profile" element={<ProfilePage />} />
        </Routes>
      </main>
    </div>
  );
}
