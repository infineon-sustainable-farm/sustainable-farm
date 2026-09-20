import MachineryLayout from "./components/MachineryLayout";
import EquipmentRegistry from "./components/EquipmentRegistry";
import OperatorAssignments from "./components/OperatorAssignments";
import SpareParts from "./components/SpareParts";

export default [
  {
    path: "machinery",
    element: <MachineryLayout />,
    children:[
      {path: "equipment-registry", element: <EquipmentRegistry/> },
      {path: "operators", element: <OperatorAssignments/> },
      {path: "spare-parts", element: <SpareParts/> },
    ],
  },
];
