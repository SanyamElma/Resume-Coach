import { useQuery } from '@tanstack/react-query';
import { Link, useParams } from 'react-router-dom';
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { analysisApi } from '../services';
import QueryState from '../components/QueryState';
import ScoreRing from '../components/ui/ScoreRing';

function ListCard({ title, items, tone }) {
  const toneClass = {
    green: 'text-green-700',
    red: 'text-red-700',
    amber: 'text-amber-700',
    brand: 'text-brand-700',
  }[tone];
  return (
    <div className="card">
      <h3 className={`font-semibold ${toneClass}`}>{title}</h3>
      {items?.length ? (
        <ul className="mt-3 list-disc space-y-1.5 pl-5 text-sm text-slate-700">
          {items.map((i, idx) => (
            <li key={idx}>{i}</li>
          ))}
        </ul>
      ) : (
        <p className="mt-3 text-sm text-slate-400">None identified.</p>
      )}
    </div>
  );
}

export default function AnalysisResultPage() {
  const { id } = useParams();
  const query = useQuery({ queryKey: ['analysis', id], queryFn: () => analysisApi.get(id) });
  const r = query.data;

  const subScores = r
    ? [
        { name: 'Skills', score: r.skillMatchScore ?? 0 },
        { name: 'Experience', score: r.experienceMatchScore ?? 0 },
        { name: 'Education', score: r.educationMatchScore ?? 0 },
        { name: 'Keywords', score: r.keywordMatchScore ?? 0 },
      ]
    : [];

  return (
    <div>
      <Link to="/analysis" className="text-sm text-brand-600 hover:text-brand-700">← Back to analyses</Link>

      <QueryState query={query}>
        {r && (
          <div className="mt-4 space-y-6">
            <div className="card flex flex-col items-center gap-6 sm:flex-row sm:items-center sm:justify-between">
              <div>
                <h1 className="text-2xl font-bold text-slate-900">{r.jobTitle}</h1>
                <p className="text-sm text-slate-500">Resume: {r.resumeName}</p>
              </div>
              <ScoreRing value={r.matchPercentage} label="ATS Match" />
            </div>

            <div className="card">
              <h3 className="mb-4 font-semibold text-slate-900">Score breakdown</h3>
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={subScores}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                  <XAxis dataKey="name" fontSize={12} />
                  <YAxis domain={[0, 100]} fontSize={12} />
                  <Tooltip />
                  <Bar dataKey="score" fill="#6366f1" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>

            <div className="grid gap-6 md:grid-cols-2">
              <ListCard title="Strengths" items={r.strengths} tone="green" />
              <ListCard title="Weaknesses" items={r.weaknesses} tone="amber" />
              <ListCard title="Missing skills" items={r.missingSkills} tone="red" />
              <ListCard title="Recommendations" items={r.recommendations} tone="brand" />
            </div>
          </div>
        )}
      </QueryState>
    </div>
  );
}
