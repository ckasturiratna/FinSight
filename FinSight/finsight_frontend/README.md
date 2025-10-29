FinSight Frontend (Vite + React + TS)

Overview
- UI for authentication, dashboard, and managing portfolios/holdings.
- Stack: React 18, TypeScript, Vite, Tailwind, shadcn/ui, React Router, React Query.

Requirements
- Node.js 18+ (recommended 20+)
- Backend running at http://localhost:8080 (or set API base below)

Quick Start
1) Install deps:
   - cd finsight_frontend
   - npm install
2) Run dev server (defaults to http://localhost:5173):
   - npm run dev
3) Open the app:
   - http://localhost:5173

API Base URL
- Default API base: http://localhost:8080
- Override via env at runtime:
  - macOS/Linux: VITE_API_BASE=http://localhost:8081 npm run dev
  - Windows PowerShell: setx VITE_API_BASE "http://localhost:8081"; restart terminal and run npm run dev
- The base is read in src/lib/api.ts.

Authentication
- Register and Login are available; auth state persists in localStorage:
  - finsight_token (JWT)
  - finsight_user (basic decoded user info)
- Protected routes: dashboard, portfolios pages.

Portfolios UI
- Portfolios list: create, edit, delete portfolios.
- Portfolio detail: list/add/edit/delete holdings. The holding dialog now includes optional min/max price thresholds with inline validation (min ≤ max) and surfaces saved values when editing.
- Holdings require the ticker to exist in the backend companies table.

Live Valuation Enhancements
- The valuation table highlights each holding's threshold status (in range vs. breached) using the latest Finnhub quote. Threshold values and badges refresh whenever a holding is created, updated, or deleted.

Portfolio Performance History
- The **Performance History** card on the portfolio detail page calls `GET /api/portfolios/{id}/history` via `portfolioApi.getHistory`.
- History points are cached with React Query (`['portfolio-history', id]`) and rendered as a Recharts area chart showing invested vs. market value over time.
- Until the backend’s nightly snapshot job runs (or you trigger `PortfolioService.snapshotPortfolios()` manually) the card displays a “No history captured yet” placeholder.

Primary Routes
- /login – sign in
- /register – create account
- /dashboard – overview + quick actions
- /portfolios – list/manage portfolios
- /portfolios/:id – portfolio details + holdings

Commands
- npm run dev – start dev server (5173)
- npm run build – production build to dist/
- npm run preview – preview the production build locally
- npm run lint – run eslint

Styling
- Tailwind CSS with design tokens defined in src/index.css.
- shadcn/ui component primitives in src/components/ui.

Troubleshooting
- CORS error from browser:
  - Ensure backend is up and listening on 8080 (or set VITE_API_BASE).
  - Backend must allow CORS; dev config is permissive in SecurityConfig.
- “Permission denied” running vite from node_modules:
  - xattr -dr com.apple.quarantine node_modules || true
  - chmod -R u+rwX node_modules/.bin
  - rm -rf node_modules package-lock.json && npm install
- Port conflict 8080:
  - Frontend uses 5173; backend uses 8080. Change vite.config.ts if needed.

Code Pointers
- API helper: src/lib/api.ts
- Auth context: src/contexts/AuthContext.tsx
- Routes: src/App.tsx
- Pages: src/pages/* (Login, Register, Dashboard, Portfolios, PortfolioDetail)
