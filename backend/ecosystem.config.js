module.exports = {
  apps: [
    {
      name: 'microbevision-api',
      script: './server.js',
      instances: 'max', // Scale across all CPU cores for load handling (BUG-011 FIX)
      exec_mode: 'cluster', // Enables Node.js native clustering
      max_memory_restart: '800M', // Prevent OOM crashes by restarting at 800MB threshold
      env: {
        NODE_ENV: 'production',
        PORT: 3000
      },
      error_file: './logs/api-error.log',
      out_file: './logs/api-out.log',
      merge_logs: true,
      time: true
    },
    {
      name: 'microbevision-python-ai',
      script: 'app.py',
      cwd: '../ai_service',
      interpreter: 'python',
      instances: 1, // Keep python at 1 instance for now since we rate-limit queue
      max_memory_restart: '1500M', // Python OpenCV can be memory heavy
      env: {
        FLASK_ENV: 'production',
        PORT: 5001
      },
      error_file: '../backend/logs/python-error.log',
      out_file: '../backend/logs/python-out.log',
      merge_logs: true,
      time: true
    }
  ]
};
