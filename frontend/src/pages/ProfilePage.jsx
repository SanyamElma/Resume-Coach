import { useForm } from 'react-hook-form';
import { useMutation } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { profileApi } from '../services';
import { extractError } from '../services/apiClient';
import { useAuth } from '../context/AuthContext';

export default function ProfilePage() {
  const { user, setUser } = useAuth();
  const { register, handleSubmit, formState: { errors, isDirty } } = useForm({
    defaultValues: { name: user?.name ?? '' },
  });

  const update = useMutation({
    mutationFn: (payload) => profileApi.update(payload),
    onSuccess: (updated) => {
      setUser(updated);
      toast.success('Profile updated');
    },
    onError: (error) => toast.error(extractError(error)),
  });

  return (
    <div className="mx-auto max-w-lg">
      <h1 className="text-2xl font-bold text-slate-900">Profile</h1>
      <div className="card mt-6 space-y-4">
        <form onSubmit={handleSubmit((v) => update.mutate(v))} className="space-y-4">
          <div>
            <label className="label">Full name</label>
            <input className="input" {...register('name', { required: 'Name is required' })} />
            {errors.name && <p className="mt-1 text-xs text-red-600">{errors.name.message}</p>}
          </div>
          <div>
            <label className="label">Email</label>
            <input className="input bg-slate-50" value={user?.email ?? ''} disabled />
          </div>
          <div>
            <label className="label">Role</label>
            <input className="input bg-slate-50" value={user?.role ?? ''} disabled />
          </div>
          <button type="submit" className="btn-primary w-full" disabled={!isDirty || update.isPending}>
            {update.isPending ? 'Saving…' : 'Save changes'}
          </button>
        </form>
      </div>
    </div>
  );
}
