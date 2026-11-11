/**
 * Product Transformation - Module shell
 *
 * Same structure as MachineryLayout / PlantsLayout / WaterSupplyLayout:
 * the module's sidebar on the left, the active screen on the right through
 * <Outlet />. There is exactly one sidebar per module.
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { Outlet } from 'react-router-dom';
import ProductTransformationSidebar from './ProductTransformationSidebar';

export default function ProductTransformationLayout() {
  return (
    <div className="flex h-dvh">
      <ProductTransformationSidebar />
      <main className="h-dvh min-w-0 flex-1 overflow-y-auto pt-16 md:pt-0">
        <Outlet />
      </main>
    </div>
  );
}
