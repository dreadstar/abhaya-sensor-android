import React, { useState, useRef, useEffect } from 'react';
import { Play, Pause, Settings, Smartphone, Wifi, MapPin, Battery, Activity, Thermometer, Eye, Heart, Lightbulb, Volume2, ChevronDown, ChevronRight, Camera, Mic, Flashlight, Focus, RotateCcw, Image, Video } from 'lucide-react';

interface Sensor {
  id: string;
  name: string;
  desc: string;
}

interface SensorCategory {
  name: string;
  icon: React.ComponentType<React.SVGProps<SVGSVGElement>>;
  sensors: Sensor[];
}

interface FrequencyOption {
  value: number;
  label: string;
  desc: string;
}

interface CameraConfig {
  enabled: boolean;
  camera: 'front' | 'back';
  mode: 'video' | 'photo' | 'periodic';
  frameRate: number;
  aspectRatio: string;
  focus: 'auto' | 'manual';
  focusDistance?: number;
  flash: 'on' | 'off' | 'auto';
  periodicInterval?: number;
}

interface AudioConfig {
  enabled: boolean;
  quality: 'low' | 'medium' | 'high';
  threshold: number;
  recordOnThreshold: boolean;
}

const SensorStreamingUI: React.FC = () => {
  const [isStreaming, setIsStreaming] = useState<boolean>(false);
  const [pollingFrequency, setPollingFrequency] = useState<number>(10);
  const [selectedSensors, setSelectedSensors] = useState<Set<string>>(new Set());
  const [expandedCategories, setExpandedCategories] = useState<Set<string>>(new Set(['motion']));
  const [cameraConfig, setCameraConfig] = useState<CameraConfig>({
    enabled: false,
    camera: 'back',
    mode: 'video',
    frameRate: 30,
    aspectRatio: '16:9',
    focus: 'auto',
    flash: 'auto',
    periodicInterval: 10
  });
  const [audioConfig, setAudioConfig] = useState<AudioConfig>({
    enabled: false,
    quality: 'medium',
    threshold: 30,
    recordOnThreshold: false
  });
  const [cameraPreview, setCameraPreview] = useState<boolean>(false);
  const videoRef = useRef<HTMLVideoElement>(null);
  const [currentSoundLevel, setCurrentSoundLevel] = useState<number>(0);

  const sensorCategories: Record<string, SensorCategory> = {
    motion: {
      name: 'Motion Sensors',
      icon: Activity,
      sensors: [
        { id: 'accelerometer', name: 'Accelerometer', desc: 'Device acceleration (m/s²)' },
        { id: 'gyroscope', name: 'Gyroscope', desc: 'Rotation rate (rad/s)' },
        { id: 'magnetometer', name: 'Magnetometer', desc: 'Magnetic field (μT)' },
        { id: 'linear_acceleration', name: 'Linear Acceleration', desc: 'Acceleration without gravity' },
        { id: 'rotation_vector', name: 'Rotation Vector', desc: 'Device orientation' },
        { id: 'gravity', name: 'Gravity', desc: 'Gravity direction & magnitude' },
        { id: 'step_counter', name: 'Step Counter', desc: 'Steps since reboot' },
        { id: 'step_detector', name: 'Step Detector', desc: 'Individual step detection' },
      ]
    },
    environmental: {
      name: 'Environmental',
      icon: Thermometer,
      sensors: [
        { id: 'temperature', name: 'Ambient Temperature', desc: 'Room temperature (°C)' },
        { id: 'pressure', name: 'Pressure', desc: 'Atmospheric pressure (hPa)' },
        { id: 'humidity', name: 'Humidity', desc: 'Relative humidity (%)' },
        { id: 'light', name: 'Light', desc: 'Ambient light (lx)' },
      ]
    },
    proximity: {
      name: 'Proximity & Gestures',
      icon: Eye,
      sensors: [
        { id: 'proximity', name: 'Proximity', desc: 'Distance to object (cm)' },
        { id: 'wake_gesture', name: 'Wake Gesture', desc: 'Wake-up detection' },
        { id: 'pickup_gesture', name: 'Pick Up Gesture', desc: 'Device pickup detection' },
      ]
    },
    health: {
      name: 'Health Sensors',
      icon: Heart,
      sensors: [
        { id: 'heart_rate', name: 'Heart Rate', desc: 'Heart rate (BPM)' },
        { id: 'heart_beat', name: 'Heart Beat', desc: 'Heartbeat detection' },
      ]
    },
    system: {
      name: 'System Metrics',
      icon: Smartphone,
      sensors: [
        { id: 'battery_level', name: 'Battery Level', desc: 'Charge percentage & status' },
        { id: 'cpu_usage', name: 'CPU Usage', desc: 'Processor utilization' },
        { id: 'memory_usage', name: 'Memory Usage', desc: 'RAM usage statistics' },
        { id: 'storage', name: 'Storage', desc: 'Available storage space' },
        { id: 'thermal', name: 'Thermal State', desc: 'Device temperature status' },
      ]
    },
    network: {
      name: 'Network & Location',
      icon: Wifi,
      sensors: [
        { id: 'wifi_signal', name: 'WiFi Signal', desc: 'Signal strength & info' },
        { id: 'cellular_signal', name: 'Cellular Signal', desc: 'Mobile signal strength' },
        { id: 'gps_location', name: 'GPS Location', desc: 'Precise location data' },
        { id: 'network_location', name: 'Network Location', desc: 'Cell/WiFi location' },
        { id: 'data_usage', name: 'Data Usage', desc: 'Network usage statistics' },
      ]
    },
    media: {
      name: 'Camera & Audio',
      icon: Camera,
      sensors: [
        { id: 'camera_stream', name: 'Camera Stream', desc: 'Video capture with controls' },
        { id: 'audio_stream', name: 'Audio Stream', desc: 'Audio capture with threshold' },
        { id: 'periodic_photos', name: 'Periodic Photos', desc: 'Timed photo capture' },
      ]
    },
    audio: {
      name: 'Audio & Display',
      icon: Volume2,
      sensors: [
        { id: 'audio_level', name: 'Audio Levels', desc: 'Volume & audio state' },
        { id: 'screen_brightness', name: 'Screen Brightness', desc: 'Display brightness level' },
        { id: 'orientation', name: 'Screen Orientation', desc: 'Device orientation' },
      ]
    }
  };

  const frequencyOptions: FrequencyOption[] = [
    { value: 1, label: '1 Hz (1/sec)', desc: 'Very slow' },
    { value: 5, label: '5 Hz (5/sec)', desc: 'Slow' },
    { value: 10, label: '10 Hz (10/sec)', desc: 'Normal' },
    { value: 25, label: '25 Hz (25/sec)', desc: 'Fast' },
    { value: 50, label: '50 Hz (50/sec)', desc: 'Very fast' },
    { value: 100, label: '100 Hz (100/sec)', desc: 'Maximum' },
  ];

  const toggleCategory = (categoryId: string): void => {
    const newExpanded = new Set(expandedCategories);
    if (newExpanded.has(categoryId)) {
      newExpanded.delete(categoryId);
    } else {
      newExpanded.add(categoryId);
    }
    setExpandedCategories(newExpanded);
  };

  const toggleSensor = (sensorId: string): void => {
    const newSelected = new Set(selectedSensors);
    if (newSelected.has(sensorId)) {
      newSelected.delete(sensorId);
    } else {
      newSelected.add(sensorId);
    }
    setSelectedSensors(newSelected);
  };

  const selectAllInCategory = (categoryId: string): void => {
    const newSelected = new Set(selectedSensors);
    sensorCategories[categoryId].sensors.forEach(sensor => {
      newSelected.add(sensor.id);
    });
    setSelectedSensors(newSelected);
  };

  const deselectAllInCategory = (categoryId: string): void => {
    const newSelected = new Set(selectedSensors);
    sensorCategories[categoryId].sensors.forEach(sensor => {
      newSelected.delete(sensor.id);
    });
    setSelectedSensors(newSelected);
  };

  const startStreaming = (): void => {
    if (selectedSensors.size === 0) {
      alert('Please select at least one sensor or metric to stream.');
      return;
    }
    // Begin streaming: update state and start camera/audio previews if configured
    setIsStreaming(true);
    if (cameraConfig.enabled) {
      // attempt to start camera preview when streaming starts
      startCameraPreview().catch(() => {});
    }
    if (audioConfig.enabled) {
      // audio monitoring will begin via useEffect when isStreaming is true
    }
  };

  const stopStreaming = (): void => {
    // Stop streaming and any active captures/previews
    setIsStreaming(false);
    if (cameraPreview) {
      try { stopCameraPreview(); } catch (_: any) {}
    }
    // Additional cleanup for audio or other captures can be added here
  };

  // Camera and audio management
  const startCameraPreview = async (): Promise<void> => {
    try {
      const constraints = {
        video: {
          facingMode: cameraConfig.camera === 'front' ? 'user' : 'environment',
          frameRate: cameraConfig.frameRate,
          aspectRatio: cameraConfig.aspectRatio === '16:9' ? 16/9 : 4/3
        }
      } as MediaStreamConstraints;
      
      const stream = await navigator.mediaDevices.getUserMedia(constraints);
      if (videoRef.current) {
        videoRef.current.srcObject = stream;
        setCameraPreview(true);
      }
    } catch (error) {
      console.error('Camera access failed:', error);
      alert('Camera access denied or not available');
    }
  };

  const stopCameraPreview = (): void => {
    if (videoRef.current && videoRef.current.srcObject) {
      const stream = videoRef.current.srcObject as MediaStream;
      stream.getTracks().forEach(track => track.stop());
      videoRef.current.srcObject = null;
      setCameraPreview(false);
    }
  };

  const updateCameraConfig = (updates: Partial<CameraConfig>): void => {
    setCameraConfig((prev: CameraConfig) => ({ ...prev, ...updates }));
    if (cameraPreview && (updates.camera || updates.frameRate || updates.aspectRatio)) {
      stopCameraPreview();
      setTimeout(startCameraPreview, 100);
    }
  };

  const updateAudioConfig = (updates: Partial<AudioConfig>): void => {
    setAudioConfig((prev: AudioConfig) => ({ ...prev, ...updates }));
  };

  // Simulate sound level monitoring
  useEffect(() => {
    let interval: any;
    if (audioConfig.enabled && isStreaming) {
      interval = setInterval(() => {
        setCurrentSoundLevel(Math.random() * 100);
      }, 100);
    }
    return () => clearInterval(interval);
  }, [audioConfig.enabled, isStreaming]);

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 text-white">
      {/* Header */}
      <div className="bg-black/30 backdrop-blur-md border-b border-white/10 sticky top-0 z-10">
        <div className="px-4 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-3">
              <div className="p-2 bg-gradient-to-r from-purple-500 to-blue-500 rounded-lg">
                <Activity className="w-6 h-6" />
              </div>
              <div>
                <h1 className="text-xl font-bold">Sensor Stream</h1>
                <p className="text-sm text-gray-300">Configure data streaming</p>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <div className={`w-3 h-3 rounded-full ${isStreaming ? 'bg-green-400 animate-pulse' : 'bg-gray-400'}`}></div>
              <span className="text-sm font-medium">
                {isStreaming ? 'Streaming' : 'Idle'}
              </span>
            </div>
          </div>
        </div>
      </div>

      {/* Camera Configuration Panel */}
      {(selectedSensors.has('camera_stream') || selectedSensors.has('periodic_photos')) && (
        <div className="p-4 border-b border-white/10 bg-black/20">
          <div className="flex items-center space-x-3 mb-4">
            <Camera className="w-5 h-5 text-blue-400" />
            <h2 className="text-lg font-semibold">Camera Configuration</h2>
          </div>
          
          {/* Camera Preview */}
          <div className="mb-4">
            <div className="relative bg-black rounded-lg overflow-hidden" style={{ aspectRatio: cameraConfig.aspectRatio }}>
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className={`w-full h-full object-cover ${cameraPreview ? '' : 'hidden'}`}
              />
              {!cameraPreview && (
                <div className="absolute inset-0 flex items-center justify-center text-gray-400">
                  <div className="text-center">
                    <Camera className="w-12 h-12 mx-auto mb-2 opacity-50" />
                    <p className="text-sm">Camera Preview</p>
                  </div>
                </div>
              )}
              <div className="absolute top-2 right-2 flex space-x-2">
                <button
                  onClick={cameraPreview ? stopCameraPreview : startCameraPreview}
                  className={`p-2 rounded-full ${cameraPreview ? 'bg-red-500/80' : 'bg-green-500/80'} backdrop-blur-sm`}
                >
                  {cameraPreview ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                </button>
                <button
                  onClick={() => updateCameraConfig({ camera: cameraConfig.camera === 'front' ? 'back' : 'front' })}
                  className="p-2 bg-blue-500/80 rounded-full backdrop-blur-sm"
                >
                  <RotateCcw className="w-4 h-4" />
                </button>
              </div>
            </div>
          </div>

          {/* Camera Controls */}
          <div className="grid grid-cols-2 gap-3 mb-4">
            <div>
              <label className="block text-sm font-medium mb-2">Camera</label>
              <select
                value={cameraConfig.camera}
                onChange={(e) => updateCameraConfig({ camera: e.target.value as 'front' | 'back' })}
                className="w-full bg-white/10 border border-white/20 rounded-lg p-2 text-white"
              >
                <option value="back">Back Camera</option>
                <option value="front">Front Camera</option>
              </select>
            </div>
            
            <div>
              <label className="block text-sm font-medium mb-2">Mode</label>
              <select
                value={cameraConfig.mode}
                onChange={(e) => updateCameraConfig({ mode: e.target.value as 'video' | 'photo' | 'periodic' })}
                className="w-full bg-white/10 border border-white/20 rounded-lg p-2 text-white"
              >
                <option value="video">Video Stream</option>
                <option value="photo">Single Photo</option>
                <option value="periodic">Periodic Photos</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Frame Rate</label>
              <select
                value={cameraConfig.frameRate}
                onChange={(e) => updateCameraConfig({ frameRate: parseInt(e.target.value) })}
                className="w-full bg-white/10 border border-white/20 rounded-lg p-2 text-white"
              >
                <option value={15}>15 FPS</option>
                <option value={24}>24 FPS</option>
                <option value={30}>30 FPS</option>
                <option value={60}>60 FPS</option>
              </select>
            </div>

            <div>
              <label className="block text-sm font-medium mb-2">Aspect Ratio</label>
              <select
                value={cameraConfig.aspectRatio}
                onChange={(e) => updateCameraConfig({ aspectRatio: e.target.value })}
                className="w-full bg-white/10 border border-white/20 rounded-lg p-2 text-white"
              >
                <option value="16:9">16:9 (Wide)</option>
                <option value="4:3">4:3 (Standard)</option>
                <option value="1:1">1:1 (Square)</option>
              </select>
            </div>
          </div>

          {/* Advanced Camera Controls */}
          <div className="grid grid-cols-3 gap-2 mb-4">
            <button
              onClick={() => updateCameraConfig({ focus: cameraConfig.focus === 'auto' ? 'manual' : 'auto' })}
              className={`flex items-center justify-center space-x-1 p-2 rounded-lg ${
                cameraConfig.focus === 'auto' ? 'bg-green-500/30 text-green-300' : 'bg-white/10 text-white'
              }`}
            >
              <Focus className="w-4 h-4" />
              <span className="text-xs">{cameraConfig.focus === 'auto' ? 'Auto' : 'Manual'}</span>
            </button>

            <button
              onClick={() => updateCameraConfig({ 
                flash: cameraConfig.flash === 'auto' ? 'on' : cameraConfig.flash === 'on' ? 'off' : 'auto' 
              })}
              className={`flex items-center justify-center space-x-1 p-2 rounded-lg ${
                cameraConfig.flash === 'on' ? 'bg-yellow-500/30 text-yellow-300' : 
                cameraConfig.flash === 'auto' ? 'bg-blue-500/30 text-blue-300' : 'bg-white/10 text-white'
              }`}
            >
              <Flashlight className="w-4 h-4" />
              <span className="text-xs capitalize">{cameraConfig.flash}</span>
            </button>

            <div className="flex items-center space-x-1">
              <Image className="w-4 h-4" />
              <span className="text-xs">{cameraConfig.mode}</span>
            </div>
          </div>

          {/* Periodic Photo Interval */}
          {cameraConfig.mode === 'periodic' && (
            <div>
              <label className="block text-sm font-medium mb-2">
                Photo Interval: {cameraConfig.periodicInterval}s
              </label>
              <input
                type="range"
                min="1"
                max="300"
                value={cameraConfig.periodicInterval}
                onChange={(e) => updateCameraConfig({ periodicInterval: parseInt(e.target.value) })}
                className="w-full accent-blue-500"
              />
            </div>
          )}
        </div>
      )}

      {/* Audio Configuration Panel */}
      {selectedSensors.has('audio_stream') && (
        <div className="p-4 border-b border-white/10 bg-black/20">
          <div className="flex items-center space-x-3 mb-4">
            <Mic className="w-5 h-5 text-green-400" />
            <h2 className="text-lg font-semibold">Audio Configuration</h2>
          </div>

          {/* Audio Quality */}
          <div className="grid grid-cols-2 gap-3 mb-4">
            <div>
              <label className="block text-sm font-medium mb-2">Quality</label>
              <select
                value={audioConfig.quality}
                onChange={(e) => updateAudioConfig({ quality: e.target.value as 'low' | 'medium' | 'high' })}
                className="w-full bg-white/10 border border-white/20 rounded-lg p-2 text-white"
              >
                <option value="low">Low (32kbps)</option>
                <option value="medium">Medium (128kbps)</option>
                <option value="high">High (320kbps)</option>
              </select>
            </div>

            <div>
              <label className="flex items-center space-x-2 p-2">
                <input
                  type="checkbox"
                  checked={audioConfig.recordOnThreshold}
                  onChange={(e) => updateAudioConfig({ recordOnThreshold: e.target.checked })}
                  className="w-4 h-4 text-green-600 bg-gray-800 border-gray-600 rounded focus:ring-green-500"
                />
                <span className="text-sm">Record on threshold</span>
              </label>
            </div>
          </div>

          {/* Sound Level Threshold */}
          <div className="mb-4">
            <label className="block text-sm font-medium mb-2">
              Sound Threshold: {audioConfig.threshold}%
            </label>
            <div className="relative">
              <input
                type="range"
                min="0"
                max="100"
                value={audioConfig.threshold}
                onChange={(e) => updateAudioConfig({ threshold: parseInt(e.target.value) })}
                className="w-full accent-green-500"
              />
              {/* Current sound level indicator */}
              {isStreaming && (
                <div className="mt-2">
                  <div className="flex justify-between text-xs text-gray-400 mb-1">
                    <span>Current Level: {currentSoundLevel.toFixed(1)}%</span>
                    <span className={currentSoundLevel > audioConfig.threshold ? 'text-green-400' : 'text-red-400'}>
                      {currentSoundLevel > audioConfig.threshold ? 'Recording' : 'Silent'}
                    </span>
                  </div>
                  <div className="w-full bg-gray-700 rounded-full h-2">
                    <div 
                      className={`h-2 rounded-full transition-all ${
                        currentSoundLevel > audioConfig.threshold ? 'bg-green-500' : 'bg-gray-500'
                      }`}
                      style={{ width: `${currentSoundLevel}%` }}
                    ></div>
                    <div 
                      className="absolute top-0 w-0.5 h-2 bg-yellow-400 rounded-full"
                      style={{ left: `${audioConfig.threshold}%`, transform: 'translateX(-50%)' }}
                    ></div>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}

      {/* Polling Frequency Section */}
      <div className="p-4 border-b border-white/10 bg-black/20">
        <div className="flex items-center space-x-3 mb-4">
          <Settings className="w-5 h-5 text-purple-400" />
          <h2 className="text-lg font-semibold">Polling Frequency</h2>
        </div>
        <div className="grid grid-cols-2 gap-2">
          {frequencyOptions.map((option) => (
            <button
              key={option.value}
              onClick={() => setPollingFrequency(option.value)}
              className={`p-3 rounded-lg border transition-all ${
                pollingFrequency === option.value
                  ? 'bg-purple-500/30 border-purple-400 shadow-lg shadow-purple-500/20'
                  : 'bg-white/5 border-white/10 hover:bg-white/10'
              }`}
            >
              <div className="font-medium text-sm">{option.label}</div>
              <div className="text-xs text-gray-400 mt-1">{option.desc}</div>
            </button>
          ))}
        </div>
      </div>

      {/* Sensors Selection */}
      <div className="flex-1 p-4">
        <div className="flex items-center justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold">Select Sensors & Metrics</h2>
            <div className="text-xs text-gray-400">Status: <span className={`font-medium ${isStreaming ? 'text-green-300' : 'text-gray-300'}`}>{isStreaming ? 'Streaming' : 'Idle'}</span></div>
          </div>
          <div className="flex items-center space-x-3">
            <span className="text-sm text-gray-400 bg-white/10 px-2 py-1 rounded-full">{selectedSensors.size} selected</span>
            <button onClick={stopStreaming} className="px-3 py-1 rounded bg-red-600/80 text-sm">Stop</button>
            <button onClick={startStreaming} className="px-3 py-1 rounded bg-green-600/80 text-sm">Start</button>
          </div>
        </div>

        <div className="space-y-3">
          {Object.entries(sensorCategories).map(([categoryId, category]) => {
            const CategoryIcon = category.icon;
            const isExpanded = expandedCategories.has(categoryId);
            const categorySensors = category.sensors;
            const selectedInCategory = categorySensors.filter((s: Sensor) => selectedSensors.has(s.id)).length;

            return (
              <div key={categoryId} className="bg-white/5 backdrop-blur-sm rounded-lg border border-white/10">
                <div 
                  className="flex items-center justify-between p-4 cursor-pointer hover:bg-white/5"
                  onClick={() => toggleCategory(categoryId)}
                >
                  <div className="flex items-center space-x-3">
                    <CategoryIcon className="w-5 h-5 text-purple-400" />
                    <div>
                      <div className="font-medium">{category.name}</div>
                      <div className="text-sm text-gray-400">
                        {selectedInCategory}/{categorySensors.length} selected
                      </div>
                    </div>
                  </div>
                  <div className="flex items-center space-x-2">
                    {selectedInCategory > 0 && (
                      <div className="w-2 h-2 bg-green-400 rounded-full"></div>
                    )}
                    {isExpanded ? <ChevronDown className="w-5 h-5" /> : <ChevronRight className="w-5 h-5" />}
                  </div>
                </div>

                {isExpanded && (
                  <div className="border-t border-white/10">
                    <div className="p-3 bg-white/5 flex space-x-2">
                      <button 
                        onClick={() => selectAllInCategory(categoryId)}
                        className="text-xs bg-purple-500/20 hover:bg-purple-500/30 text-purple-300 px-2 py-1 rounded"
                      >
                        Select All
                      </button>
                      <button 
                        onClick={() => deselectAllInCategory(categoryId)}
                        className="text-xs bg-gray-500/20 hover:bg-gray-500/30 text-gray-300 px-2 py-1 rounded"
                      >
                        Deselect All
                      </button>
                    </div>
                    <div className="p-2 space-y-1">
                      {categorySensors.map((sensor: Sensor) => (
                        <div key={sensor.id} className="flex items-center p-2 hover:bg-white/5 rounded">
                          <input
                            type="checkbox"
                            id={sensor.id}
                            checked={selectedSensors.has(sensor.id)}
                            onChange={() => toggleSensor(sensor.id)}
                            className="mr-3 w-4 h-4 text-purple-600 bg-gray-800 border-gray-600 rounded focus:ring-purple-500"
                          />
                          <div className="flex-1">
                            <div className="font-medium">{sensor.name}</div>
                            <div className="text-xs text-gray-400">{sensor.desc}</div>
                          </div>
                          <div className="text-xs text-gray-300">{selectedSensors.has(sensor.id) ? 'On' : 'Off'}</div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            );
          })}
        </div>

        {/* Footer Controls */}
        <div className="mt-6 flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <button
              onClick={() => { setSelectedSensors(new Set()); }}
              className="px-3 py-2 bg-white/5 rounded-lg text-sm"
            >
              Clear
            </button>

            <button
              onClick={() => {
                // quick select common sensors
                const quick = new Set(selectedSensors);
                ['accelerometer','gyroscope','linear_acceleration','gps_location'].forEach(id => quick.add(id));
                setSelectedSensors(new Set(quick));
              }}
              className="px-3 py-2 bg-white/5 rounded-lg text-sm"
            >
              Quick Select
            </button>
          </div>

          <div className="flex items-center space-x-3">
            <button
              onClick={stopStreaming}
              className="px-4 py-2 rounded-lg bg-red-600/80"
            >
              Stop
            </button>
            <button
              onClick={startStreaming}
              className="px-4 py-2 rounded-lg bg-green-600/80"
            >
              Start
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

export default SensorStreamingUI;
