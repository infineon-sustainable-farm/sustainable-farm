import { BrowserRouter } from 'react-router-dom'
import { WaterSupplyApp } from './features/watersupply/components/WaterSupplyApp'

/**
 * Point d'entrée du frontend.
 * Aucune authentification n'est requise — l'utilisateur arrive directement sur l'application.
 * BrowserRouter fournit une URL directe pour chaque vue (ex. /irrigation, /consommation).
 * En production, nginx redirige toute route vers index.html (try_files).
 */
function App() {
  return (
    <BrowserRouter>
      <WaterSupplyApp />
    </BrowserRouter>
  )
}

export default App