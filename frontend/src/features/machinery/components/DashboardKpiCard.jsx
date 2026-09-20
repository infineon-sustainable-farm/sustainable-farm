import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";

const TONES = {
  primary: "bg-teal-50 text-primary",
  warning: "bg-amber-50 text-amber-600",
  error: "bg-red-50 text-red-600",
  info: "bg-sky-50 text-sky-700",
};

function DashboardKpiCard({ to, icon: Icon, label, value, hint, tone = "primary", delay = 0 }) {
  return (
    <Link
      to={to}
      className="animate-fade-up group bg-white rounded-2xl border border-gray-100 shadow-sm hover:shadow-md hover:-translate-y-0.5 transition-all p-5 flex flex-col gap-4"
      style={{ animationDelay: `${delay}ms` }}
    >
      <div className="flex items-center justify-between">
        <span className={`h-10 w-10 rounded-xl flex items-center justify-center ${TONES[tone]}`}>
          <Icon size={20} />
        </span>
        <ArrowRight size={16} className="text-gray-300 group-hover:text-primary group-hover:translate-x-0.5 transition-all" />
      </div>

      <div>
        <p className="text-3xl font-bold tracking-tight">{value}</p>
        <p className="text-sm font-medium text-gray-700 mt-0.5">{label}</p>
        <p className="text-xs text-gray-400 mt-1">{hint}</p>
      </div>
    </Link>
  );
}
export default DashboardKpiCard;
