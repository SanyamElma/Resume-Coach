import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { adminApi } from '../services';
import { extractError } from '../services/apiClient';
import QueryState from '../components/QueryState';

function Metric({ label, value }) {
  return (
    <div className="card">
      <div className="text-sm text-slate-500">{label}</div>
      <div className="mt-1 text-3xl font-bold text-slate-900">{value}</div>
    </div>
  );
}

export default function AdminPage() {
  const queryClient = useQueryClient();
  const metrics = useQuery({ queryKey: ['admin-metrics'], queryFn: adminApi.metrics });
  const users = useQuery({ queryKey: ['admin-users'], queryFn: () => adminApi.users(0, 50) });

  const remove = useMutation({
    mutationFn: (id) => adminApi.deleteUser(id),
    onSuccess: () => {
      toast.success('User deleted');
      queryClient.invalidateQueries({ queryKey: ['admin-users'] });
      queryClient.invalidateQueries({ queryKey: ['admin-metrics'] });
    },
    onError: (error) => toast.error(extractError(error)),
  });

  const m = metrics.data;
  const userList = users.data?.content ?? [];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Admin dashboard</h1>
        <QueryState query={metrics}>
          {m && (
            <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
              <Metric label="Total users" value={m.totalUsers} />
              <Metric label="Total resumes" value={m.totalResumes} />
              <Metric label="Total interviews" value={m.totalInterviews} />
              <Metric label="Total analyses" value={m.totalAnalyses} />
              <Metric label="New users (7d)" value={m.newUsersLast7Days} />
              <Metric label="Interviews (24h)" value={m.interviewsLast24Hours} />
            </div>
          )}
        </QueryState>
      </div>

      <div>
        <h2 className="text-lg font-semibold text-slate-900">User management</h2>
        <div className="mt-4">
          <QueryState query={users} empty={userList.length === 0 ? 'No users.' : null}>
            <div className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
              <table className="w-full text-left text-sm">
                <thead className="bg-slate-50 text-xs uppercase text-slate-500">
                  <tr>
                    <th className="px-4 py-3">Name</th>
                    <th className="px-4 py-3">Email</th>
                    <th className="px-4 py-3">Role</th>
                    <th className="px-4 py-3">Joined</th>
                    <th className="px-4 py-3 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {userList.map((u) => (
                    <tr key={u.id} className="hover:bg-slate-50">
                      <td className="px-4 py-3 font-medium text-slate-900">{u.name}</td>
                      <td className="px-4 py-3 text-slate-600">{u.email}</td>
                      <td className="px-4 py-3">
                        <span className={`badge ${u.role === 'ADMIN' ? 'bg-purple-50 text-purple-700' : 'bg-slate-100 text-slate-700'}`}>
                          {u.role}
                        </span>
                      </td>
                      <td className="px-4 py-3 text-slate-600">{new Date(u.createdAt).toLocaleDateString()}</td>
                      <td className="px-4 py-3 text-right">
                        {u.role !== 'ADMIN' && (
                          <button
                            className="text-sm font-medium text-red-600 hover:text-red-700"
                            onClick={() => remove.mutate(u.id)}
                          >
                            Delete
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </QueryState>
        </div>
      </div>
    </div>
  );
}
