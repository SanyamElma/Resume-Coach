import { useState } from 'react';
import { useMutation, useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { interviewApi, jobApi, resumeApi } from '../services';
import { extractError } from '../services/apiClient';

const categoryColor = {
  TECHNICAL: 'bg-brand-50 text-brand-700',
  BEHAVIORAL: 'bg-green-50 text-green-700',
  HR: 'bg-amber-50 text-amber-700',
  SYSTEM_DESIGN: 'bg-purple-50 text-purple-700',
};
const difficultyColor = {
  EASY: 'bg-green-100 text-green-700',
  MEDIUM: 'bg-amber-100 text-amber-700',
  HARD: 'bg-red-100 text-red-700',
};

export default function QuestionsPage() {
  const [resumeId, setResumeId] = useState('');
  const [jobId, setJobId] = useState('');
  const [questions, setQuestions] = useState([]);

  const resumes = useQuery({ queryKey: ['resumes'], queryFn: () => resumeApi.list(0, 50) });
  const jobs = useQuery({ queryKey: ['jobs'], queryFn: () => jobApi.list(0, 50) });

  const generate = useMutation({
    mutationFn: () =>
      interviewApi.generateQuestions({
        resumeId: resumeId || null,
        jobDescriptionId: jobId || null,
        count: 20,
      }),
    onSuccess: (data) => {
      setQuestions(data);
      toast.success(`Generated ${data.length} questions`);
    },
    onError: (error) => toast.error(extractError(error)),
  });

  return (
    <div>
      <h1 className="text-2xl font-bold text-slate-900">Interview question generator</h1>
      <p className="mt-1 text-sm text-slate-600">Generate 20 tailored questions from your resume and a target role.</p>

      <div className="card mt-6 grid gap-4 sm:grid-cols-3">
        <div>
          <label className="label">Resume (optional)</label>
          <select className="input" value={resumeId} onChange={(e) => setResumeId(e.target.value)}>
            <option value="">Any</option>
            {(resumes.data?.content ?? []).map((r) => (
              <option key={r.id} value={r.id}>{r.resumeName}</option>
            ))}
          </select>
        </div>
        <div>
          <label className="label">Job (optional)</label>
          <select className="input" value={jobId} onChange={(e) => setJobId(e.target.value)}>
            <option value="">Any</option>
            {(jobs.data?.content ?? []).map((j) => (
              <option key={j.id} value={j.id}>{j.title}</option>
            ))}
          </select>
        </div>
        <div className="flex items-end">
          <button className="btn-primary w-full" onClick={() => generate.mutate()} disabled={generate.isPending}>
            {generate.isPending ? 'Generating…' : 'Generate 20 questions'}
          </button>
        </div>
      </div>

      <div className="mt-6 space-y-3">
        {questions.map((q, i) => (
          <div key={i} className="card">
            <div className="mb-2 flex flex-wrap gap-2">
              <span className={`badge ${categoryColor[q.category] || 'bg-slate-100 text-slate-700'}`}>
                {q.category?.replace('_', ' ')}
              </span>
              <span className={`badge ${difficultyColor[q.difficulty] || 'bg-slate-100 text-slate-700'}`}>
                {q.difficulty}
              </span>
            </div>
            <p className="text-slate-800">
              <span className="mr-2 font-semibold text-slate-400">{i + 1}.</span>
              {q.question}
            </p>
          </div>
        ))}
      </div>
    </div>
  );
}
