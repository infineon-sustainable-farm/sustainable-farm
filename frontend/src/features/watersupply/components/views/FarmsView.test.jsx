// @vitest-environment jsdom
// DOM environment and between-test cleanup declared in the file: the module requires
// no shared Vitest configuration (see SourcesView.test.jsx).
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, fireEvent, screen, waitFor, within } from '@testing-library/react'
import { controlByLabel, renderView as render } from './testRender'
import { FarmsView } from './FarmsView'
import { farmApi, fieldApi, zoneApi } from '../../api/watersupplyApi'

vi.mock('../../api/watersupplyApi', () => ({
  farmApi: { getFarms: vi.fn(), createFarm: vi.fn(), updateFarm: vi.fn(), deleteFarm: vi.fn() },
  fieldApi: { getFields: vi.fn(), createField: vi.fn(), deleteField: vi.fn() },
  zoneApi: { getZones: vi.fn(), createZone: vi.fn(), deleteZone: vi.fn() },
}))

const original = Object.getOwnPropertyDescriptor(globalThis.navigator, 'geolocation')
const getCurrentPosition = vi.fn()

beforeEach(() => {
  vi.clearAllMocks()
  Object.defineProperty(globalThis.navigator, 'geolocation', { value: { getCurrentPosition }, configurable: true })
  getCurrentPosition.mockImplementation((success) => success({ coords: { latitude: 10.6331234, longitude: -4.7612341, accuracy: 30 } }))
  farmApi.getFarms.mockResolvedValue([
    { id: 'farm-1', name: 'North Farm', address: 'Banfora', areaHectares: 12, description: null, latitude: 10.5, longitude: -4.5 },
  ])
  fieldApi.getFields.mockResolvedValue([
    { id: 'field-1', farmId: 'farm-1', name: 'Mango field', areaHectares: 3, coordinates: '10.6, -4.8' },
  ])
  zoneApi.getZones.mockResolvedValue([])
})

afterEach(() => {
  cleanup()
  if (original) Object.defineProperty(globalThis.navigator, 'geolocation', original)
  else delete globalThis.navigator.geolocation
})

const props = { notify: vi.fn() }

describe('FarmsView - location', () => {
  it('shows where farms and fields are, with a map link', async () => {
    render(<FarmsView {...props} />)

    const farmLink = await screen.findByText('10.5000, -4.5000')
    expect(farmLink.closest('a').getAttribute('href')).toContain('openstreetmap.org')
    expect(screen.getByText('10.6000, -4.8000')).toBeTruthy()
  })

  it('locates a new farm automatically and saves its position', async () => {
    farmApi.createFarm.mockResolvedValue({ id: 'farm-2' })
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getByText('New farm'))
    const form = document.querySelector('form')
    await waitFor(() => expect(controlByLabel(form, 'Latitude').value).toBe('10.633123'))
    expect(controlByLabel(form, 'Longitude').value).toBe('-4.761234')
    expect(screen.getByText(/accurate to about 30 m/)).toBeTruthy()

    fireEvent.change(controlByLabel(form, 'Name'), { target: { value: 'South Farm' } })
    fireEvent.submit(form)

    await waitFor(() => expect(farmApi.createFarm).toHaveBeenCalledWith(expect.objectContaining({
      name: 'South Farm', latitude: 10.633123, longitude: -4.761234,
    })))
  })

  it('keeps the position when a farm is edited (the API replaces it on every update)', async () => {
    farmApi.updateFarm.mockResolvedValue({})
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getByTitle('Edit'))
    // Editing never relocates: the farm keeps the position it was created with.
    expect(getCurrentPosition).not.toHaveBeenCalled()
    fireEvent.submit(document.querySelector('form'))

    await waitFor(() => expect(farmApi.updateFarm).toHaveBeenCalledWith('farm-1', expect.objectContaining({
      latitude: 10.5, longitude: -4.5,
    })))
  })

  it('lets the user type the position when the location is refused', async () => {
    getCurrentPosition.mockImplementation((_success, failure) => failure({ code: 1 }))
    farmApi.createFarm.mockResolvedValue({ id: 'farm-3' })
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getByText('New farm'))
    await waitFor(() => expect(screen.getByText(/Location access refused/)).toBeTruthy())
    const form = document.querySelector('form')
    fireEvent.change(controlByLabel(form, 'Name'), { target: { value: 'Hill Farm' } })
    fireEvent.change(controlByLabel(form, 'Latitude'), { target: { value: '10.7' } })
    fireEvent.submit(form)

    // Half a position is refused before reaching the API.
    await waitFor(() => expect(screen.getByText(/both latitude and longitude/)).toBeTruthy())
    expect(farmApi.createFarm).not.toHaveBeenCalled()

    fireEvent.change(controlByLabel(form, 'Longitude'), { target: { value: '-4.7' } })
    fireEvent.submit(form)
    await waitFor(() => expect(farmApi.createFarm).toHaveBeenCalledWith(expect.objectContaining({ latitude: 10.7, longitude: -4.7 })))
  })

  it('locates a new field and stores its position as "latitude, longitude"', async () => {
    fieldApi.createField.mockResolvedValue({ id: 'field-2' })
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getByText('New field'))
    const form = document.querySelector('form')
    await waitFor(() => expect(controlByLabel(form, 'Location').value).toBe('10.633123, -4.761234'))
    fireEvent.change(controlByLabel(form, 'Parent farm'), { target: { value: 'farm-1' } })
    fireEvent.change(controlByLabel(form, 'Name'), { target: { value: 'Cashew field' } })
    fireEvent.change(controlByLabel(form, 'Area'), { target: { value: '2' } })
    fireEvent.submit(form)

    await waitFor(() => expect(fieldApi.createField).toHaveBeenCalledWith(expect.objectContaining({
      farmId: 'farm-1', name: 'Cashew field', coordinates: '10.633123, -4.761234',
    })))
  })

  it('refuses a field location that is not "latitude, longitude"', async () => {
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getByText('New field'))
    const form = document.querySelector('form')
    await waitFor(() => expect(controlByLabel(form, 'Location').value).not.toBe(''))
    fireEvent.change(controlByLabel(form, 'Location'), { target: { value: 'near the river' } })
    fireEvent.submit(form)

    await waitFor(() => expect(screen.getByText(/Write the location as/)).toBeTruthy())
    expect(fieldApi.createField).not.toHaveBeenCalled()
  })
})

describe('FarmsView - dialogs', () => {
  it('opens the farm form in a dialog and closes it on Cancel without saving', async () => {
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getByText('New farm'))
    const dialog = screen.getByRole('dialog', { name: 'New farm' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(farmApi.createFarm).not.toHaveBeenCalled()
  })

  it('deletes a farm after confirmation, then reloads the lists', async () => {
    farmApi.deleteFarm.mockResolvedValue(undefined)
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getAllByTitle('Delete')[0])
    const dialog = await screen.findByRole('dialog', { name: 'Delete North Farm' })
    expect(dialog).toHaveTextContent('This action cannot be undone.')
    fireEvent.click(within(dialog).getByRole('button', { name: 'Delete' }))

    await waitFor(() => expect(farmApi.deleteFarm).toHaveBeenCalledWith('farm-1'))
    await waitFor(() => expect(farmApi.getFarms).toHaveBeenCalledTimes(2))
    expect(props.notify).toHaveBeenCalledWith('Deleted')
  })

  it('keeps the farm when the deletion is cancelled', async () => {
    render(<FarmsView {...props} />)
    await screen.findByText('North Farm')

    fireEvent.click(screen.getAllByTitle('Delete')[0])
    const dialog = await screen.findByRole('dialog', { name: 'Delete North Farm' })
    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(farmApi.deleteFarm).not.toHaveBeenCalled()
  })
})

describe('FarmsView - create links from other modules', () => {
  const location = () => screen.getByTestId('location').textContent
  const at = (query) => ({ url: `/watersupply/farms${query}` })

  it('opens the new farm dialog on ?create=1, locates it, and removes the link on Cancel', async () => {
    render(<FarmsView {...props} />, at('?create=1'))
    const dialog = await screen.findByRole('dialog', { name: 'New farm' })
    await waitFor(() => expect(controlByLabel(dialog, 'Latitude').value).toBe('10.633123'))

    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(location()).toBe('/watersupply/farms')
  })

  it('removes the link once the farm is saved', async () => {
    farmApi.createFarm.mockResolvedValue({ id: 'farm-2' })
    render(<FarmsView {...props} />, at('?create=1'))
    const dialog = await screen.findByRole('dialog', { name: 'New farm' })
    fireEvent.change(controlByLabel(dialog, 'Name'), { target: { value: 'Linked Farm' } })
    fireEvent.submit(dialog.querySelector('form'))

    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(farmApi.createFarm).toHaveBeenCalledWith(expect.objectContaining({ name: 'Linked Farm' }))
    expect(location()).toBe('/watersupply/farms')
  })

  it('opens the new field dialog on ?create=field and removes the link on Cancel', async () => {
    render(<FarmsView {...props} />, at('?create=field'))
    const dialog = await screen.findByRole('dialog', { name: 'New field' })
    expect(screen.getAllByRole('dialog')).toHaveLength(1)

    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(location()).toBe('/watersupply/farms')
  })

  it('opens the new zone dialog on ?create=zone and removes the link on Cancel', async () => {
    render(<FarmsView {...props} />, at('?create=zone'))
    const dialog = await screen.findByRole('dialog', { name: 'New zone' })
    expect(screen.getAllByRole('dialog')).toHaveLength(1)

    fireEvent.click(within(dialog).getByRole('button', { name: 'Cancel' }))
    await waitFor(() => expect(screen.queryByRole('dialog')).toBeNull())
    expect(location()).toBe('/watersupply/farms')
  })
})
