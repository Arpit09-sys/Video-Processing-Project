import { useState, useEffect } from 'react';

function VideoPlayer({ video, onClose }) {
  const [quality, setQuality] = useState('720p');

  const qualityMap = {
    '480p': video.transcoded480pUrl,
    '720p': video.transcoded720pUrl,
    '1080p': video.transcoded1080pUrl,
    'Original': video.originalUrl,
  };

  // Pick the best available quality
  useEffect(() => {
    if (qualityMap['720p']) setQuality('720p');
    else if (qualityMap['1080p']) setQuality('1080p');
    else if (qualityMap['480p']) setQuality('480p');
    else setQuality('Original');
  }, [video]);

  const currentUrl = qualityMap[quality];

  const formatSize = (bytes) => {
    if (!bytes) return '—';
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
    return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
  };

  const formatDuration = (seconds) => {
    if (!seconds) return '—';
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const formatDate = (dateStr) => {
    if (!dateStr) return '—';
    const d = new Date(dateStr.endsWith('Z') ? dateStr : `${dateStr}Z`);
    return d.toLocaleString();
  };

  // Close on Escape key
  useEffect(() => {
    const handleKey = (e) => { if (e.key === 'Escape') onClose(); };
    window.addEventListener('keydown', handleKey);
    return () => window.removeEventListener('keydown', handleKey);
  }, [onClose]);

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={(e) => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{video.title}</h2>
          <button className="modal-close" onClick={onClose} aria-label="Close">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="18" y1="6" x2="6" y2="18"></line><line x1="6" y1="6" x2="18" y2="18"></line></svg>
          </button>
        </div>

        <div className="modal-body">
          {currentUrl && video.status === 'COMPLETED' ? (
            <>
              <div className="player-container">
                <video
                  key={currentUrl}
                  controls
                  autoPlay
                  src={currentUrl}
                >
                  Your browser does not support the video tag.
                </video>
              </div>

              <div className="quality-selector">
                <label>Quality:</label>
                {Object.entries(qualityMap).map(([q, url]) =>
                  url ? (
                    <button
                      key={q}
                      className={`quality-btn ${quality === q ? 'active' : ''}`}
                      onClick={() => setQuality(q)}
                    >
                      {q}
                    </button>
                  ) : null
                )}
              </div>
            </>
          ) : (
            <div style={{ textAlign: 'center', padding: '40px' }}>
              <span className={`status-badge status-${video.status?.toLowerCase()}`}>
                {video.status}
              </span>
              {video.status === 'PROCESSING' && (
                <div className="progress-bar-container" style={{ marginTop: '20px' }}>
                  <div className="progress-bar">
                    <div
                      className="progress-bar-fill"
                      style={{ width: `${video.progressPercent || 0}%` }}
                    ></div>
                  </div>
                  <p className="progress-text">
                    Transcoding... {video.progressPercent || 0}%
                  </p>
                </div>
              )}
            </div>
          )}

          <div className="video-details">
            <div className="detail-item">
              <div className="detail-label">Status</div>
              <div className="detail-value">
                <span className={`status-badge status-${video.status?.toLowerCase()}`}>
                  {video.status}
                </span>
              </div>
            </div>
            <div className="detail-item">
              <div className="detail-label">File Size</div>
              <div className="detail-value">{formatSize(video.fileSize)}</div>
            </div>
            <div className="detail-item">
              <div className="detail-label">Duration</div>
              <div className="detail-value">{formatDuration(video.duration)}</div>
            </div>
            <div className="detail-item">
              <div className="detail-label">Format</div>
              <div className="detail-value">{video.contentType || '—'}</div>
            </div>
            <div className="detail-item">
              <div className="detail-label">Uploaded</div>
              <div className="detail-value">{formatDate(video.createdAt)}</div>
            </div>
            <div className="detail-item">
              <div className="detail-label">Original File</div>
              <div className="detail-value" style={{ fontSize: '12px', wordBreak: 'break-all' }}>
                {video.originalFilename || '—'}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default VideoPlayer;
