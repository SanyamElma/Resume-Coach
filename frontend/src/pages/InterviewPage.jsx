import { useEffect, useRef, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import toast from 'react-hot-toast';
import { interviewApi, jobApi, resumeApi } from '../services';
import { extractError } from '../services/apiClient';
import Spinner from '../components/ui/Spinner';
import ScoreRing from '../components/ui/ScoreRing';

export default function InterviewPage() {
  const [session, setSession] = useState(null);
  const [resumeId, setResumeId] = useState('');
  const [jobId, setJobId] = useState('');
  const [answer, setAnswer] = useState('');
  const [busy, setBusy] = useState(false);
  const scrollRef = useRef(null);

  const resumes = useQuery({ queryKey: ['resumes'], queryFn: () => resumeApi.list(0, 50) });
  const jobs = useQuery({ queryKey: ['jobs'], queryFn: () => jobApi.list(0, 50) });

  useEffect(() => {
    scrollRef.current?.scrollTo({ top: scrollRef.current.scrollHeight, behavior: 'smooth' });
  }, [session?.messages?.length]);

  const start = async () => {
    setBusy(true);
    try {
      const created = await interviewApi.start({ resumeId: resumeId || null, jobDescriptionId: jobId || null });
      setSession(created);
    } catch (error) {
      toast.error(extractError(error));
    } finally {
      setBusy(false);
    }
  };

  const send = async () => {
    if (!answer.trim()) return;
    setBusy(true);
    try {
      const updated = await interviewApi.sendMessage(session.id, answer.trim());
      setSession(updated);
      setAnswer('');
    } catch (error) {
      toast.error(extractError(error));
    } finally {
      setBusy(false);
    }
  };

  const finish = async () => {
    setBusy(true);
    try {
      const completed = await interviewApi.complete(session.id);
      setSession(completed);
      toast.success('Interview completed');
    } catch (error) {
      toast.error(extractError(error));
    } finally {
      setBusy(false);
    }
  };

  // --- Setup screen ---
  if (!session) {
    return (
      <div className="mx-auto max-w-xl">
        <h1 className="text-2xl font-bold text-slate-900">AI Mock Interview</h1>
        <p className="mt-1 text-sm text-slate-600">Practice with an AI interviewer that asks, evaluates, and scores.</p>
        <div className="card mt-6 space-y-4">
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
            <label className="label">Target role (optional)</label>
            <select className="input" value={jobId} onChange={(e) => setJobId(e.target.value)}>
              <option value="">Any</option>
              {(jobs.data?.content ?? []).map((j) => (
                <option key={j.id} value={j.id}>{j.title}</option>
              ))}
            </select>
          </div>
          <button className="btn-primary w-full" onClick={start} disabled={busy}>
            {busy ? 'Starting…' : 'Start interview'}
          </button>
        </div>
      </div>
    );
  }

  const isComplete = session.status === 'COMPLETED';

  return (
    <div className="mx-auto max-w-3xl">
      <div className="mb-4 flex items-center justify-between">
        <h1 className="text-2xl font-bold text-slate-900">Mock Interview</h1>
        {!isComplete && (
          <button className="btn-secondary" onClick={finish} disabled={busy}>
            End & score
          </button>
        )}
      </div>

      {isComplete && (
        <div className="card mb-4">
          <h3 className="mb-4 font-semibold text-slate-900">Your results</h3>
          <div className="flex flex-wrap justify-around gap-4">
            <ScoreRing value={session.score ?? 0} label="Overall" size={100} />
            <ScoreRing value={session.communicationScore ?? 0} label="Comms" size={100} />
            <ScoreRing value={session.technicalScore ?? 0} label="Technical" size={100} />
            <ScoreRing value={session.confidenceScore ?? 0} label="Confidence" size={100} />
          </div>
          {session.feedback && (
            <p className="mt-4 whitespace-pre-wrap rounded-lg bg-slate-50 p-4 text-sm text-slate-700">
              {session.feedback}
            </p>
          )}
        </div>
      )}

      <div ref={scrollRef} className="card max-h-[55vh] space-y-4 overflow-y-auto">
        {session.messages.map((m) => (
          <div key={m.id} className={`flex ${m.sender === 'USER' ? 'justify-end' : 'justify-start'}`}>
            <div
              className={`max-w-[80%] rounded-2xl px-4 py-2.5 text-sm ${
                m.sender === 'USER' ? 'bg-brand-600 text-white' : 'bg-slate-100 text-slate-800'
              }`}
            >
              {m.message}
              {m.answerScore != null && (
                <div className="mt-1 text-xs opacity-80">Answer score: {m.answerScore}/100</div>
              )}
            </div>
          </div>
        ))}
        {busy && <Spinner className="mx-auto" />}
      </div>

      {!isComplete && (
        <div className="mt-4 flex gap-2">
          <textarea
            rows={2}
            className="input flex-1 resize-none"
            placeholder="Type your answer…"
            value={answer}
            onChange={(e) => setAnswer(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && !e.shiftKey) {
                e.preventDefault();
                send();
              }
            }}
          />
          <button className="btn-primary" onClick={send} disabled={busy || !answer.trim()}>
            Send
          </button>
        </div>
      )}
    </div>
  );
}
