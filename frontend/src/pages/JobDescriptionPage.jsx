import { useForm } from 'react-hook-form';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { jobApi } from '../services';
import { extractError } from '../services/apiClient';
import QueryState from '../components/QueryState';

export default function JobDescriptionPage() {
  const queryClient = useQueryClient();
  const { register, handleSubmit, reset, formState: { errors } } = useForm();

  const query = useQuery({ queryKey: ['jobs'], queryFn: () => jobApi.list(0, 50) });

  const create = useMutation({
    mutationFn: (payload) => jobApi.create(payload),
    onSuccess: () => {
      toast.success('Job description saved & structured');
      reset();
      queryClient.invalidateQueries({ queryKey: ['jobs'] });
    },
    onError: (error) => toast.error(extractError(error)),
  });

  const jobs = query.data?.content ?? [];

  return (
    <div className="grid gap-6 lg:grid-cols-2">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Add a job description</h1>
        <p className="mt-1 text-sm text-slate-600">Paste any JD — we structure the required skills and keywords with AI.</p>
        <form onSubmit={handleSubmit((v) => create.mutate(v))} className="card mt-6 space-y-4">
          <div>
            <label className="label">Job title</label>
            <input className="input" {...register('title', { required: 'Title is required' })} />
            {errors.title && <p className="mt-1 text-xs text-red-600">{errors.title.message}</p>}
          </div>
          <div>
            <label className="label">Company</label>
            <input className="input" {...register('company')} />
          </div>
          <div>
            <label className="label">Description</label>
            <textarea
              rows={10}
              className="input"
              {...register('description', { required: 'Description is required' })}
            />
            {errors.description && <p className="mt-1 text-xs text-red-600">{errors.description.message}</p>}
          </div>
          <button type="submit" className="btn-primary w-full" disabled={create.isPending}>
            {create.isPending ? 'Saving…' : 'Save job description'}
          </button>
        </form>
      </div>

      <div>
        <h2 className="text-lg font-semibold text-slate-900">Saved job descriptions</h2>
        <div className="mt-4">
          <QueryState query={query} empty={jobs.length === 0 ? 'No job descriptions yet.' : null}>
            <div className="space-y-3">
              {jobs.map((job) => (
                <div key={job.id} className="card">
                  <div className="font-semibold text-slate-900">{job.title}</div>
                  {job.company && <div className="text-sm text-slate-500">{job.company}</div>}
                  {job.structuredData?.requiredSkills?.length > 0 && (
                    <div className="mt-3 flex flex-wrap gap-1.5">
                      {job.structuredData.requiredSkills.slice(0, 10).map((s) => (
                        <span key={s} className="badge bg-brand-50 text-brand-700">{s}</span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
            </div>
          </QueryState>
        </div>
      </div>
    </div>
  );
}
