import { useState } from "react";
import { Menu, X, LayoutDashboard, Cog, Wrench, Fuel, ClipboardList, Users, Boxes, LogOut } from "lucide-react";

const navItems = [
  { label: "Dashboard", icon: LayoutDashboard },
  { label: "Equipment Registry", icon: Cog },
  { label: "Maintenance", icon: Wrench },
  { label: "Usage & Fuel Log", icon: Fuel },
  { label: "Repair Log", icon: ClipboardList },
  { label: "Operators", icon: Users },
  { label: "Spare Parts", icon: Boxes },
];

const linkStyles =
  "flex items-center gap-3 h-10 w-full px-4 rounded-lg cursor-pointer text-white hover:bg-[color-mix(in_srgb,var(--color-primary)_85%,white_15%)]";

function Sidebar() {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <>
      <button
        type="button"
        className="md:hidden fixed top-4 left-4 z-50 text-white bg-primary p-2 rounded-lg"
        onClick={() => setIsOpen(!isOpen)}
      >
        {isOpen ? <X /> : <Menu />}
      </button>

      <nav
        className={`bg-primary w-60 h-dvh flex flex-col pt-5 px-3
          fixed md:static top-0 left-0 z-40
          transition-transform duration-300
          ${isOpen ? "translate-x-0" : "-translate-x-full"} md:translate-x-0`}
      >
        <div className="w-full flex flex-col items-center gap-2 px-2">
          <img src="/logo.webp" alt="Logo" width="150" />
          <h3 className="text-white font-bold text-center leading-tight wrap-break-word text-[clamp(0.75rem,4vw,1.125rem)]">
            MACHINERY MANAGEMENT
          </h3>
        </div>

        <div className="bg-white h-px w-4/5 my-4 mx-auto"></div>

        <ul className="w-full flex flex-col gap-2">
          {navItems.map(({ label, icon: Icon }) => (
            <li key={label}>
              <button type="button" className={linkStyles}>
                <Icon />
                {label}
              </button>
            </li>
          ))}
        </ul>

        <button type="button" className={`${linkStyles} mt-auto mb-[max(1.5rem,env(safe-area-inset-bottom))]`}>
          <LogOut />
          Logout
        </button>
      </nav>
    </>
  );
}
export default Sidebar;