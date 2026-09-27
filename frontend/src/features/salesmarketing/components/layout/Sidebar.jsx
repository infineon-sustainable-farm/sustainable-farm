import React, { useState } from 'react';
import { NavLink, useNavigate } from 'react-router-dom';

const navItems = [
  { path: '/dashboard', label: 'DASHBOARD', icon: 'grid_view' },
  { path: '/forecasting', label: 'DEMAND FORECASTING', icon: 'trending_up' },
  { path: '/demand-alerts', label: 'DEMAND ALERTS', icon: 'notifications' },
  { path: '/reports', label: 'DEMAND REPORTS', icon: 'description' },
  { path: '/crm', label: 'CUSTOMERS', icon: 'group' },
  { path: '/sales-channels', label: 'SALES CHANNELS', icon: 'shopping_cart' },
  { path: '/pricing', label: 'PRICING', icon: 'sell' },
  { path: '/campaigns', label: 'CAMPAIGNS', icon: 'campaign' },
  { path: '/delivery', label: 'DELIVERY TRACKING', icon: 'local_shipping' },
];

export default function Sidebar() {
  const navigate = useNavigate();
  const [showLogoutConfirm, setShowLogoutConfirm] = useState(false);
  const logoUrl = "https://lh3.googleusercontent.com/aida-public/AB6AXuAjnqC009D8rGKDRRE5N_xt335K2BdnjyR0gIkgdeN5m4Y4FyuCSj7Yyln8XEOp4CXlwSMXFKEZhmkmMEehheDjC0RD2hR1Vz-3Uhajay7O2eaDm6xNdiVinyR6xOgJ2InGmhH_1wDbPSOowxZhMuNPev14lf0gq_k7__qJhD85yOA2Id2YbxYRe2D0XR04GSaWn4M0SekDZ7aUreC124Q8W-GwUBBqE-7WJR1-NQp8LjV_DP6zrNGKNR36nK4fDxIDBw";

  const handleLogout = () => {
    window.localStorage.clear();
    window.sessionStorage.clear();
    setShowLogoutConfirm(false);
    navigate('/dashboard', { replace: true });
  };

  return (
    <aside className="fixed left-0 top-0 h-full w-[240px] bg-[#0a8276] text-white z-50 flex flex-col py-xl shadow-xl">
      {/* Logo & Title Header */}
      <div className="flex flex-col items-center pt-4 pb-6 px-lg shrink-0">
        <div className="w-[30px] h-[30px] flex items-center justify-center mb-4">
          <div className="w-[30px] h-[30px] bg-white rounded-full flex items-center justify-center overflow-hidden shadow-sm p-0">
            <img 
              src={logoUrl} 
              alt="Logo" 
              className="w-[30px] h-[30px] object-contain" 
              style={{ transform: 'scale(2.5)' }} 
            />
          </div>
        </div>
        <h2 className="font-headline-sm text-[15px] text-white font-bold tracking-wide uppercase text-center w-full border-b border-white/20 pb-4">
          SALES &amp; MARKETING
        </h2>
      </div>

      {/* Navigation Items */}
      <nav className="flex-1 min-h-0 overflow-y-auto px-md flex flex-col gap-xs">
        {navItems.map((item) => (
          <NavLink
            key={item.path}
            to={item.path}
            className={({ isActive }) =>
              `flex items-center px-lg py-md rounded-full transition-all duration-200 group ${
                isActive
                  ? 'bg-primary-fixed-dim text-on-primary-fixed font-bold'
                  : 'opacity-80 hover:opacity-100 hover:bg-white/10 text-white'
              }`
            }
          >
            <span className="material-symbols-outlined mr-md text-[20px]">{item.icon}</span>
            <span className="font-label-md text-label-md uppercase tracking-wider">{item.label}</span>
          </NavLink>
        ))}
      </nav>

      {/* Footer / Logout */}
      <div className="px-md pt-lg shrink-0 border-t border-white/10 mt-2">
        <button
          type="button"
          onClick={() => setShowLogoutConfirm(true)}
          className="flex items-center px-lg py-md text-white opacity-80 hover:opacity-100 hover:bg-error/20 rounded-full transition-all group"
        >
          <span className="material-symbols-outlined mr-md text-[20px]">logout</span>
          <span className="font-label-md text-label-md uppercase tracking-wider">LOGOUT</span>
        </button>
      </div>

      {showLogoutConfirm && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-md bg-on-surface/40 backdrop-blur-sm">
          <div className="bg-surface-container-lowest text-on-surface rounded-xl shadow-xl w-full max-w-sm p-xl">
            <h2 className="font-headline-sm text-headline-sm mb-sm">Log out?</h2>
            <p className="font-body-sm text-body-sm text-on-surface-variant mb-lg">
              This clears the current browser session and returns to the dashboard.
            </p>
            <div className="flex gap-md">
              <button
                type="button"
                onClick={() => setShowLogoutConfirm(false)}
                className="flex-1 h-11 rounded-xl bg-surface-container font-label-md text-label-md hover:bg-surface-container-high transition-colors"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleLogout}
                className="flex-1 h-11 rounded-xl bg-error text-on-error font-label-md text-label-md hover:opacity-90 transition-opacity"
              >
                Log out
              </button>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}
