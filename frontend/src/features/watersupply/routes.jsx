import { WaterSupplyApp } from './components/WaterSupplyApp'

// Le module watersupply fournit son propre layout (sidebar + zone de contenu) et gère
// ses sous-routes en interne (vue active dérivée de l'URL, index = Tableau de bord).
// Deux branches pointent vers le même composant :
//  - index : matche exactement "/" — le splat "*" NE matche PAS la racine (react-router v7),
//    sans cette branche le tableau de bord rend une page vide ;
//  - splat : toutes les autres URLs du module (ex. /fermes, /irrigation).
// Il est monté à la racine : c'est le module d'accueil de l'application.
export default [
  {
    index: true,
    element: <WaterSupplyApp />,
  },
  {
    path: '*',
    element: <WaterSupplyApp />,
  },
]
