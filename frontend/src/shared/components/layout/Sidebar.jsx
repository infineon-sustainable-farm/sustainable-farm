import React from 'react';
import { Link, useLocation } from 'react-router-dom';
import './Sidebar.css';

const Sidebar = () => {
  const location = useLocation();
  const currentPath = location.pathname;

  const navItems = [
    { path: '/dashboard', label: 'Dashboard', icon: 'dashboard' },
    { path: '/harvest', label: 'Harvest', icon: 'eco' },
    { path: '/raw-intake', label: 'Raw Intake', icon: 'inventory_2' },
    { path: '/batches', label: 'Batches', icon: 'inventory' },
    { path: '/washing-sorting', label: 'Washing & Sorting', icon: 'format_paint' },
    { path: '/drying', label: 'Drying', icon: 'wb_sunny' },
    { path: '/equipment', label: 'Equipment', icon: 'warehouse' },
    { path: '/operators', label: 'Operators', icon: 'people' },
  ];

  return (
    <nav className="sidebar">
      <div className="sidebar-header">
        <div className="sidebar-logo">
          <img src="/assets/sf_logo_zout-bg.png" alt="Sustainable Farm" />
        </div>
        <div className="sidebar-app-label">PRODUCT TRANSFORMATION</div>
        <div className="sidebar-divider" />
      </div>

      <ul className="sidebar-nav">
        {navItems.map((item) => (
          <li key={item.path} className={currentPath === item.path ? 'active' : ''}>
            <Link to={item.path} className="nav-item">
              <span className="material-icons nav-icon">{item.icon}</span>
              <span className="nav-label">{item.label}</span>
            </Link>
          </li>
        ))}
      </ul>

      <div className="sidebar-footer">
        <Link to="/logout" className="nav-item nav-item-footer">
          <span className="material-icons nav-icon">logout</span>
          <span className="nav-label">Logout</span>
        </Link>
      </div>
    </nav>
  );
};

export default Sidebar;