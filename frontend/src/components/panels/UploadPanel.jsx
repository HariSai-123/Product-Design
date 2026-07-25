import { useState, useRef, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { apiUploadSample } from '../../services/api';

const APPLIANCE_TYPES = ['Catheter', 'Surgical Syringe', 'Scalpel', 'Endoscope Tube', 'Petri Dish (Control)', 'Other'];

export default function UploadPanel({ setActiveSample, showNotification, reloadSamples }) {
  const navigate = useNavigate();
  const [form, setForm] = useState({ batchId: '', applianceType: 'Catheter', dilution: 1, comments: '' });
  const [selectedFile, setSelectedFile] = useState(null);
  const [previewUrl, setPreviewUrl] = useState(null);
  const [captureMode, setCaptureMode] = useState('local');
  const [loading, setLoading] = useState(false);
  const [isDragOver, setIsDragOver] = useState(false);
  const fileInputRef = useRef(null);
  const videoRef = useRef(null);
  const [cameraStream, setCameraStream] = useState(null);

  // AbortController ref — cancelled when a new file is selected mid-upload
  const abortControllerRef = useRef(null);
  // Unique request ID — only update UI if this matches the response
  const currentRequestIdRef = useRef(null);

  const clearFileState = useCallback(() => {
    // Cancel any in-flight upload for the previous image
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
      abortControllerRef.current = null;
    }
    currentRequestIdRef.current = null;
    setActiveSample(null);
    if (previewUrl && previewUrl.startsWith('blob:')) {
      URL.revokeObjectURL(previewUrl);
    }
    setPreviewUrl(null);
    setSelectedFile(null);
  }, [previewUrl, setActiveSample]);

  const handleFileSelect = useCallback((file) => {
    // Step 1: Clear ALL previous state (analysis result, image, sample ID, object URL)
    clearFileState();

    if (!file || !file.type.startsWith('image/')) {
      showNotification('danger', 'Invalid file type. Please upload a valid image file (JPG, PNG, TIFF).');
      return;
    }

    // Step 2: Generate a fresh blob URL for this exact File object
    const objectUrl = URL.createObjectURL(file);
    const img = new Image();
    img.onload = () => {
      if (img.width < 100 || img.height < 100) {
        showNotification('danger', 'Image resolution is too low. Please upload a clear photo of a Petri dish.');
        URL.revokeObjectURL(objectUrl);
        return;
      }
      if (img.width / img.height > 3 || img.height / img.width > 3) {
        showNotification('danger', 'Image dimensions are invalid. Please upload a standard Petri dish photo.');
        URL.revokeObjectURL(objectUrl);
        return;
      }
      // Step 3: Store the new File and preview
      setSelectedFile(file);
      setPreviewUrl(objectUrl);
    };
    img.src = objectUrl;
  }, [clearFileState, showNotification]);

  const handleDrop = (e) => {
    e.preventDefault();
    setIsDragOver(false);
    if (e.dataTransfer.files.length > 0) handleFileSelect(e.dataTransfer.files[0]);
  };

  const handleCameraMode = async () => {
    setCaptureMode('camera');
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ video: true });
      setCameraStream(stream);
      if (videoRef.current) videoRef.current.srcObject = stream;
    } catch (err) {
      showNotification('danger', 'Camera access denied or unavailable.');
      setCaptureMode('local');
    }
  };

  const stopCamera = () => {
    if (cameraStream) { cameraStream.getTracks().forEach(t => t.stop()); setCameraStream(null); }
  };

  const handleTabChange = (mode) => {
    if (mode !== 'camera') stopCamera();
    setCaptureMode(mode);
    if (mode === 'camera') handleCameraMode();
  };

  const handleSnapshot = () => {
    const video = videoRef.current;
    if (!video) return;
    const canvas = document.createElement('canvas');
    canvas.width = video.videoWidth;
    canvas.height = video.videoHeight;
    canvas.getContext('2d').drawImage(video, 0, 0);
    canvas.toBlob(blob => {
      const file = new File([blob], `camera-snapshot-${Date.now()}.png`, { type: 'image/png' });
      handleFileSelect(file);
      stopCamera();
      setCaptureMode('local');
    }, 'image/png');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    // Step 4: Validate the current File object is fresh
    if (!selectedFile) {
      showNotification('danger', 'Please select an appliance agar image to analyze.');
      return;
    }
    if (!form.batchId.trim()) {
      showNotification('danger', 'Please enter a Batch ID.');
      return;
    }

    setLoading(true);

    // Step 5: Create a unique request ID and AbortController for THIS submission
    const requestId = crypto.randomUUID();
    currentRequestIdRef.current = requestId;
    const controller = new AbortController();
    abortControllerRef.current = controller;

    try {
      // Step 6: Build FormData using the CURRENT selectedFile directly
      // Field name 'sampleImage' must match the backend multer field name exactly
      const fd = new FormData();
      fd.append('sampleImage', selectedFile, selectedFile.name);
      fd.append('batchId', form.batchId.trim());
      fd.append('applianceType', form.applianceType);
      fd.append('dilutionFactor', String(form.dilution));
      fd.append('operatorName', 'Lab Operator');
      fd.append('comments', form.comments);

      const data = await apiUploadSample(fd, controller.signal);

      // Step 7: Only update UI if this response belongs to the current request
      // (prevents stale Image A response from overwriting Image B's UI)
      if (currentRequestIdRef.current !== requestId) {
        console.warn('[Upload] Stale response discarded. Request ID mismatch.');
        return;
      }

      showNotification('success', `Analysis complete! Detected ${data.sample.colonyCount} colonies.`);
      setActiveSample(data.sample);
      await reloadSamples();
      navigate('/app/analysis');
    } catch (err) {
      if (err.name === 'AbortError') {
        // Upload was cancelled because user selected a new image — expected
        return;
      }
      // Only show error if this is still the current request
      if (currentRequestIdRef.current === requestId) {
        showNotification('danger', err.message || 'Upload failed. Please try again.');
      }
    } finally {
      if (currentRequestIdRef.current === requestId) {
        setLoading(false);
        abortControllerRef.current = null;
      }
    }
  };

  return (
    <div id="upload-panel" className="panel-view active">
      <div className="panel-header"><h2>Scan Appliance Sample</h2><p>Upload agar petri dish images of sanitization verification tests.</p></div>
      <div className="upload-grid">
        <form id="sample-upload-form" className="glass-card" onSubmit={handleSubmit}>
          <h3 style={{ marginBottom: 20 }}>1. Sample Specifications</h3>
          <div className="form-group">
            <label htmlFor="form-batch-id">Batch ID (Unique Identifier)</label>
            <input type="text" id="form-batch-id" className="form-input" placeholder="e.g. B-2026-CAT88" required value={form.batchId} onChange={e => setForm({ ...form, batchId: e.target.value })} />
          </div>
          <div className="form-group">
            <label htmlFor="form-appliance-type">Tested Medical Appliance</label>
            <select id="form-appliance-type" className="form-input" required value={form.applianceType} onChange={e => setForm({ ...form, applianceType: e.target.value })}>
              {APPLIANCE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label htmlFor="form-dilution">Plating Dilution Factor (z-Ratio)</label>
            <input type="number" id="form-dilution" className="form-input" min="1" step="1" value={form.dilution} onChange={e => setForm({ ...form, dilution: e.target.value })} />
          </div>
          <div className="form-group">
            <label htmlFor="form-comments">Operator Remarks</label>
            <textarea id="form-comments" className="form-input" rows="3" placeholder="Enter custom pathology remarks..." value={form.comments} onChange={e => setForm({ ...form, comments: e.target.value })} />
          </div>
          <button
            type="submit"
            className="btn btn-primary"
            style={{ width: '100%', marginTop: 10 }}
            disabled={loading || !selectedFile}
          >
            {loading ? 'Analyzing Scan...' : 'Execute Automated Diagnostic Scan'}
          </button>
        </form>

        <div className="glass-card" style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
          <h3 style={{ marginBottom: 16 }}>2. Agar Plate Visual Capture</h3>
          <div className="capture-tabs">
            {['local', 'camera'].map(mode => (
              <button key={mode} type="button" className={`capture-tab${captureMode === mode ? ' active' : ''}`} onClick={() => handleTabChange(mode)}>
                <span>{mode === 'local' ? 'Local File' : 'Live Camera'}</span>
              </button>
            ))}
          </div>

          <div className="capture-viewports">
            {captureMode === 'local' && (
              <div className="capture-viewport-panel active">
                <div
                  className={`dropzone${isDragOver ? ' dragover' : ''}`}
                  onClick={() => fileInputRef.current?.click()}
                  onDragOver={e => { e.preventDefault(); setIsDragOver(true); }}
                  onDragLeave={() => setIsDragOver(false)}
                  onDrop={handleDrop}
                >
                  <svg fill="none" stroke="currentColor" strokeWidth="1.5" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" d="M12 16.5V9.75m0 0l3 3m-3-3l-3 3M6.75 19.5a4.5 4.5 0 01-1.41-8.775 5.25 5.25 0 0110.233-2.33 3 3 0 013.758 3.848A3.752 3.752 0 0118 19.5H6.75z" /></svg>
                  <p>{selectedFile ? selectedFile.name : 'Drag & drop appliance agar scan file'}</p>
                  <span>Supports PNG, JPG, and TIFF</span>
                  <input
                    ref={fileInputRef}
                    type="file"
                    style={{ display: 'none' }}
                    accept="image/jpeg,image/png,image/tiff"
                    onChange={e => { if (e.target.files[0]) handleFileSelect(e.target.files[0]); e.target.value = ''; }}
                  />
                </div>
              </div>
            )}
            {captureMode === 'camera' && (
              <div className="capture-viewport-panel active">
                <div className="camera-stream-wrapper">
                  <video ref={videoRef} autoPlay playsInline muted />
                  <div className="camera-plate-overlay"><div className="camera-plate-circle"><span>ALIGN AGAR PLATE HERE</span></div></div>
                </div>
                <div className="camera-controls-bar">
                  <button type="button" className="btn btn-primary" onClick={handleSnapshot} style={{ borderRadius: 50, fontSize: 12 }}>📷 Take Scan Snapshot</button>
                </div>
              </div>
            )}
          </div>

          {previewUrl && (
            <div className="upload-preview-wrapper" style={{ position: 'relative' }}>
              <img className="preview-image" src={previewUrl} alt="Scan preview" />
              <button
                type="button"
                className="btn btn-secondary"
                style={{ padding: '6px 12px', fontSize: 11, marginTop: 10, width: '100%' }}
                onClick={clearFileState}
              >
                Reset Capture &amp; Take Another Scan
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
