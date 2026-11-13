/**
 * Product Transformation - loading state.
 *
 * A skeleton of the screen that is about to appear, plus the written message
 * the screens already pass in. The skeleton is decoration (`aria-hidden`); the
 * message is the live text, so a screen reader hears "Loading batches...".
 *
 * The shape follows the Machinery dashboard skeleton: a title block, four
 * tiles and a wide card, all on the 16 px grid.
 */

import { Loader2 } from 'lucide-react';
import { SKELETON } from '../styles/ptStyles';

const LoadingState = ({ message = 'Loading...' }) => {
  return (
    <div className="p-6" role="status" aria-live="polite">
      <div className="space-y-6" aria-hidden="true">
        <div className={`h-8 w-52 ${SKELETON}`} />
        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
          {[0, 1, 2, 3].map((index) => (
            <div key={index} className={`h-28 ${SKELETON}`} />
          ))}
        </div>
        <div className={`h-64 w-full ${SKELETON}`} />
      </div>

      <p className="mt-4 flex items-center justify-center gap-2 text-sm text-gray-600">
        <Loader2 className="h-4 w-4 animate-spin text-primary" aria-hidden="true" />
        {message}
      </p>
    </div>
  );
};

export default LoadingState;
