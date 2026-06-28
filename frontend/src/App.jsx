import { useState, useEffect, useCallback } from 'react';
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

  return (
    <>
      <header className="header">
        <div className="header-inner">
          <div className="logo">
            <div className="logo-icon">▶</div>
            <span className="logo-text">CloudStream</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span className="spinner" style={{ opacity: loading ? 1 : 0 }}></span>
            <span style={{ fontSize: '13px', color: 'var(--text-muted)' }}>
              {videos.length} videos
            </span>
          </div>
        </div>
      </header>

      <main className="app-container content-area">
        {stats && <StatsBar stats={stats} />}

        <section>
          <h2 className="section-title">Upload Video</h2>
          <UploadZone
            onUploadComplete={handleUploadComplete}
            onError={(msg) => showToast(msg, 'error')}
          />
        </section>

        <section style={{ marginTop: '48px' }}>
          <h2 className="section-title">Video Library</h2>
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
