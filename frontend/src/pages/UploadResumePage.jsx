import { useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import toast from 'react-hot-toast';
import { resumeApi } from '../services';
import { extractError } from '../services/apiClient';

const MAX_BYTES = 10 * 1024 * 1024;

export default function UploadResumePage() {
  const [file, setFile] = useState(null);
  const [name, setName] = useState('');
  const [dragging, setDragging] = useState(false);
  const queryClient = useQueryClient();
  const navigate = useNavigate();

  const mutation = useMutation({
    mutationFn: () => resumeApi.upload(file, name),
    onSuccess: (resume) => {
      toast.success('Resume uploaded and parsed');
      queryClient.invalidateQueries({ queryKey: ['resumes'] });
      navigate(`/resumes`, { state: { highlight: resume.id } });
    },
    onError: (error) => toast.error(extractError(error)),
  });

  const validateAndSet = (selected) => {
    if (!selected) return;
    if (selected.type !== 'application/pdf') {
      toast.error('Only PDF files are accepted');
      return;
    }
    if (selected.size > MAX_BYTES) {
      toast.error('File exceeds the 10MB limit');
      return;
    }
    setFile(selected);
    if (!name) setName(selected.name.replace(/\.pdf$/i, ''));
  };

  return (
    <div className="mx-auto max-w-2xl">
      <h1 className="text-2xl font-bold text-slate-900">Upload your resume</h1>
      <p className="mt-1 text-sm text-slate-600">PDF only, up to 10MB. We extract and structure the content automatically.</p>

      <div className="card mt-6 space-y-5">
        <div>
          <label className="label" htmlFor="resume-name">Resume name</label>
          <input
            id="resume-name"
            className="input"
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="e.g. Backend Engineer 2026"
          />
        </div>

        <div
          onDragOver={(e) => {
            e.preventDefault();
            setDragging(true);
          }}
          onDragLeave={() => setDragging(false)}
          onDrop={(e) => {
            e.preventDefault();
            setDragging(false);
            validateAndSet(e.dataTransfer.files?.[0]);
          }}
          className={`flex flex-col items-center justify-center rounded-xl border-2 border-dashed px-6 py-12 text-center transition ${
            dragging ? 'border-brand-500 bg-brand-50' : 'border-slate-300'
          }`}
        >
          <p className="text-sm text-slate-600">
            {file ? (
              <span className="font-medium text-slate-900">{file.name}</span>
            ) : (
              'Drag & drop your PDF here, or'
            )}
          </p>
          <label className="btn-secondary mt-3 cursor-pointer">
            Browse files
            <input
              type="file"
              accept="application/pdf"
              className="hidden"
              onChange={(e) => validateAndSet(e.target.files?.[0])}
            />
          </label>
        </div>

        <button
          className="btn-primary w-full"
          disabled={!file || mutation.isPending}
          onClick={() => mutation.mutate()}
        >
          {mutation.isPending ? 'Uploading & parsing…' : 'Upload resume'}
        </button>
      </div>
    </div>
  );
}
