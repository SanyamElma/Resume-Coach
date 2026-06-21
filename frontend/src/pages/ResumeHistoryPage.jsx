import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import toast from 'react-hot-toast';
import { resumeApi } from '../services';
import { extractError } from '../services/apiClient';
import QueryState from '../components/QueryState';

export default function ResumeHistoryPage() {
  const queryClient = useQueryClient();
  const query = useQuery({ queryKey: ['resumes'], queryFn: () => resumeApi.list(0, 50) });

  const remove = useMutation({
    mutationFn: (id) => resumeApi.remove(id),
    onSuccess: () => {
      toast.success('Resume deleted');
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
    },
    onError: (error) => toast.error(extractError(error)),
  });

  const resumes = query.data?.content ?? [];

  return (
    <div>
      <div className="mb-6 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Resume history</h1>
        <Link to="/upload" className="btn-primary">Upload new</Link>
      </div>

      <QueryState query={query} empty={resumes.length === 0 ? 'No resumes yet. Upload your first PDF.' : null}>
        <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
          <table className="w-full text-left text-sm">
            <thead className="bg-slate-50 text-xs uppercase text-slate-500">
              <tr>
                <th className="px-4 py-3">Name</th>
                <th className="px-4 py-3">Version</th>
                <th className="px-4 py-3">Size</th>
                <th className="px-4 py-3">Uploaded</th>
                <th className="px-4 py-3 text-right">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {resumes.map((r) => (
                <tr key={r.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-900">{r.resumeName}</td>
                  <td className="px-4 py-3 text-slate-600">v{r.version}</td>
                  <td className="px-4 py-3 text-slate-600">
                    {r.sizeBytes ? `${(r.sizeBytes / 1024).toFixed(0)} KB` : '—'}
                  </td>
                  <td className="px-4 py-3 text-slate-600">{new Date(r.createdAt).toLocaleDateString()}</td>
                  <td className="px-4 py-3 text-right">
                    <button
                      className="text-sm font-medium text-red-600 hover:text-red-700"
                      onClick={() => remove.mutate(r.id)}
                    >
                      Delete
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </QueryState>
    </div>
  );
}
