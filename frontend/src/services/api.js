import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080';

const apiClient = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  timeout: 10000,
});

apiClient.interceptors.request.use(
  (config) => config,
  (error) => Promise.reject(error)
);

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response) {
      console.error('API Error:', error.response.data);
    } else if (error.request) {
      console.error('Network Error:', error.message);
    } else {
      console.error('Request Error:', error.message);
    }
    return Promise.reject(error);
  }
);

export { apiClient };

// Re-export all service APIs for centralized access
export { dashboardApi } from './dashboardService';
export { harvestApi } from './harvestService';
export { rawIntakeApi } from './rawIntakeService';
export { batchApi } from './batchService';
export { dryingApi } from './dryingService';
export { washingSortingApi } from './washingSortingService';
export { equipmentApi } from './equipmentService';
export { operatorApi } from './operatorService';

export default apiClient;