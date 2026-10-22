/**
 * Loading State Component
 * Displays a loading spinner with optional message
 *
 * @author Abdoul Ben Fatao SANON
 * @version 1.0.0
 */

import { Loader2 } from 'lucide-react';

const LoadingState = ({ message = 'Loading...' }) => {
  return (
    <div className="flex flex-col items-center justify-center p-8">
      <Loader2 className="w-8 h-8 animate-spin text-primary mb-4" />
      <p className="text-gray-600">{message}</p>
    </div>
  );
};

export default LoadingState;
