/**
 * Reads the modules discovered in src/features/<module>/routes.jsx.
 *
 * Each routes.jsx exports its route tree as default and, when the module is integrated, a `nav`:
 *
 *   export const nav = {
 *     id: 'machinery',                      // module folder name
 *     basePath: '/machinery',               // URL of the module home
 *     icon: Tractor,                        // lucide component, launcher tile and sidebar group
 *     nameKey: 'machinery:name',            // translated module name
 *     items: [{ to, labelKey, icon, end }], // sidebar links of the module, `end` for its home
 *     shell: true,                          // optional: render the module inside AppLayout
 *     sidebarFooter: LowStockNotifications, // optional: component rendered at the bottom of the
 *                                           // sidebar, above the language switcher, while the
 *                                           // module is the current one (shell modules only)
 *   }
 *
 * A module opts into the shared shell with `nav.shell = true` or `export const useAppShell = true`.
 * Until then it keeps its own layout. Modules without `nav` (sitesecurity, cropstorage, A12) are
 * routed but appear neither in the sidebar nor on the launcher.
 *
 * @param {Record<string, { default?: object[], nav?: object, useAppShell?: boolean }>} files
 *   result of import.meta.glob('../features/*\/routes.jsx', { eager: true }), or fakes in tests.
 * @returns {{ routes: object[], nav: object | null, shell: boolean }[]}
 */
export function collectModules(files) {
  return Object.keys(files)
    .sort()
    .map((path) => {
      const file = files[path]
      const nav = file.nav ?? null
      return {
        routes: file.default ?? [],
        nav,
        shell: Boolean(nav?.shell || file.useAppShell),
      }
    })
}
