import { useQuery } from '@tanstack/react-query';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import { Link } from 'react-router-dom';
import { dashboardApi } from '../services';
import QueryState from '../components/QueryState';
import { useAuth } from '../context/AuthContext';

const COLORS = ['#6366f1', '#22c55e', '#f59e0b', '#ef4444', '#06b6d4', '#a855f7', '#ec4899', '#84cc16'];

function StatCard({ label, value }) {
  return (
    <div className="card">
      <div className="text-sm text-slate-500">{label}</div>
      <div className="mt-1 text-3xl font-bold text-slate-900">{value}</div>
    </div>
  );
}

const formatDate = (iso) => new Date(iso).toLocaleDateString(undefined, { month: 'short', day: 'numeric' });

export default function DashboardPage() {
  const { user } = useAuth();
  const query = useQuery({ queryKey: ['dashboard'], queryFn: dashboardApi.get });
  const data = query.data;

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-bold text-slate-900">Welcome, {user?.name}</h1>
          <p className="text-sm text-slate-600">Your resume and interview performance at a glance.</p>
        </div>
        <div className="flex gap-2">
          <Link to="/upload" className="btn-primary">Upload Resume</Link>
          <Link to="/analysis" className="btn-secondary">Run Analysis</Link>
        </div>
      </div>

      <QueryState query={query}>
        {data && (
          <div className="space-y-6">
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
              <StatCard label="Resumes" value={data.totalResumes} />
              <StatCard label="Analyses" value={data.totalAnalyses} />
              <StatCard label="Interviews" value={data.totalInterviews} />
              <StatCard label="Latest match" value={data.latestMatchScore != null ? `${data.latestMatchScore}%` : '—'} />
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
              <ChartCard title="Resume Match Score Trend">
                {data.resumeScoreTrend.length ? (
                  <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={data.resumeScoreTrend.map((p) => ({ ...p, date: formatDate(p.date) }))}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                      <XAxis dataKey="date" fontSize={12} />
                      <YAxis domain={[0, 100]} fontSize={12} />
                      <Tooltip />
                      <Line type="monotone" dataKey="score" stroke="#6366f1" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <Empty>Run an analysis to see your trend.</Empty>
                )}
              </ChartCard>

              <ChartCard title="Interview Score Trend">
                {data.interviewScoreTrend.length ? (
                  <ResponsiveContainer width="100%" height={260}>
                    <LineChart data={data.interviewScoreTrend.map((p) => ({ ...p, date: formatDate(p.date) }))}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                      <XAxis dataKey="date" fontSize={12} />
                      <YAxis domain={[0, 100]} fontSize={12} />
                      <Tooltip />
                      <Line type="monotone" dataKey="score" stroke="#22c55e" strokeWidth={2} />
                    </LineChart>
                  </ResponsiveContainer>
                ) : (
                  <Empty>Complete a mock interview to see your trend.</Empty>
                )}
              </ChartCard>

              <ChartCard title="Skill Distribution">
                {data.skillDistribution.length ? (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={data.skillDistribution}>
                      <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                      <XAxis dataKey="name" fontSize={11} interval={0} angle={-20} textAnchor="end" height={60} />
                      <YAxis allowDecimals={false} fontSize={12} />
                      <Tooltip />
                      <Bar dataKey="count" radius={[4, 4, 0, 0]}>
                        {data.skillDistribution.map((_, i) => (
                          <Cell key={i} fill={COLORS[i % COLORS.length]} />
                        ))}
                      </Bar>
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <Empty>No skill data yet.</Empty>
                )}
              </ChartCard>

              <ChartCard title="Most Frequent Missing Skills">
                {data.missingSkillsFrequency.length ? (
                  <ResponsiveContainer width="100%" height={260}>
                    <BarChart data={data.missingSkillsFrequency} layout="vertical">
                      <CartesianGrid strokeDasharray="3 3" stroke="#eef2f7" />
                      <XAxis type="number" allowDecimals={false} fontSize={12} />
                      <YAxis type="category" dataKey="name" width={120} fontSize={11} />
                      <Tooltip />
                      <Bar dataKey="count" fill="#ef4444" radius={[0, 4, 4, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                ) : (
                  <Empty>No missing-skill data yet.</Empty>
                )}
              </ChartCard>
            </div>
          </div>
        )}
      </QueryState>
    </div>
  );
}

function ChartCard({ title, children }) {
  return (
    <div className="card">
      <h3 className="mb-4 font-semibold text-slate-900">{title}</h3>
      {children}
    </div>
  );
}

function Empty({ children }) {
  return <div className="flex h-[260px] items-center justify-center text-sm text-slate-400">{children}</div>;
}
