# Module Sales & Marketing

Les appels API ci-dessous utilisent le préfixe `/api`, indiqué directement
dans les routes de `api/api.js`. Le client partagé est importé depuis
`shared/api/client.js`. Les chemins sont relevés dans `routes.jsx` et les
contrôleurs du backend.

| Écran | Route | Adresses API appelées | Contrôleurs backend |
|---|---|---|---|
| Dashboard | `/salesmarketing` | `/demand-forecasts`, `/alerts`, `/product-batches`, `/products`, `/pricing-history`, `/customers`, `/orders` | `DemandForecastController`, `AlertController`, `ProductBatchController`, `ProductController`, `PricingHistoryController`, `CustomerController`, `OrderController` |
| Demand Forecasting | `/salesmarketing/forecasting` | `/demand-forecasts`, `/forecast-model-runs`, `/products`, `/orders`, `/order-items` | `DemandForecastController`, `ForecastModelRunController`, `ProductController`, `OrderController`, `OrderItemController` |
| Demand Alerts | `/salesmarketing/demand-alerts` | `/alerts`, `/demand-forecasts`, `/pricing-history` | `AlertController`, `DemandForecastController`, `PricingHistoryController` |
| Demand Reports | `/salesmarketing/reports` | `/demand-reports`, `/report-files` | `DemandReportController`, `ReportFileController` |
| Customers | `/salesmarketing/crm` | `/customers`, `/orders`, `/customer-contacts`, `/certifications`, `/customers/{customerId}/certifications` | `CustomerController`, `OrderController`, `CustomerContactController`, `CertificationController`, `CustomerCertificationController` |
| Sales Channels | `/salesmarketing/sales-channels` | `/sales-channels`, `/sales-channel-targets`, `/orders` | `SalesChannelController`, `SalesChannelTargetController`, `OrderController` |
| Pricing | `/salesmarketing/pricing` | `/pricing-history`, `/competitor-prices`, `/pricing-recommendations`, `/products` | `PricingHistoryController`, `CompetitorPriceController`, `PricingRecommendationController`, `ProductController` |
| Campaigns | `/salesmarketing/campaigns` | `/campaigns`, `/campaign-metrics` | `CampaignController`, `CampaignMetricController` |
| Delivery Tracking | `/salesmarketing/delivery` | `/shipments`, `/shipment-events` | `ShipmentController`, `ShipmentEventController` |

Les opérations de lecture utilisent les endpoints listés. Les écrans qui
créent, modifient ou suppriment des données utilisent également les
endpoints correspondants en `POST`, `PUT` ou `DELETE`. Les identifiants
individuels sont passés dans le chemin sous la forme `/{id}`.
