function VideoGrid({ videos, loading, onPlay, onDelete }) {
  const formatSize = (bytes) => {
    if (!bytes) return '—';
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  };

  const formatDuration = (seconds) => {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    // Append 'Z' if missing to ensure it parses as UTC
    const d = new Date(dateStr.endsWith('Z') ? dateStr : `${dateStr}Z`);
    return d.toLocaleDateString('en-US', {
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  if (!loading && videos.length === 0) {
    return (
      <div className="empty-state">
        <div className="empty-state-icon">
          <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1" strokeLinecap="round" strokeLinejoin="round">
            <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18"></rect>
            <line x1="7" y1="2" x2="7" y2="22"></line>
            <line x1="17" y1="2" x2="17" y2="22"></line>
            <line x1="2" y1="12" x2="22" y2="12"></line>
            <line x1="2" y1="7" x2="7" y2="7"></line>
            <line x1="2" y1="17" x2="7" y2="17"></line>
            <line x1="17" y1="17" x2="22" y2="17"></line>
            <line x1="17" y1="7" x2="22" y2="7"></line>
          </svg>
        </div>
        <h3>Your library is empty</h3>
        <p>Upload your first video to get started with CloudStream.</p>
      </div>
    );
  }

  return (
    <div className="video-grid">
      {videos.map((video) => (
        <div key={video.id} className="video-card">
          <div className="video-thumbnail">
            {video.thumbnailUrl ? (
              <img src={video.thumbnailUrl} alt={video.title} />
            ) : (
              <span className="video-thumbnail-placeholder">
                <svg width="32" height="32" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
              </span>
            )}
            {video.duration > 0 && (
              <span className="video-duration">
                {formatDuration(video.duration)}
              </span>
            )}
          </div>

          <div className="video-card-body">
            <div className="video-title" title={video.title}>
              {video.title}
            </div>

            <div className="video-meta">
              <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path><polyline points="7 10 12 15 17 10"></polyline><line x1="12" y1="15" x2="12" y2="3"></line></svg>
                {formatSize(video.fileSize)}
              </span>
              <span style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"></rect><line x1="16" y1="2" x2="16" y2="6"></line><line x1="8" y1="2" x2="8" y2="6"></line><line x1="3" y1="10" x2="21" y2="10"></line></svg>
                {formatDate(video.createdAt)}
              </span>
            </div>

            <span className={`status-badge status-${video.status?.toLowerCase()}`}>
              {video.status}
            </span>

            {(video.status === 'PROCESSING' || video.status === 'QUEUED') && (
              <div className="video-progress">
                <div className="progress-bar">
                  <div
                    className="progress-bar-fill"
                    style={{ width: `${video.progressPercent || 0}%` }}
                  ></div>
                </div>
                <div className="video-progress-text">
                  {video.status === 'QUEUED'
                    ? 'Waiting in queue…'
                    : `Transcoding… ${video.progressPercent || 0}%`}
                </div>
              </div>
            )}

            {video.status === 'FAILED' && video.errorMessage && (
              <p style={{ fontSize: '12px', color: 'var(--error)', marginTop: '8px', lineHeight: 1.4 }}>
                {video.errorMessage}
              </p>
            )}

            <div className="video-actions">
              {video.status === 'COMPLETED' && (
                <button
                  className="btn btn-primary btn-sm"
                  onClick={() => onPlay(video)}
                >
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="5 3 19 12 5 21 5 3"></polygon></svg>
                  Play
                </button>
              )}
              <button
                className="btn btn-secondary btn-sm"
                onClick={() => onPlay(video)}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>
                Details
              </button>
              <button
                className="btn btn-danger btn-sm"
                onClick={() => {
                  if (window.confirm('Delete this video?')) onDelete(video.id);
                }}
              >
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path></svg>
              </button>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

export default VideoGrid;
