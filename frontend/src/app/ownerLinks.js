import { ownerCreatePath } from '../shared/hooks/useCreateParam'

/**
 * Owner module of each shared resource and the URL that opens its create form. A module that only
 * picks one of these resources imports it from here and passes it to OwnedResourceSelect or
 * OwnerHint (`owner` prop): "Ask the <Module> module to add it." with a link to `createPath`.
 *
 * Decision of the resource ownership audit (2026-11-19): one owner module creates, edits and
 * deletes each resource; the others choose it in a list loaded from the owner's API.
 */
export const OWNER_LINKS = Object.freeze({
  equipment: { moduleId: 'machinery', createPath: ownerCreatePath('/machinery/equipment-registry') },
  person: { moduleId: 'visitormanagement', createPath: ownerCreatePath('/visitormanagement/staff') },
  farm: { moduleId: 'watersupply', createPath: ownerCreatePath('/watersupply/farms') },
  field: { moduleId: 'watersupply', createPath: ownerCreatePath('/watersupply/farms', 'field') },
  zone: { moduleId: 'watersupply', createPath: ownerCreatePath('/watersupply/farms', 'zone') },
  variety: { moduleId: 'plants', createPath: ownerCreatePath('/plants/harvest-maturity', 'variety') },
  harvest: { moduleId: 'plants', createPath: ownerCreatePath('/plants/harvest-maturity') },
  processingBatch: {
    moduleId: 'producttransformation',
    createPath: ownerCreatePath('/product-transformation/raw-intake'),
  },
  packagedLot: {
    moduleId: 'producttransformation',
    createPath: ownerCreatePath('/product-transformation/packaging'),
  },
})
