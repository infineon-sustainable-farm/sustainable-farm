/**
 * Product Transformation - Module sidebar
 *
 * Follows the module-shell standard used by Machinery, Plants and WaterSupply:
 * one teal sidebar per module, lucide icons, real routes through NavLink, and
 * an off-canvas menu below the md breakpoint.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { useState } from 'react';
import { NavLink } from 'react-router-dom';
import {
  CheckCircle,
  FileText,
  LayoutDashboard,
  Menu,
  Package,
  PackageCheck,
  Settings,
  X,
} from 'lucide-react';

const NAV_ITEMS = [
  { label: 'Dashboard', icon: LayoutDashboard, path: '/product-transformation' },
  { label: 'Batches', icon: Package, path: '/product-transformation/batches' },
  {
    label: 'Quality Control',
    icon: CheckCircle,
    path: '/product-transformation/quality-control',
  },
  { label: 'Compliance', icon: FileText, path: '/product-transformation/compliance' },
  { label: 'Packaging', icon: PackageCheck, path: '/product-transformation/packaging' },
  { label: 'Settings', icon: Settings, path: '/product-transformation/settings' },
];

// 32 px entries with 16 px icons, the one icon size of the module shell.
const ITEM_BASE =
  'flex h-8 items-center gap-2 rounded-md px-3 text-sm text-white focus-visible:outline-2 focus-visible:outline-offset-[-2px] focus-visible:outline-white';
const ICON_SIZE = 16;

const linkStyles = ({ isActive }) =>
  `${ITEM_BASE} hover:bg-black/10 ${
    isActive ? 'bg-black/10 font-semibold shadow-[inset_2px_0_0_#fff]' : 'font-normal'
  }`;

export default function ProductTransformationSidebar() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        aria-label={isOpen ? 'Close navigation' : 'Open navigation'}
        aria-expanded={isOpen}
        onClick={() => setIsOpen(!isOpen)}
        className="fixed top-4 left-4 z-50 rounded-lg bg-primary p-2 text-white md:hidden"
      >
        {isOpen ? <X /> : <Menu />}
      </button>

      <nav
        aria-label="Product Transformation module"
        className={`fixed top-0 left-0 z-40 flex h-dvh w-60 min-w-60 flex-col bg-primary px-4 py-4
          transition-transform duration-300 md:static md:translate-x-0
          ${isOpen ? 'translate-x-0' : '-translate-x-full'}`}
      >
        <div className="flex flex-col items-center gap-1 px-2">
          {/* The application's logo, served from public/ as the other module shells do. */}
          <img src="/logo.webp" alt="" width="80" height="80" />
          <p className="font-heading text-center text-xs font-bold tracking-widest text-white uppercase">
            Product Transformation
          </p>
        </div>

        <div className="my-4 h-px w-full bg-white/20" />

        <ul className="flex flex-col gap-1">
          {NAV_ITEMS.map(({ label, icon: Icon, path }) => (
            <li key={label}>
              <NavLink
                to={path}
                end={path === '/product-transformation'}
                className={linkStyles}
                onClick={() => setIsOpen(false)}
              >
                <Icon size={ICON_SIZE} aria-hidden="true" className="shrink-0" />
                {label}
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </>
  );
}
