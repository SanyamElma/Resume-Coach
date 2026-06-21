import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import { Link, useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { analysisApi, jobApi, resumeApi } from '../services';
import { extractError } from '../services/apiClient';
import QueryState from '../components/QueryState';

export default function AnalysisPage() {
  const navigate = useNavigate();
  const [resumeId, setResumeId] = useState('');
  const [jobId, setJobId] = useState('');

  const resumes = useQuery({ queryKey: ['resumes'], queryFn: () => resumeApi.list(0, 50) });
  const jobs = useQuery({ queryKey: ['jobs'], queryFn: () => jobApi.list(0, 50) });
  const history = useQuery({ queryKey: ['analysis-history'], queryFn: () => analysisApi.history(0, 20) });

  const run = useMutation({
    mutationFn: () => analysisApi.run(resumeId, jobId),
    onSuccess: (report) => {
      toast.success('Analysis complete');
      navigate(`/analysis/${report.id}`);
    },
    onError: (error) => toast.error(extractError(error)),
  });

  const reports = history.data?.content ?? [];

  return (
    <div className="space-y-8">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Run a skill-gap analysis</h1>
        <p className="mt-1 text-sm text-slate-600">Compare a resume against a job description to get your ATS match score.</p>

        <div className="card mt-6 grid gap-4 sm:grid-cols-2">
          <div>
            <label className="label">Resume</label>
            <select className="input" value={resumeId} onChange={(e) => setResumeId(e.target.value)}>
              <option value="">Select a resume…</option>
              {(resumes.data?.content ?? []).map((r) => (
                <option key={r.id} value={r.id}>{r.resumeName} (v{r.version})</option>
              ))}
            </select>
          </div>
          <div>
            <label className="label">Job description</label>
            <select className="input" value={jobId} onChange={(e) => setJobId(e.target.value)}>
              <option value="">Select a job…</option>
              {(jobs.data?.content ?? []).map((j) => (
                <option key={j.id} value={j.id}>{j.title}</option>
              ))}
            </select>
          </div>
          <div className="sm:col-span-2">
            <button
              className="btn-primary w-full"
              disabled={!resumeId || !jobId || run.isPending}
              onClick={() => run.mutate()}
            >
              {run.isPending ? 'Analyzing…' : 'Run analysis'}
            </button>
            {(!resumes.data?.content?.length || !jobs.data?.content?.length) && (
              <p className="mt-3 text-center text-xs text-slate-500">
                Need a <Link to="/upload" className="text-brand-600">resume</Link> and a{' '}
                <Link to="/jobs" className="text-brand-600">job description</Link> first.
              </p>
            )}
          </div>
        </div>
      </div>

      <div>
        <h2 className="text-lg font-semibold text-slate-900">Past analyses</h2>
        <div className="mt-4">
          <QueryState query={history} empty={reports.length === 0 ? 'No analyses yet.' : null}>
            <div className="space-y-3">
              {reports.map((r) => (
                <Link key={r.id} to={`/analysis/${r.id}`} className="card flex items-center justify-between hover:border-brand-300">
                  <div>
                    <div className="font-medium text-slate-900">{r.jobTitle}</div>
                    <div className="text-sm text-slate-500">{r.resumeName} · {new Date(r.createdAt).toLocaleDateString()}</div>
                  </div>
                  <div className="text-2xl font-bold text-brand-600">{r.matchPercentage}%</div>
                </Link>
              ))}
            </div>
          </QueryState>
        </div>
      </div>
    </div>
  );
}
