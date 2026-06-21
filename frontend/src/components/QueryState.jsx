import Spinner from './ui/Spinner';
import { extractError } from '../services/apiClient';

/** Renders loading / error / empty states for a React Query result, else its children. */
export default function QueryState({ query, empty, children }) {
  if (query.isLoading) {
    return (
      <div className="flex justify-center py-16">
        <Spinner />
      </div>
    );
  }
  if (query.isError) {
    return (
      <div className="card border-red-200 bg-red-50 text-red-700">
        {extractError(query.error)}
      </div>
    );
  }
  if (empty) {
    return <div className="card text-center text-slate-500">{empty}</div>;
  }
  return children;
}
