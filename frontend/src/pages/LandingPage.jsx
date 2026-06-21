import { Link } from 'react-router-dom';
import { motion } from 'framer-motion';

const features = [
  { title: 'Resume Analysis', desc: 'Upload your PDF and get an ATS score with skill-gap insights in seconds.' },
  { title: 'Skill Gap Detection', desc: 'See exactly which skills you are missing for any job description.' },
  { title: 'AI Suggestions', desc: 'Get actionable rewrites for summaries, bullet points, and keywords.' },
  { title: 'Mock Interviews', desc: 'Practice with an AI interviewer that scores and coaches you.' },
];

const steps = [
  { n: '1', title: 'Upload your resume', desc: 'We extract and structure your experience and skills.' },
  { n: '2', title: 'Add a job description', desc: 'Paste any JD — we structure the requirements with AI.' },
  { n: '3', title: 'Analyze & practice', desc: 'Get your match score, fixes, and a tailored mock interview.' },
];

const testimonials = [
  { name: 'Priya S.', role: 'Backend Engineer', quote: 'Bumped my match score from 58 to 89 and landed 3 interviews.' },
  { name: 'Marcus L.', role: 'Full-Stack Dev', quote: 'The mock interviews were scarily realistic. Huge confidence boost.' },
  { name: 'Aisha K.', role: 'Data Analyst', quote: 'Finally understood which keywords my resume was missing.' },
];

const fadeUp = {
  hidden: { opacity: 0, y: 24 },
  show: { opacity: 1, y: 0 },
};

export default function LandingPage() {
  return (
    <div className="min-h-screen bg-white">
      {/* Nav */}
      <header className="mx-auto flex max-w-7xl items-center justify-between px-4 py-5">
        <div className="flex items-center gap-2 font-bold text-brand-700">
          <span className="grid h-8 w-8 place-items-center rounded-lg bg-brand-600 text-white">AI</span>
          Resume Coach
        </div>
        <div className="flex items-center gap-3">
          <Link to="/login" className="btn-ghost">Sign in</Link>
          <Link to="/register" className="btn-primary">Get Started</Link>
        </div>
      </header>

      {/* Hero */}
      <section className="relative overflow-hidden">
        <div className="mx-auto max-w-7xl px-4 py-20 text-center">
          <motion.h1
            initial="hidden"
            animate="show"
            variants={fadeUp}
            transition={{ duration: 0.6 }}
            className="mx-auto max-w-3xl text-4xl font-extrabold tracking-tight text-slate-900 sm:text-6xl"
          >
            Land Your Dream Job With <span className="text-brand-600">AI</span>
          </motion.h1>
          <motion.p
            initial="hidden"
            animate="show"
            variants={fadeUp}
            transition={{ duration: 0.6, delay: 0.15 }}
            className="mx-auto mt-6 max-w-2xl text-lg text-slate-600"
          >
            Analyze your resume, identify missing skills, and prepare for interviews using AI.
          </motion.p>
          <motion.div
            initial="hidden"
            animate="show"
            variants={fadeUp}
            transition={{ duration: 0.6, delay: 0.3 }}
            className="mt-10 flex items-center justify-center gap-4"
          >
            <Link to="/register" className="btn-primary px-6 py-3 text-base">Get Started</Link>
            <Link to="/login" className="btn-secondary px-6 py-3 text-base">Try Demo</Link>
          </motion.div>
        </div>
        <div className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-96 bg-gradient-to-b from-brand-50 to-transparent" />
      </section>

      {/* Features */}
      <section className="mx-auto max-w-7xl px-4 py-16">
        <h2 className="text-center text-3xl font-bold text-slate-900">Everything you need to get hired</h2>
        <div className="mt-12 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {features.map((f, i) => (
            <motion.div
              key={f.title}
              initial="hidden"
              whileInView="show"
              viewport={{ once: true }}
              variants={fadeUp}
              transition={{ duration: 0.4, delay: i * 0.1 }}
              className="card"
            >
              <h3 className="text-lg font-semibold text-slate-900">{f.title}</h3>
              <p className="mt-2 text-sm text-slate-600">{f.desc}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* How it works */}
      <section className="bg-slate-50 py-16">
        <div className="mx-auto max-w-7xl px-4">
          <h2 className="text-center text-3xl font-bold text-slate-900">How it works</h2>
          <div className="mt-12 grid gap-8 md:grid-cols-3">
            {steps.map((s) => (
              <div key={s.n} className="text-center">
                <div className="mx-auto grid h-12 w-12 place-items-center rounded-full bg-brand-600 text-lg font-bold text-white">
                  {s.n}
                </div>
                <h3 className="mt-4 text-lg font-semibold text-slate-900">{s.title}</h3>
                <p className="mt-2 text-sm text-slate-600">{s.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Testimonials */}
      <section className="mx-auto max-w-7xl px-4 py-16">
        <h2 className="text-center text-3xl font-bold text-slate-900">Loved by job seekers</h2>
        <div className="mt-12 grid gap-6 md:grid-cols-3">
          {testimonials.map((t) => (
            <div key={t.name} className="card">
              <p className="text-slate-700">“{t.quote}”</p>
              <div className="mt-4 text-sm font-semibold text-slate-900">{t.name}</div>
              <div className="text-xs text-slate-500">{t.role}</div>
            </div>
          ))}
        </div>
      </section>

      {/* Pricing placeholder */}
      <section className="bg-slate-50 py-16">
        <div className="mx-auto max-w-3xl px-4 text-center">
          <h2 className="text-3xl font-bold text-slate-900">Simple pricing</h2>
          <p className="mt-3 text-slate-600">Start free. Upgrade when you are ready. (Pricing coming soon.)</p>
          <div className="mt-8 inline-block card">
            <div className="text-4xl font-extrabold text-brand-700">Free</div>
            <p className="mt-2 text-sm text-slate-600">Resume analysis, skill gaps & mock interviews</p>
            <Link to="/register" className="btn-primary mt-6 w-full">Get Started</Link>
          </div>
        </div>
      </section>

      <footer className="border-t border-slate-200 py-10 text-center text-sm text-slate-500">
        © {new Date().getFullYear()} AI Resume Analyzer & Interview Coach. Built for job seekers.
      </footer>
    </div>
  );
}
