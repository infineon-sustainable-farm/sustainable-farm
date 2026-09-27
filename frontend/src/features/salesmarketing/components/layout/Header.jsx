import React from 'react';

export default function Header() {
  return (
    <header className="fixed top-0 left-[240px] right-0 h-20 bg-surface-container-lowest/90 backdrop-blur-md z-40 flex items-center justify-between px-margin-desktop shadow-[0_1px_8px_rgba(10,130,118,0.06)]">
      <div className="flex items-center gap-lg">
        <div className="h-6 w-px bg-outline-variant"></div>
        <h1 className="font-headline-sm text-headline-sm text-primary uppercase font-bold tracking-wider">
          SALES &amp; MARKETING
        </h1>
      </div>
      <div className="flex items-center gap-xl">
        <button className="flex items-center text-on-surface-variant hover:text-primary transition-colors">
          <span className="material-symbols-outlined">search</span>
        </button>
        <button className="relative flex items-center text-on-surface-variant hover:text-primary transition-colors">
          <span className="material-symbols-outlined">notifications</span>
          <span className="absolute top-0 right-0 w-2 h-2 bg-secondary rounded-full"></span>
        </button>
        <div className="flex items-center gap-md pl-md border-l border-outline-variant">
          <div className="text-right hidden sm:block">
            <p className="font-label-md text-label-md text-on-surface font-semibold">Alex Rivera</p>
            <p className="text-[11px] text-on-surface-variant">Module Lead</p>
          </div>
          <div className="w-10 h-10 rounded-full bg-primary flex items-center justify-center shadow-md">
            <span className="material-symbols-outlined text-on-primary text-[20px]">person</span>
          </div>
        </div>
      </div>
    </header>
  );
}
