/**
 * Controlled sort of a kit Table bound to useListControls: the list sorts and paginates, the
 * table shows the arrows and aria-sort. Spread the result on <Table {...tableSort(list)} />.
 */
export function tableSort(list) {
  return {
    sort: list.sort?.key ? { key: list.sort.key, direction: list.sort.dir } : null,
    onSortChange: ({ key }) => list.toggleSort(key),
  }
}
