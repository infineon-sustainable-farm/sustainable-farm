import MachineryLayout from "./components/MachineryLayout";
import EquipmentRegistry from "./components/EquipmentRegistry";

export default [
  {
    path: "machinery",
    element: <MachineryLayout />,
    children:[
      {path: "equipment-registry", element: <EquipmentRegistry/> },
    ],
  },
];
