import React from 'react';
import { Outlet } from 'react-router-dom';
import Sidebar from './Sidebar';
import Header from './Header';

export default function MainLayout() {
  return (
    <div className="min-h-screen bg-background font-body-md text-on-background">
      <Sidebar />
      <div className="pl-[240px]">
        <Header />
        <main className="relative pt-20 bg-surface min-h-screen">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
