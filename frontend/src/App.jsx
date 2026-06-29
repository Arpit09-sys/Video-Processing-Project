import { useState, useEffect, useCallback, useRef } from 'react';
import UploadZone from './components/UploadZone.jsx';
import VideoGrid from './components/VideoGrid.jsx';
import VideoPlayer from './components/VideoPlayer.jsx';
import StatsBar from './components/StatsBar.jsx';
import Toast from './components/Toast.jsx';
import { videoApi } from './api.js';

function App() {
  const [videos, setVideos] = useState([]);
  const [stats, setStats] = useState(null);
  const [selectedVideo, setSelectedVideo] = useState(null);
  const [toast, setToast] = useState(null);
  const [loading, setLoading] = useState(true);
  const uploadRef = useRef(null);

  const showToast = useCallback((message, type = 'success') => {
    setToast({ message, type });
    setTimeout(() => setToast(null), 3000);
  }, []);

  const fetchVideos = useCallback(async () => {
    try {
      const res = await videoApi.list();
      setVideos(res.data.content || []);
    } catch (err) {
      console.error('Failed to fetch videos:', err);
    } finally {
      setLoading(false);
    }
  }, []);

  const fetchStats = useCallback(async () => {
    try {
      const res = await videoApi.getStats();
      setStats(res.data);
    } catch (err) {
      console.error('Failed to fetch stats:', err);
    }
  }, []);

  useEffect(() => {
    fetchVideos();
    fetchStats();
    // Poll for updates every 5 seconds
    const interval = setInterval(() => {
      fetchVideos();
      fetchStats();
    }, 5000);
    return () => clearInterval(interval);
  }, [fetchVideos, fetchStats]);

  const handleUploadComplete = useCallback(() => {
    showToast('Video uploaded successfully! Transcoding started.');
    fetchVideos();
    fetchStats();
  }, [showToast, fetchVideos, fetchStats]);

  const handleDelete = useCallback(async (id) => {
    try {
      await videoApi.delete(id);
      showToast('Video deleted.');
      fetchVideos();
      fetchStats();
    } catch (err) {
      showToast('Failed to delete video.', 'error');
    }
  }, [showToast, fetchVideos, fetchStats]);

  const handlePlay = useCallback(async (video) => {
    try {
      const res = await videoApi.get(video.id);
      setSelectedVideo(res.data);
    } catch {
      setSelectedVideo(video);
    }
  }, []);

  const scrollToUpload = () => {
    uploadRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <>
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <div className="logo-icon">▶</div>
            <span className="logo-text">CloudStream</span>
          </div>
          <div className="header-status">
            <span className="spinner" style={{ opacity: loading ? 1 : 0 }}></span>
            <span>{videos.length} video{videos.length !== 1 ? 's' : ''}</span>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="hero">
        <div className="app-container">
          <div className="hero-badge">
            <span className="hero-badge-dot"></span>
            Cloud-Powered Video Platform
          </div>
          <h1>
            Process your videos.{' '}
            <span className="hero-gradient-text">Effortlessly.</span>
          </h1>
          <p>
            Upload any video and let CloudStream handle the rest. Automatic transcoding 
            to multiple resolutions, real-time progress tracking, and instant streaming — 
            all in one seamless workflow.
          </p>
          <div className="hero-actions">
            <button className="btn btn-primary btn-lg" onClick={scrollToUpload}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              Upload Video
            </button>
            <button className="btn btn-secondary btn-lg" onClick={() => document.getElementById('library')?.scrollIntoView({ behavior: 'smooth' })}>
              Browse Library
            </button>
          </div>
        </div>
      </section>

      <main className="app-container content-area">
        {/* Features */}
        <div className="features-grid">
          <div className="feature-card">
            <div className="feature-icon feature-icon-blue">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="16 16 12 12 8 16"></polyline>
                <line x1="12" y1="12" x2="12" y2="21"></line>
                <path d="M20.39 18.39A5 5 0 0 0 18 9h-1.26A8 8 0 1 0 3 16.3"></path>
              </svg>
            </div>
            <h3>Cloud Upload</h3>
            <p>Drag, drop, and upload videos up to 3GB. Support for MP4, WebM, MKV, and AVI formats.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon feature-icon-green">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polygon points="23 7 16 12 23 17 23 7"></polygon>
                <rect x="1" y="5" width="15" height="14" rx="2" ry="2"></rect>
              </svg>
            </div>
            <h3>Multi-Resolution</h3>
            <p>Automatic transcoding to 480p, 720p, and 1080p. Every video optimized for any screen size.</p>
          </div>
          <div className="feature-card">
            <div className="feature-icon feature-icon-purple">
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="22 12 18 12 15 21 9 3 6 12 2 12"></polyline>
              </svg>
            </div>
            <h3>Real-Time Tracking</h3>
            <p>Monitor transcoding progress live. Know exactly when your videos are ready to stream.</p>
          </div>
        </div>

        <hr className="section-divider" />

        {/* Stats */}
        {stats && <StatsBar stats={stats} />}

        {/* Upload */}
        <section ref={uploadRef}>
          <h2 className="section-title">Upload Video</h2>
          <p className="section-subtitle">Choose a video file to upload and process automatically.</p>
          <UploadZone
            onUploadComplete={handleUploadComplete}
            onError={(msg) => showToast(msg, 'error')}
          />
        </section>

        <hr className="section-divider" />

        {/* Video Library */}
        <section id="library">
          <h2 className="section-title">Video Library</h2>
          <p className="section-subtitle">All your uploaded and processed videos in one place.</p>
          <VideoGrid
            videos={videos}
            loading={loading}
            onPlay={handlePlay}
            onDelete={handleDelete}
          />
        </section>
      </main>

      {selectedVideo && (
        <VideoPlayer
          video={selectedVideo}
          onClose={() => setSelectedVideo(null)}
        />
      )}

      {toast && <Toast message={toast.message} type={toast.type} />}
    </>
  );
}

export default App;
